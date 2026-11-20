package com.example.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CaptionSegment
import com.example.data.model.TargetPlatform
import com.example.data.model.TitleOption
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ViralCopyRemixCard(
    selectedPlatform: TargetPlatform,
    selectedTone: String,
    isRemixing: Boolean,
    titleSuggestions: List<TitleOption>,
    hookTitle: String,
    viralCaption: String,
    hashtagsInput: String,
    thumbnailText: String,
    pinnedComment: String,
    onSelectPlatform: (TargetPlatform) -> Unit,
    onSelectTone: (String) -> Unit,
    onRemixWithAi: () -> Unit,
    onApplyTitleOption: (String) -> Unit,
    onHookTitleChange: (String) -> Unit,
    onViralCaptionChange: (String) -> Unit,
    onHashtagsChange: (String) -> Unit,
    onThumbnailTextChange: (String) -> Unit,
    onPinnedCommentChange: (String) -> Unit
) {
    val tones = listOf(
        "Bold & Contrarian",
        "Educational Punchy",
        "Storytelling Emotional",
        "Gen-Z Hype",
        "Minimalist Pro"
    )

    Surface(
        color = StudioCard,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, StudioOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Viral Title, Caption & Hashtags",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Platform Target",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TargetPlatform.entries.forEach { platform ->
                    FilterChip(
                        selected = selectedPlatform == platform,
                        onClick = { onSelectPlatform(platform) },
                        label = { Text(platform.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CoralPulse.copy(alpha = 0.22f),
                            selectedLabelColor = CoralPulse
                        )
                    )
                }
            }

            Text(
                text = "Copywriting Tone",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tones.forEach { tone ->
                    FilterChip(
                        selected = selectedTone == tone,
                        onClick = { onSelectTone(tone) },
                        label = { Text(tone) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                            selectedLabelColor = CyberCyan
                        )
                    )
                }
            }

            Button(
                onClick = onRemixWithAi,
                enabled = !isRemixing,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioCardElevated,
                    contentColor = CyberCyan
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("ai_remix_button")
            ) {
                if (isRemixing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = CyberCyan,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remixing with Gemini AI…")
                } else {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.remix_with_ai))
                }
            }

            if (titleSuggestions.isNotEmpty()) {
                Text(
                    text = "Tap an AI Title Option to Apply:",
                    style = MaterialTheme.typography.labelMedium,
                    color = AmberGold
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    titleSuggestions.forEach { option ->
                        Surface(
                            color = StudioObsidian,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onApplyTitleOption(option.title) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = option.angle.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AmberGold
                                )
                                Text(
                                    text = option.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = hookTitle,
                onValueChange = onHookTitleChange,
                label = { Text("Viral Hook Title") },
                shape = RoundedCornerShape(14.dp),
                colors = studioFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_hook_title_input")
            )

            OutlinedTextField(
                value = viralCaption,
                onValueChange = onViralCaptionChange,
                label = { Text("Social Post Caption") },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(14.dp),
                colors = studioFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_viral_caption_input")
            )

            OutlinedTextField(
                value = hashtagsInput,
                onValueChange = onHashtagsChange,
                label = { Text("Hashtags (space separated)") },
                minLines = 2,
                maxLines = 3,
                shape = RoundedCornerShape(14.dp),
                colors = studioFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_hashtags_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = thumbnailText,
                    onValueChange = onThumbnailTextChange,
                    label = { Text("Cover Text") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = studioFieldColors(),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = pinnedComment,
                    onValueChange = onPinnedCommentChange,
                    label = { Text("Pinned Comment CTA") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = studioFieldColors(),
                    modifier = Modifier.weight(1.3f)
                )
            }
        }
    }
}

@Composable
fun KineticCaptionsEditorCard(
    segments: List<CaptionSegment>,
    isRegenerating: Boolean,
    onRegenerateCaptions: () -> Unit,
    onSelectOffset: (Long) -> Unit,
    onEditSegmentText: (Int, String) -> Unit
) {
    Surface(
        color = StudioCard,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, StudioOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    Icon(
                        imageVector = Icons.Filled.ClosedCaption,
                        contentDescription = null,
                        tint = CyberCyan
                    )
                    Text(
                        text = "AI Timed Captions (${segments.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedButton(
                    onClick = onRegenerateCaptions,
                    enabled = !isRegenerating,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.55f))
                ) {
                    if (isRegenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = CyberCyan,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Sync AI Captions", color = CyberCyan)
                    }
                }
            }

            if (segments.isEmpty()) {
                Text(
                    text = "Tap Sync AI Captions to generate timed kinetic subtitles for this clip.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                segments.forEachIndexed { index, seg ->
                    Surface(
                        color = StudioObsidian,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StudioOutline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectOffset(seg.startOffsetSec) }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${formatTimecode(seg.startOffsetSec)} → ${formatTimecode(seg.endOffsetSec)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan
                                )
                                if (seg.highlightWord.isNotBlank()) {
                                    Text(
                                        text = "POP: ${seg.highlightWord}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AmberGold
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = seg.text,
                                onValueChange = { onEditSegmentText(index, it) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = studioFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExportAndSaveCard(
    isSaving: Boolean,
    onSaveToCloud: () -> Unit,
    onCopyViralPack: () -> Unit,
    onCopySrt: () -> Unit,
    onSharePack: () -> Unit
) {
    Surface(
        color = StudioCard,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, StudioOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Export & Publish Pack",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Button(
                onClick = onSaveToCloud,
                enabled = !isSaving,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldViral,
                    contentColor = StudioObsidian
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_clip_edits_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = StudioObsidian,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Edits to Cloud",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyViralPack,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("copy_viral_pack_button")
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Pack", color = CyberCyan)
                }

                OutlinedButton(
                    onClick = onCopySrt,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("copy_srt_button")
                ) {
                    Icon(Icons.Filled.Subtitles, contentDescription = null, tint = AmberGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy .SRT", color = AmberGold)
                }
            }

            Button(
                onClick = onSharePack,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CoralPulse,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("share_viral_pack_button")
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.share_clip_pack))
            }
        }
    }
}

@Composable
private fun studioFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyberCyan,
    unfocusedBorderColor = StudioOutline,
    focusedContainerColor = StudioObsidian,
    unfocusedContainerColor = StudioObsidian,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = CyberCyan,
    unfocusedLabelColor = TextMuted
)
