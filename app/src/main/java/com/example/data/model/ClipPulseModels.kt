package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TargetPlatform(val label: String, val maxChars: Int) {
    SHORTS("YouTube Shorts", 100),
    REELS("Instagram Reels", 2200),
    TIKTOK("TikTok", 2200),
    LINKEDIN("LinkedIn Video", 3000);

    companion object {
        fun fromRaw(raw: String?): TargetPlatform =
            entries.firstOrNull { it.name == raw } ?: SHORTS
    }
}

enum class CaptionStyle(val displayName: String, val tagline: String) {
    HORMOZI_NEON("Hormozi Neon", "High-contrast yellow & cyan pop"),
    MRBEAST_IMPACT("Impact Punch", "Bold uppercase with dark stroke"),
    MINIMAL_GLASS("Minimal Glass", "Sleek frosted pill subtitles"),
    CYBER_PULSE("Cyber Pulse", "Coral & neon gradient glow");

    companion object {
        fun fromRaw(raw: String?): CaptionStyle =
            entries.firstOrNull { it.name == raw } ?: HORMOZI_NEON
    }
}

enum class AspectRatio(val label: String, val ratioFloat: Float) {
    RATIO_9_16("9:16 Vertical", 9f / 16f),
    RATIO_1_1("1:1 Square", 1f),
    RATIO_4_5("4:5 Portrait", 4f / 5f);

    companion object {
        fun fromRaw(raw: String?): AspectRatio =
            entries.firstOrNull { it.name == raw } ?: RATIO_9_16
    }
}

@Keep
@IgnoreExtraProperties
data class CaptionSegment(
    val startOffsetSec: Long = 0L,
    val endOffsetSec: Long = 3L,
    val text: String = "",
    val highlightWord: String = ""
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "startOffsetSec" to startOffsetSec.coerceAtLeast(0L),
        "endOffsetSec" to endOffsetSec.coerceAtLeast(startOffsetSec + 1L),
        "text" to text.take(160),
        "highlightWord" to highlightWord.take(40)
    )
}

@Keep
@IgnoreExtraProperties
data class StreamProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sourceUrl: String = "",
    val videoId: String = "",
    val sourceTitle: String = "",
    val channelName: String = "",
    val thumbnailUrl: String = "",
    val durationSeconds: Long = 0L,
    val transcriptText: String = "",
    val targetPlatform: String = TargetPlatform.SHORTS.name,
    val clipCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val platformEnum: TargetPlatform
        get() = TargetPlatform.fromRaw(targetPlatform)

    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "sourceUrl" to sourceUrl.take(500),
        "videoId" to videoId.take(32),
        "sourceTitle" to sourceTitle.ifBlank { "Untitled Stream" }.take(200),
        "channelName" to channelName.ifBlank { "Creator" }.take(120),
        "thumbnailUrl" to thumbnailUrl.take(500),
        "durationSeconds" to durationSeconds.coerceIn(0L, 86400L),
        "transcriptText" to transcriptText.take(12000),
        "targetPlatform" to TargetPlatform.fromRaw(targetPlatform).name,
        "clipCount" to clipCount.coerceIn(0L, 50L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class ViralClip(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val hookTitle: String = "",
    val startTimeSec: Long = 0L,
    val endTimeSec: Long = 30L,
    val viralScore: Long = 85L,
    val hookScore: Long = 85L,
    val retentionScore: Long = 84L,
    val shareabilityScore: Long = 86L,
    val whyViral: String = "",
    val viralCaption: String = "",
    val hashtags: List<String> = emptyList(),
    val thumbnailText: String = "",
    val pinnedComment: String = "",
    val captionStyle: String = CaptionStyle.HORMOZI_NEON.name,
    val aspectRatio: String = AspectRatio.RATIO_9_16.name,
    val captionSegments: List<CaptionSegment> = emptyList(),
    val favorite: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val captionStyleEnum: CaptionStyle
        get() = CaptionStyle.fromRaw(captionStyle)

    val aspectRatioEnum: AspectRatio
        get() = AspectRatio.fromRaw(aspectRatio)

    val clipDurationSec: Long
        get() = (endTimeSec - startTimeSec).coerceAtLeast(1L)

    fun toCreateMap(ownerUid: String, parentProjectId: String): Map<String, Any> {
        val safeStart = startTimeSec.coerceIn(0L, 86399L)
        val safeEnd = endTimeSec.coerceIn(safeStart + 1L, 86400L)
        return mapOf(
            "userId" to ownerUid,
            "projectId" to parentProjectId,
            "hookTitle" to hookTitle.ifBlank { "Viral Clip" }.take(160),
            "startTimeSec" to safeStart,
            "endTimeSec" to safeEnd,
            "viralScore" to viralScore.coerceIn(0L, 100L),
            "hookScore" to hookScore.coerceIn(0L, 100L),
            "retentionScore" to retentionScore.coerceIn(0L, 100L),
            "shareabilityScore" to shareabilityScore.coerceIn(0L, 100L),
            "whyViral" to whyViral.ifBlank { "High retention hook and engaging punchline." }.take(600),
            "viralCaption" to viralCaption.ifBlank { hookTitle }.take(2200),
            "hashtags" to hashtags.take(20).map { it.take(50) },
            "thumbnailText" to thumbnailText.take(80),
            "pinnedComment" to pinnedComment.take(400),
            "captionStyle" to CaptionStyle.fromRaw(captionStyle).name,
            "aspectRatio" to AspectRatio.fromRaw(aspectRatio).name,
            "captionSegments" to captionSegments.take(30).map { it.toFirestoreMap() },
            "favorite" to favorite,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    fun toSrtString(): String {
        if (captionSegments.isEmpty()) return ""
        return captionSegments.mapIndexed { index, seg ->
            val start = formatSrtTime(seg.startOffsetSec)
            val end = formatSrtTime(seg.endOffsetSec.coerceAtLeast(seg.startOffsetSec + 1L))
            "${index + 1}\n$start --> $end\n${seg.text}"
        }.joinToString("\n\n")
    }

    fun toShareableSocialPack(): String = buildString {
        appendLine("TITLE:")
        appendLine(hookTitle)
        appendLine()
        appendLine("VIRAL CAPTION:")
        appendLine(viralCaption)
        appendLine()
        if (hashtags.isNotEmpty()) {
            appendLine("HASHTAGS:")
            appendLine(hashtags.joinToString(" "))
            appendLine()
        }
        if (pinnedComment.isNotBlank()) {
            appendLine("PINNED COMMENT:")
            appendLine(pinnedComment)
        }
    }.trim()
}

@Keep
@IgnoreExtraProperties
data class HookLabDraft(
    @DocumentId val id: String = "",
    val userId: String = "",
    val topic: String = "",
    val tone: String = "",
    val generatedHooks: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "topic" to topic.ifBlank { "Stream Highlight" }.take(300),
        "tone" to tone.ifBlank { "Bold" }.take(60),
        "generatedHooks" to generatedHooks.ifBlank { "..." }.take(6000),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

data class TitleOption(
    val title: String,
    val angle: String
)

data class ViralRemixResult(
    val titleOptions: List<TitleOption>,
    val viralCaption: String,
    val hashtags: List<String>,
    val pinnedComment: String,
    val thumbnailText: String
)

data class FetchedStreamMetadata(
    val sourceUrl: String,
    val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val durationSeconds: Long,
    val transcriptText: String,
    val captionsAutoExtracted: Boolean
)

fun formatTimecode(totalSeconds: Long): String {
    val clamped = totalSeconds.coerceAtLeast(0L)
    val hours = clamped / 3600L
    val mins = (clamped % 3600L) / 60L
    val secs = clamped % 60L
    return if (hours > 0L) {
        String.format("%d:%02d:%02d", hours, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}

private fun formatSrtTime(totalSeconds: Long): String {
    val clamped = totalSeconds.coerceAtLeast(0L)
    val hours = clamped / 3600L
    val mins = (clamped % 3600L) / 60L
    val secs = clamped % 60L
    return String.format("%02d:%02d:%02d,000", hours, mins, secs)
}
