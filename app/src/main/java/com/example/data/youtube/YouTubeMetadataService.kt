package com.example.data.youtube

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.FetchedStreamMetadata
import com.example.data.model.formatTimecode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class YouTubeMetadataService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun extractVideoId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.matches(Regex("^[a-zA-Z0-9_\\-]{11}$"))) {
            return trimmed
        }
        val patterns = listOf(
            "(?:v=|/v/|embed/|shorts/|live/|youtu\\.be/)([a-zA-Z0-9_\\-]{11})"
        )
        for (regex in patterns) {
            val matcher = Pattern.compile(regex).matcher(trimmed)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        return null
    }

    suspend fun fetchYouTubeMetadata(rawUrl: String): Result<FetchedStreamMetadata> =
        withContext(Dispatchers.IO) {
            try {
                val videoId = extractVideoId(rawUrl)
                    ?: return@withContext Result.failure(
                        IllegalArgumentException("Enter a valid YouTube stream, live, or Shorts URL.")
                    )

                val canonicalUrl = "https://www.youtube.com/watch?v=$videoId"
                val encodedUrl = URLEncoder.encode(canonicalUrl, "UTF-8")

                // 1. Fetch official YouTube oEmbed metadata
                val oembedReq = Request.Builder()
                    .url("https://www.youtube.com/oembed?url=$encodedUrl&format=json")
                    .header("User-Agent", "Mozilla/5.0 (Android 15; Mobile)")
                    .build()

                var title = ""
                var author = ""
                var thumbnail = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                client.newCall(oembedReq).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("YouTube video unavailable or private (HTTP ${resp.code}).")
                        )
                    }
                    val body = resp.body?.string().orEmpty()
                    val json = JSONObject(body)
                    title = json.optString("title", "YouTube Stream")
                    author = json.optString("author_name", "YouTube Creator")
                    thumbnail = json.optString("thumbnail_url", thumbnail)
                }

                // 2. Best-effort fetch of watch page for duration, description & caption tracks
                var durationSeconds = 900L
                var extractedTranscript = ""
                var captionsAutoExtracted = false

                try {
                    val watchReq = Request.Builder()
                        .url(canonicalUrl)
                        .header(
                            "User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
                        )
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .build()

                    val html = client.newCall(watchReq).execute().use { resp ->
                        if (resp.isSuccessful) resp.body?.string().orEmpty() else ""
                    }

                    if (html.isNotBlank()) {
                        val lengthMatch = Regex("\"lengthSeconds\":\"(\\d+)\"").find(html)
                        lengthMatch?.groupValues?.getOrNull(1)?.toLongOrNull()?.let { len ->
                            if (len > 0L) durationSeconds = len
                        }

                        val descMatch = Regex("\"shortDescription\":\"(.*?)\",\"isCrawlable\"").find(html)
                        val rawDesc = descMatch?.groupValues?.getOrNull(1)
                            ?.replace("\\n", "\n")
                            ?.replace("\\\"", "\"")
                            ?.take(1200)
                            .orEmpty()

                        val captionUrlMatch = Regex("\"captionTracks\":\\[\\{\"baseUrl\":\"([^\"]+)\"").find(html)
                        val encodedCaptionUrl = captionUrlMatch?.groupValues?.getOrNull(1)
                        if (!encodedCaptionUrl.isNullOrBlank()) {
                            val cleanCaptionUrl = encodedCaptionUrl
                                .replace("\\u0026", "&")
                                .replace("\\/", "/")
                            val transcriptXml = fetchCaptionXml(cleanCaptionUrl)
                            if (transcriptXml.isNotBlank()) {
                                extractedTranscript = transcriptXml
                                captionsAutoExtracted = true
                            }
                        }

                        if (extractedTranscript.isBlank() && rawDesc.isNotBlank()) {
                            extractedTranscript = "Video Description & Chapters:\n$rawDesc"
                        }
                    }
                } catch (_: Exception) {
                    // Graceful fallback: oEmbed metadata is already populated
                }

                Result.success(
                    FetchedStreamMetadata(
                        sourceUrl = canonicalUrl,
                        videoId = videoId,
                        title = title,
                        channelName = author,
                        thumbnailUrl = thumbnail,
                        durationSeconds = durationSeconds,
                        transcriptText = extractedTranscript,
                        captionsAutoExtracted = captionsAutoExtracted
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun fetchCaptionXml(captionUrl: String): String {
        val req = Request.Builder()
            .url(captionUrl)
            .header("User-Agent", "Mozilla/5.0")
            .build()
        val xml = client.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string().orEmpty() else ""
        }
        if (xml.isBlank()) return ""

        val textRegex = Regex("<text start=\"([0-9.]+)\"[^>]*>(.*?)</text>")
        val builder = StringBuilder()
        for (match in textRegex.findAll(xml)) {
            val startSec = match.groupValues[1].toDoubleOrNull()?.toLong() ?: 0L
            val decoded = decodeXmlEntities(match.groupValues[2]).trim()
            if (decoded.isNotEmpty()) {
                builder.append("[${formatTimecode(startSec)}] $decoded\n")
            }
            if (builder.length > 9500) break
        }
        return builder.toString().trim()
    }

    private fun decodeXmlEntities(raw: String): String {
        return raw
            .replace("&amp;#39;", "'")
            .replace("&#39;", "'")
            .replace("&amp;quot;", "\"")
            .replace("&quot;", "\"")
            .replace("&amp;amp;", "&")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("\n", " ")
            .let {
                try {
                    if (it.contains("%")) URLDecoder.decode(it.replace("+", "%2B"), "UTF-8") else it
                } catch (_: Exception) {
                    it
                }
            }
    }

    suspend fun inspectLocalVideo(context: Context, uri: Uri): Result<FetchedStreamMetadata> =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 60_000L
                val durationSec = (durationMs / 1000L).coerceAtLeast(5L)

                var displayName = "Local Stream Recording"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIdx >= 0) {
                        displayName = cursor.getString(nameIdx) ?: displayName
                    }
                }

                Result.success(
                    FetchedStreamMetadata(
                        sourceUrl = uri.toString(),
                        videoId = "",
                        title = displayName.substringBeforeLast('.'),
                        channelName = "Device Recording",
                        thumbnailUrl = uri.toString(),
                        durationSeconds = durationSec,
                        transcriptText = "",
                        captionsAutoExtracted = false
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }
        }
}
