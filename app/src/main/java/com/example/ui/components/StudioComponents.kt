package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AspectRatio
import com.example.data.model.CaptionSegment
import com.example.data.model.CaptionStyle
import com.example.data.model.ViralClip
import com.example.data.model.formatTimecode
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralPulse
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldViral
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun ViralScoreBadge(
    score: Long,
    modifier: Modifier = Modifier
) {
    val badgeColor = when {
        score >= 90L -> EmeraldViral
        score >= 78L -> CyberCyan
        else -> AmberGold
    }
    Surface(
        color = badgeColor.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = badgeColor
            )
            Text(
                text = "VIRAL",
                style = MaterialTheme.typography.labelSmall,
                color = badgeColor.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun WaveformTimelineCanvas(
    durationSeconds: Long,
    startSec: Long,
    endSec: Long,
    playheadOffsetSec: Long,
    modifier: Modifier = Modifier
) {
    val safeTotal = durationSeconds.coerceAtLeast(10L).toFloat()
    val startRatio = (startSec.toFloat() / safeTotal).coerceIn(0f, 1f)
    val endRatio = (endSec.toFloat() / safeTotal).coerceIn(startRatio, 1f)
    val clipSpan = (endSec - startSec).coerceAtLeast(1L).toFloat()
    val playRatioInStream = ((startSec + playheadOffsetSec).toFloat() / safeTotal).coerceIn(startRatio, endRatio)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(StudioObsidian)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        val barCount = 48
        val barWidth = size.width / (barCount * 1.6f)
        val gap = (size.width - barCount * barWidth) / (barCount - 1).coerceAtLeast(1)

        // Highlight clipped region backdrop
        val regionLeft = startRatio * size.width
        val regionWidth = ((endRatio - startRatio) * size.width).coerceAtLeast(8f)
        drawRoundRect(
            color = CoralPulse.copy(alpha = 0.16f),
            topLeft = Offset(regionLeft, 0f),
            size = Size(regionWidth, size.height),
            cornerRadius = CornerRadius(8f, 8f)
        )

        for (i in 0 until barCount) {
            val pos = i.toFloat() / barCount.toFloat()
            val amp = 0.25f + 0.72f * abs(sin(i * 0.65f + 1.1f) * sin(i * 0.27f + 0.4f))
            val barHeight = size.height * amp
            val x = i * (barWidth + gap)
            val y = (size.height - barHeight) / 2f
            val inClip = pos in startRatio..endRatio
            val color = when {
                inClip && pos <= playRatioInStream -> CyberCyan
                inClip -> CoralPulse
                else -> Color(0xFF2D354C)
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }

        // Playhead line
        val playX = (playRatioInStream * size.width).coerceIn(0f, size.width)
        drawLine(
            color = Color.White,
            start = Offset(playX, 0f),
            end = Offset(playX, size.height),
            strokeWidth = 3f
        )

        // Trim boundary handles
        drawLine(
            color = CyberCyan,
            start = Offset(regionLeft, 0f),
            end = Offset(regionLeft, size.height),
            strokeWidth = 4f
        )
        drawLine(
            color = CoralPulse,
            start = Offset(regionLeft + regionWidth, 0f),
            end = Offset(regionLeft + regionWidth, size.height),
            strokeWidth = 4f
        )
    }
}

@Composable
fun ViralityBreakdownBars(
    hookScore: Long,
    retentionScore: Long,
    shareabilityScore: Long,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ScoreBarRow("3s Hook Power", hookScore, CoralPulse)
        ScoreBarRow("Retention Pace", retentionScore, CyberCyan)
        ScoreBarRow("Shareability", shareabilityScore, AmberGold)
    }
}

@Composable
private fun ScoreBarRow(label: String, score: Long, color: Color) {
    val fraction by animateFloatAsState(
        targetValue = (score.coerceIn(0L, 100L) / 100f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = label
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text = "$score/100",
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(StudioObsidian)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(color.copy(alpha = 0.7f), color)
                        )
                    )
            )
        }
    }
}

@Composable
fun KineticCaptionPreviewFrame(
    thumbnailUrl: String,
    thumbnailText: String,
    aspectRatio: AspectRatio,
    captionStyle: CaptionStyle,
    segments: List<CaptionSegment>,
    currentOffsetSec: Long,
    clipDurationSec: Long,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSegment = segments.firstOrNull {
        currentOffsetSec in it.startOffsetSec..it.endOffsetSec
    } ?: segments.lastOrNull { currentOffsetSec >= it.startOffsetSec } ?: segments.firstOrNull()

    val frameAspect = when (aspectRatio) {
        AspectRatio.RATIO_9_16 -> 9f / 14f
        AspectRatio.RATIO_4_5 -> 4f / 5f
        AspectRatio.RATIO_1_1 -> 1f
    }

    Box(
        modifier = modifier
            .widthIn(max = 290.dp)
            .fillMaxWidth()
            .aspectRatio(frameAspect)
            .clip(RoundedCornerShape(24.dp))
            .background(StudioObsidian)
            .clickable(onClick = onTogglePlay)
            .testTag("kinetic_preview_frame")
    ) {
        if (thumbnailUrl.isNotBlank()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = "Clip video preview frame",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Dark cinematic scrim for legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            StudioObsidian.copy(alpha = 0.65f),
                            StudioObsidian.copy(alpha = 0.25f),
                            StudioObsidian.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Top aspect badge & thumbnail cover headline
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = StudioObsidian.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = aspectRatio.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    color = CoralPulse.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "${formatTimecode(currentOffsetSec)} / ${formatTimecode(clipDurationSec)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (thumbnailText.isNotBlank()) {
                Surface(
                    color = AmberGold,
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = thumbnailText.uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        ),
                        color = StudioObsidian,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Center Play/Pause indicator
        Surface(
            color = StudioObsidian.copy(alpha = 0.68f),
            shape = CircleShape,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
            modifier = Modifier
                .align(Alignment.Center)
                .size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause kinetic preview" else "Play kinetic preview",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        // Bottom Kinetic Caption Overlay
        if (activeSegment != null) {
            KineticCaptionOverlayBox(
                segment = activeSegment,
                style = captionStyle,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 14.dp, vertical = 22.dp)
            )
        }
    }
}

@Composable
private fun KineticCaptionOverlayBox(
    segment: CaptionSegment,
    style: CaptionStyle,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = when (style) {
            CaptionStyle.HORMOZI_NEON -> StudioObsidian.copy(alpha = 0.82f)
            CaptionStyle.MRBEAST_IMPACT -> Color.Black.copy(alpha = 0.72f)
            CaptionStyle.MINIMAL_GLASS -> Color.White.copy(alpha = 0.18f)
            CaptionStyle.CYBER_PULSE -> Color(0xFF1F1035).copy(alpha = 0.88f)
        },
        label = "caption_bg"
    )
    val highlightColor = when (style) {
        CaptionStyle.HORMOZI_NEON -> AmberGold
        CaptionStyle.MRBEAST_IMPACT -> CoralPulse
        CaptionStyle.MINIMAL_GLASS -> CyberCyan
        CaptionStyle.CYBER_PULSE -> CyberCyan
    }
    val displayText = if (style == CaptionStyle.MRBEAST_IMPACT || style == CaptionStyle.HORMOZI_NEON) {
        segment.text.uppercase()
    } else {
        segment.text
    }
    val highlight = if (style == CaptionStyle.MRBEAST_IMPACT || style == CaptionStyle.HORMOZI_NEON) {
        segment.highlightWord.uppercase()
    } else {
        segment.highlightWord
    }

    val annotated = buildAnnotatedString {
        if (highlight.isNotBlank() && displayText.contains(highlight, ignoreCase = true)) {
            val idx = displayText.indexOf(highlight, ignoreCase = true)
            append(displayText.substring(0, idx))
            withStyle(
                SpanStyle(
                    color = highlightColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
            ) {
                append(displayText.substring(idx, idx + highlight.length))
            }
            append(displayText.substring(idx + highlight.length))
        } else {
            append(displayText)
        }
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (style == CaptionStyle.CYBER_PULSE) CoralPulse else Color.White.copy(alpha = 0.25f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = annotated,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 24.sp
            ),
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ViralClipCard(
    clip: ViralClip,
    projectTitle: String?,
    onOpenStudio: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCopyPack: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioCard,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, StudioOutline),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onOpenStudio)
            .testTag("viral_clip_card_${clip.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ViralScoreBadge(score = clip.viralScore)
                    Surface(
                        color = StudioCardElevated,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${formatTimecode(clip.startTimeSec)}–${formatTimecode(clip.endTimeSec)} (${clip.clipDurationSec}s)",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (clip.favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorite clip",
                            tint = if (clip.favorite) AmberGold else TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onCopyPack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy viral title, caption and hashtags",
                            tint = CyberCyan
                        )
                    }
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete clip",
                                tint = TextMuted
                            )
                        }
                    }
                }
            }

            if (!projectTitle.isNullOrBlank()) {
                Text(
                    text = projectTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = CoralPulse,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = clip.hookTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = clip.viralCaption,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (clip.hashtags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    clip.hashtags.take(5).forEach { tag ->
                        Surface(
                            color = StudioObsidian,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, StudioOutline)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${clip.captionSegments.size} kinetic captions • ${clip.captionStyleEnum.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = "Open Studio →",
                    style = MaterialTheme.typography.labelLarge,
                    color = CoralPulse
                )
            }
        }
    }
}
