package com.example.data.ai

import android.util.Log
import com.example.data.model.AspectRatio
import com.example.data.model.CaptionSegment
import com.example.data.model.CaptionStyle
import com.example.data.model.TargetPlatform
import com.example.data.model.TitleOption
import com.example.data.model.ViralClip
import com.example.data.model.ViralRemixResult
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class ClipPulseAiService {

    companion object {
        private const val TAG = "ClipPulseAI"
        private const val MODEL_NAME = "gemini-3.8-flash"
        private const val AI_TIMEOUT_MS = 60_000L
    }

    private val clipExtractionSchema = Schema.obj(
        properties = mapOf(
            "clips" to Schema.array(
                description = "3 to 4 high-retention viral short clip candidates ranked by viralScore descending",
                items = Schema.obj(
                    properties = mapOf(
                        "hookTitle" to Schema.string(description = "Scroll-stopping viral hook title under 90 chars"),
                        "startTimeSec" to Schema.integer(description = "Start timestamp in seconds within stream duration"),
                        "endTimeSec" to Schema.integer(description = "End timestamp in seconds, strictly greater than startTimeSec"),
                        "viralScore" to Schema.integer(description = "Overall virality score 0-100"),
                        "hookScore" to Schema.integer(description = "First 3-second hook strength 0-100"),
                        "retentionScore" to Schema.integer(description = "Pacing and retention score 0-100"),
                        "shareabilityScore" to Schema.integer(description = "Share/save potential score 0-100"),
                        "whyViral" to Schema.string(description = "Short explanation of the viral trigger"),
                        "viralCaption" to Schema.string(description = "Engaging social caption with spacing and CTA"),
                        "hashtags" to Schema.array(
                            items = Schema.string(),
                            description = "6 to 10 relevant hashtags starting with #"
                        ),
                        "thumbnailText" to Schema.string(description = "2-4 bold words for cover overlay"),
                        "pinnedComment" to Schema.string(description = "Engaging question for the pinned comment"),
                        "captionSegments" to Schema.array(
                            description = "5 to 10 timed kinetic caption chunks relative to clip start (0s upwards)",
                            items = Schema.obj(
                                properties = mapOf(
                                    "startOffsetSec" to Schema.integer(description = "Offset from clip start in seconds"),
                                    "endOffsetSec" to Schema.integer(description = "End offset from clip start in seconds"),
                                    "text" to Schema.string(description = "Short punchy 3-7 word subtitle line"),
                                    "highlightWord" to Schema.string(description = "Single keyword in text to emphasize in neon color")
                                )
                            )
                        )
                    )
                )
            )
        )
    )

    private val remixSchema = Schema.obj(
        properties = mapOf(
            "titleOptions" to Schema.array(
                description = "3 viral title options with distinct psychological CTR angles",
                items = Schema.obj(
                    properties = mapOf(
                        "title" to Schema.string(description = "Viral hook title"),
                        "angle" to Schema.string(description = "Short angle label such as Curiosity Gap, Contrarian, or Proof")
                    )
                )
            ),
            "viralCaption" to Schema.string(description = "Platform-tailored viral caption with hook, value lines, and CTA"),
            "hashtags" to Schema.array(
                items = Schema.string(),
                description = "8 to 12 targeted hashtags starting with #"
            ),
            "pinnedComment" to Schema.string(description = "Conversation-sparking pinned comment"),
            "thumbnailText" to Schema.string(description = "Bold 2-4 word cover text")
        )
    )

    private val segmentsOnlySchema = Schema.obj(
        properties = mapOf(
            "captionSegments" to Schema.array(
                description = "6 to 12 kinetic word-synced subtitle lines covering the clip duration",
                items = Schema.obj(
                    properties = mapOf(
                        "startOffsetSec" to Schema.integer(),
                        "endOffsetSec" to Schema.integer(),
                        "text" to Schema.string(),
                        "highlightWord" to Schema.string()
                    )
                )
            )
        )
    )

    private val clipExtractorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = clipExtractionSchema
            }
        )
    }

    private val remixModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = remixSchema
            }
        )
    }

    private val captionGeneratorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = segmentsOnlySchema
            }
        )
    }

    private val streamingHookModel by lazy {
        Firebase.ai.generativeModel(modelName = MODEL_NAME)
    }

    suspend fun analyzeStreamForViralClips(
        sourceTitle: String,
        channelName: String,
        durationSeconds: Long,
        transcriptOrNotes: String,
        targetPlatform: TargetPlatform,
        targetClipLengthSec: Int,
        focusAngle: String
    ): List<ViralClip> {
        val safeDuration = durationSeconds.coerceAtLeast(30L)
        val prompt = buildString {
            appendLine("You are an elite short-form video strategist and viral clip editor.")
            appendLine("Analyze this YouTube stream/video and propose 3 distinct viral clip moments.")
            appendLine("Source Title: $sourceTitle")
            appendLine("Creator/Channel: $channelName")
            appendLine("Total Duration Seconds: $safeDuration")
            appendLine("Target Platform: ${targetPlatform.label}")
            appendLine("Target Clip Length: ~${targetClipLengthSec}s")
            if (focusAngle.isNotBlank()) {
                appendLine("Creator Focus Request: $focusAngle")
            }
            if (transcriptOrNotes.isNotBlank()) {
                appendLine("Extracted Transcript / Stream Context:")
                appendLine(transcriptOrNotes.take(8000))
            } else {
                appendLine("No transcript was available; infer realistic high-impact segment chapters and spoken hooks aligned to the stream title, creator style, and duration.")
            }
            appendLine("Requirements:")
            appendLine("- Keep every startTimeSec >= 0 and endTimeSec <= $safeDuration, with endTimeSec > startTimeSec.")
            appendLine("- Make each clip roughly ${targetClipLengthSec}s long (at least 12s).")
            appendLine("- For captionSegments, create 6-9 punchy kinetic subtitle chunks starting at startOffsetSec=0 up to clip length.")
            appendLine("- Ensure highlightWord appears inside that segment's text.")
            appendLine("- Make hashtags start with # and avoid generic spam.")
        }

        val response = withTimeout(AI_TIMEOUT_MS) {
            clipExtractorModel.generateContent(prompt)
        }
        val rawJson = response.text ?: throw IllegalStateException("AI returned empty clip analysis.")
        val root = JSONObject(rawJson)
        val clipsArray = root.getJSONArray("clips")

        val results = mutableListOf<ViralClip>()
        for (i in 0 until clipsArray.length()) {
            val obj = clipsArray.getJSONObject(i)
            val rawStart = obj.optLong("startTimeSec", (i * 120L)).coerceIn(0L, (safeDuration - 10L).coerceAtLeast(0L))
            val rawEnd = obj.optLong("endTimeSec", rawStart + targetClipLengthSec)
                .coerceIn(rawStart + 8L, safeDuration.coerceAtLeast(rawStart + 8L))

            val hashtagsArr = obj.optJSONArray("hashtags")
            val tags = buildList {
                if (hashtagsArr != null) {
                    for (t in 0 until hashtagsArr.length()) {
                        val tag = hashtagsArr.optString(t).trim()
                        if (tag.isNotBlank()) add(if (tag.startsWith("#")) tag else "#$tag")
                    }
                }
            }.take(15)

            val segArr = obj.optJSONArray("captionSegments")
            val segments = buildList {
                if (segArr != null) {
                    for (s in 0 until segArr.length()) {
                        val segObj = segArr.optJSONObject(s) ?: continue
                        val sOffset = segObj.optLong("startOffsetSec", s * 3L).coerceAtLeast(0L)
                        val eOffset = segObj.optLong("endOffsetSec", sOffset + 3L).coerceAtLeast(sOffset + 1L)
                        val txt = segObj.optString("text").trim()
                        val hl = segObj.optString("highlightWord").trim()
                        if (txt.isNotBlank()) {
                            add(CaptionSegment(sOffset, eOffset, txt, hl))
                        }
                    }
                }
            }.take(25)

            results.add(
                ViralClip(
                    hookTitle = obj.optString("hookTitle", "Viral Moment #${i + 1}"),
                    startTimeSec = rawStart,
                    endTimeSec = rawEnd,
                    viralScore = obj.optLong("viralScore", 88L).coerceIn(0L, 100L),
                    hookScore = obj.optLong("hookScore", 89L).coerceIn(0L, 100L),
                    retentionScore = obj.optLong("retentionScore", 86L).coerceIn(0L, 100L),
                    shareabilityScore = obj.optLong("shareabilityScore", 87L).coerceIn(0L, 100L),
                    whyViral = obj.optString("whyViral", "Strong emotional opening hook and clear payoff."),
                    viralCaption = obj.optString("viralCaption", ""),
                    hashtags = tags,
                    thumbnailText = obj.optString("thumbnailText", "WATCH THIS"),
                    pinnedComment = obj.optString("pinnedComment", "What do you think? Drop it below!"),
                    captionStyle = CaptionStyle.HORMOZI_NEON.name,
                    aspectRatio = AspectRatio.RATIO_9_16.name,
                    captionSegments = segments,
                    favorite = false
                )
            )
        }
        if (results.isEmpty()) {
            throw IllegalStateException("AI did not return any clip candidates.")
        }
        return results.sortedByDescending { it.viralScore }
    }

    suspend fun remixViralPack(
        sourceTitle: String,
        currentHookTitle: String,
        currentCaption: String,
        transcriptExcerpt: String,
        platform: TargetPlatform,
        tone: String
    ): ViralRemixResult {
        val prompt = buildString {
            appendLine("You are a viral social media copywriter and CTR strategist.")
            appendLine("Remix this short clip's metadata for maximum clicks, watch time, and shares.")
            appendLine("Original Stream: $sourceTitle")
            appendLine("Current Clip Hook: $currentHookTitle")
            appendLine("Current Caption: $currentCaption")
            if (transcriptExcerpt.isNotBlank()) {
                appendLine("Context/Captions: ${transcriptExcerpt.take(2500)}")
            }
            appendLine("Target Platform: ${platform.label}")
            appendLine("Requested Tone/Vibe: $tone")
            appendLine("Return 3 alternative high-CTR viral titles, a fresh engaging caption tailored to ${platform.label}, 10 targeted hashtags, a pinned comment prompt, and 2-4 word thumbnail text.")
        }

        val response = withTimeout(AI_TIMEOUT_MS) {
            remixModel.generateContent(prompt)
        }
        val json = JSONObject(response.text ?: throw IllegalStateException("Empty remix response."))

        val titlesJson = json.optJSONArray("titleOptions")
        val titleOptions = buildList {
            if (titlesJson != null) {
                for (i in 0 until titlesJson.length()) {
                    val item = titlesJson.optJSONObject(i) ?: continue
                    val t = item.optString("title").trim()
                    val a = item.optString("angle").trim()
                    if (t.isNotBlank()) add(TitleOption(t, a.ifBlank { "Hook" }))
                }
            }
        }

        val tagsJson = json.optJSONArray("hashtags")
        val hashtags = buildList {
            if (tagsJson != null) {
                for (i in 0 until tagsJson.length()) {
                    val tag = tagsJson.optString(i).trim()
                    if (tag.isNotBlank()) add(if (tag.startsWith("#")) tag else "#$tag")
                }
            }
        }.take(15)

        return ViralRemixResult(
            titleOptions = titleOptions,
            viralCaption = json.optString("viralCaption", currentCaption),
            hashtags = hashtags,
            pinnedComment = json.optString("pinnedComment", ""),
            thumbnailText = json.optString("thumbnailText", "")
        )
    }

    suspend fun regenerateTimedCaptions(
        hookTitle: String,
        clipDurationSec: Long,
        transcriptContext: String,
        styleHint: String
    ): List<CaptionSegment> {
        val prompt = buildString {
            appendLine("Create punchy kinetic short-form captions for a ${clipDurationSec}s clip.")
            appendLine("Clip Hook/Topic: $hookTitle")
            appendLine("Caption Pacing Style: $styleHint")
            if (transcriptContext.isNotBlank()) {
                appendLine("Source Transcript Context: ${transcriptContext.take(2500)}")
            }
            appendLine("Split into 6 to 10 short, dynamic spoken subtitle lines from startOffsetSec=0 to $clipDurationSec, each 3-6 words with 1 highlightWord.")
        }

        val response = withTimeout(AI_TIMEOUT_MS) {
            captionGeneratorModel.generateContent(prompt)
        }
        val json = JSONObject(response.text ?: throw IllegalStateException("Empty captions response."))
        val segArr = json.getJSONArray("captionSegments")

        return buildList {
            for (i in 0 until segArr.length()) {
                val obj = segArr.optJSONObject(i) ?: continue
                val start = obj.optLong("startOffsetSec", i * 3L).coerceIn(0L, (clipDurationSec - 1L).coerceAtLeast(0L))
                val end = obj.optLong("endOffsetSec", start + 3L).coerceIn(start + 1L, clipDurationSec.coerceAtLeast(start + 1L))
                val text = obj.optString("text").trim()
                val highlight = obj.optString("highlightWord").trim()
                if (text.isNotBlank()) {
                    add(CaptionSegment(start, end, text, highlight))
                }
            }
        }.take(25)
    }

    fun streamHookLab(topic: String, tone: String, platform: TargetPlatform): Flow<String> = flow {
        val prompt = buildString {
            appendLine("You are a viral short-form script & hook coach.")
            appendLine("Topic / Stream Moment: $topic")
            appendLine("Platform: ${platform.label}")
            appendLine("Tone: $tone")
            appendLine()
            appendLine("Write a concise, high-energy creator playbook with:")
            appendLine("1. THREE 3-Second Spoken Hooks (Pattern Interrupt, Contrarian, Secret Reveal)")
            appendLine("2. Visual & B-Roll Cut Cues (what to zoom, overlay, or flash in the first 5s)")
            appendLine("3. Viral Title + Caption + 8 Hashtags")
            appendLine("4. Retention Loop / Pinned Comment CTA")
        }

        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MS) {
                streamingHookModel.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Streaming hook lab failed", e)
            throw e
        }
    }
}
