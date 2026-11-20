package com.example.ui.studio

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.FetchedStreamMetadata
import com.example.data.model.StreamProject
import com.example.data.model.TargetPlatform
import com.example.data.model.formatTimecode
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
fun StudioScreen(
    viewModel: StudioViewModel,
    onOpenProjectClips: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val projectsState by viewModel.projectsState.collectAsStateWithLifecycle()

    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.importLocalVideo(context, uri)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Studio Hero Header Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(144.dp)
                        .clip(RoundedCornerShape(22.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_studio_hero),
                        contentDescription = "ClipPulse studio header",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        StudioObsidian.copy(alpha = 0.92f),
                                        StudioObsidian.copy(alpha = 0.55f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "STREAM → VIRAL SHORTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan
                        )
                        Text(
                            text = "Clip Any YouTube Stream",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Text(
                            text = "AI extracts top moments, kinetic captions, viral titles & hashtags.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Stream Importer Card
            item {
                Surface(
                    color = StudioCard,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, StudioOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "1. Import Stream or Video",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedTextField(
                            value = actionState.urlInput,
                            onValueChange = viewModel::updateUrlInput,
                            placeholder = {
                                Text(
                                    stringResource(R.string.youtube_url_hint),
                                    color = TextMuted
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Link,
                                    contentDescription = null,
                                    tint = CyberCyan
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = StudioOutline,
                                focusedContainerColor = StudioObsidian,
                                unfocusedContainerColor = StudioObsidian
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("youtube_url_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.fetchYouTubeStream() },
                                enabled = !actionState.isFetchingStream && !actionState.isGeneratingClips,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StudioCardElevated,
                                    contentColor = CyberCyan
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("fetch_stream_button")
                            ) {
                                if (actionState.isFetchingStream) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = CyberCyan,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(stringResource(R.string.fetch_stream))
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    pickVideoLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                enabled = !actionState.isFetchingStream && !actionState.isGeneratingClips,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, StudioOutline),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pick_local_video_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VideoLibrary,
                                    contentDescription = null,
                                    tint = CoralPulse,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Device Video",
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        AnimatedVisibility(visible = actionState.fetchedPreview != null) {
                            actionState.fetchedPreview?.let { meta ->
                                FetchedStreamCard(meta)
                            }
                        }

                        // Optional Stream Transcript / Topics Input
                        OutlinedTextField(
                            value = actionState.notesInput,
                            onValueChange = viewModel::updateNotesInput,
                            label = { Text("Transcript / Stream Topics (auto-filled when available)") },
                            placeholder = {
                                Text(
                                    "Optional: paste key quotes, stream timestamps, or topic highlights…",
                                    color = TextMuted
                                )
                            },
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CoralPulse,
                                unfocusedBorderColor = StudioOutline,
                                focusedContainerColor = StudioObsidian,
                                unfocusedContainerColor = StudioObsidian
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("stream_notes_input")
                        )

                        OutlinedTextField(
                            value = actionState.focusAngle,
                            onValueChange = viewModel::updateFocusAngle,
                            label = { Text("Clip Focus Angle (optional)") },
                            placeholder = {
                                Text("e.g. Funny reactions, bold hot takes, step-by-step tips", color = TextMuted)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = StudioOutline,
                                focusedContainerColor = StudioObsidian,
                                unfocusedContainerColor = StudioObsidian
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Target Platform",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TargetPlatform.entries.forEach { platform ->
                                val selected = actionState.selectedPlatform == platform
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.selectPlatform(platform) },
                                    label = { Text(platform.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CoralPulse.copy(alpha = 0.22f),
                                        selectedLabelColor = CoralPulse
                                    )
                                )
                            }
                        }

                        Text(
                            text = "Preferred Clip Length",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(15, 30, 45, 60, 90).forEach { sec ->
                                val selected = actionState.targetLengthSec == sec
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.selectClipLength(sec) },
                                    label = { Text("${sec}s") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                        selectedLabelColor = CyberCyan
                                    )
                                )
                            }
                        }

                        if (actionState.isGeneratingClips) {
                            LinearProgressIndicator(
                                color = CoralPulse,
                                trackColor = StudioObsidian,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }

                        actionState.statusMessage?.let { msg ->
                            Surface(
                                color = CyberCyan.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = CyberCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = CyberCyan,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        actionState.errorMessage?.let { err ->
                            Surface(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.generateViralClipsFromPreview() },
                            enabled = !actionState.isGeneratingClips && !actionState.isFetchingStream,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CoralPulse,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("extract_viral_clips_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (actionState.isGeneratingClips) {
                                    "Analyzing Viral Moments…"
                                } else {
                                    stringResource(R.string.generate_viral_clips)
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        actionState.latestGeneratedProjectId?.let { newProjectId ->
                            OutlinedButton(
                                onClick = { onOpenProjectClips(newProjectId) },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, EmeraldViral),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("view_generated_clips_button")
                            ) {
                                Text(
                                    text = "View Generated Viral Clips →",
                                    color = EmeraldViral,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }

            // Recent Stream Projects Section
            item {
                Text(
                    text = "Recent Stream Projects",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            when (val state = projectsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CoralPulse)
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }

                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        item {
                            Surface(
                                color = StudioCard.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, StudioOutline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCut,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(34.dp)
                                    )
                                    Text(
                                        text = "No clipped streams yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Paste a YouTube stream link or pick a device video above to extract your first batch of viral clips.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        items(state.data, key = { it.id }) { project ->
                            StreamProjectRowCard(
                                project = project,
                                onClick = { onOpenProjectClips(project.id) },
                                onDelete = { viewModel.deleteProject(project.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FetchedStreamCard(meta: FetchedStreamMetadata) {
    Surface(
        color = StudioObsidian,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = meta.thumbnailUrl,
                contentDescription = "Stream thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 104.dp, height = 64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioCardElevated)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = meta.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${meta.channelName} • ${formatTimecode(meta.durationSeconds)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan
                )
                if (meta.captionsAutoExtracted) {
                    Text(
                        text = "✓ Transcript captions extracted",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldViral
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamProjectRowCard(
    project: StreamProject,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = StudioCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, StudioOutline),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("project_card_${project.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = project.thumbnailUrl,
                contentDescription = "Project thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 96.dp, height = 60.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioCardElevated)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = project.sourceTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${project.channelName} • ${project.platformEnum.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${project.clipCount} Viral Clips • ${formatTimecode(project.durationSeconds)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete project",
                    tint = TextMuted
                )
            }
        }
    }
}
