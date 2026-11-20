package com.example.ui.editor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.AspectRatio
import com.example.data.model.CaptionStyle
import com.example.data.model.ViralClip
import com.example.data.model.formatTimecode
import com.example.ui.components.KineticCaptionPreviewFrame
import com.example.ui.components.ViralScoreBadge
import com.example.ui.components.ViralityBreakdownBars
import com.example.ui.components.WaveformTimelineCanvas
import com.example.ui.studio.UiState
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralPulse
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldViral
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioSlate
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ClipEditorScreen(
    viewModel: ClipEditorViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val clipUiState by viewModel.clipState.collectAsStateWithLifecycle()
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = StudioObsidian,
        topBar = {
            TopAppBar(
                title = { Text("Viral Clip Studio", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to clips"
                        )
                    }
                },
                actions = {
                    val clip = (clipUiState as? UiState.Success)?.data
                    if (clip != null) {
                        IconButton(onClick = { viewModel.toggleFavorite(clip) }) {
                            Icon(
                                imageVector = if (clip.favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Toggle favorite",
                                tint = if (clip.favorite) AmberGold else Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StudioSlate,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = clipUiState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(
                        color = CoralPulse,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is UiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                }

                is UiState.Success -> {
                    val clip = state.data
                    if (clip == null) {
                        Text(
                            text = "Clip not found.",
                            color = TextSecondary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        ClipEditorContent(
                            clip = clip,
                            editorState = editorState,
                            viewModel = viewModel,
                            context = context
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClipEditorContent(
    clip: ViralClip,
    editorState: EditorRemixState,
    viewModel: ClipEditorViewModel,
    context: Context
) {
    // Editable local drafts synced when remote clip changes
    var hookTitle by remember(clip.id, clip.hookTitle) { mutableStateOf(clip.hookTitle) }
    var viralCaption by remember(clip.id, clip.viralCaption) { mutableStateOf(clip.viralCaption) }
    var hashtagsInput by remember(clip.id, clip.hashtags) { mutableStateOf(clip.hashtags.joinToString(" ")) }
    var thumbnailText by remember(clip.id, clip.thumbnailText) { mutableStateOf(clip.thumbnailText) }
    var pinnedComment by remember(clip.id, clip.pinnedComment) { mutableStateOf(clip.pinnedComment) }
    var startSec by remember(clip.id, clip.startTimeSec) { mutableLongStateOf(clip.startTimeSec) }
    var endSec by remember(clip.id, clip.endTimeSec) { mutableLongStateOf(clip.endTimeSec) }
    var segments by remember(clip.id, clip.captionSegments) { mutableStateOf(clip.captionSegments) }

    var isPlaying by remember { mutableStateOf(true) }
    var playheadOffsetSec by remember { mutableLongStateOf(0L) }

    val clipDuration = (endSec - startSec).coerceAtLeast(1L)

    LaunchedEffect(isPlaying, clipDuration) {
        while (isPlaying) {
            delay(1000L)
            playheadOffsetSec = if (playheadOffsetSec + 1L > clipDuration) 0L else playheadOffsetSec + 1L
        }
    }

    val project = editorState.parentProject
    val streamDuration = (project?.durationSeconds ?: (endSec + 120L)).coerceAtLeast(endSec + 10L)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (editorState.feedbackMessage != null || editorState.errorMessage != null) {
                item {
                    val isErr = editorState.errorMessage != null
                    Surface(
                        color = if (isErr) MaterialTheme.colorScheme.error.copy(alpha = 0.14f) else EmeraldViral.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, if (isErr) MaterialTheme.colorScheme.error else EmeraldViral),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = editorState.errorMessage ?: editorState.feedbackMessage.orEmpty(),
                            color = if (isErr) MaterialTheme.colorScheme.error else EmeraldViral,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // 1. Live Kinetic Caption 9:16 Preview Player
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KineticCaptionPreviewFrame(
                        thumbnailUrl = project?.thumbnailUrl.orEmpty(),
                        thumbnailText = thumbnailText,
                        aspectRatio = clip.aspectRatioEnum,
                        captionStyle = clip.captionStyleEnum,
                        segments = segments,
                        currentOffsetSec = playheadOffsetSec,
                        clipDurationSec = clipDuration,
                        isPlaying = isPlaying,
                        onTogglePlay = { isPlaying = !isPlaying }
                    )

                    if (!project?.videoId.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                val ytIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.youtube.com/watch?v=${project!!.videoId}&t=${startSec}s")
                                )
                                context.startActivity(ytIntent)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CoralPulse.copy(alpha = 0.65f))
                        ) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = null, tint = CoralPulse, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${stringResource(R.string.open_youtube_timestamp)} (${formatTimecode(startSec)})",
                                color = CoralPulse
                            )
                        }
                    }
                }
            }

            // 2. Caption Visual Style & Aspect Ratio Controls
            item {
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
                            text = "Kinetic Caption Style & Framing",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CaptionStyle.entries.forEach { style ->
                                FilterChip(
                                    selected = clip.captionStyleEnum == style,
                                    onClick = { viewModel.applyStyle(clip, style) },
                                    label = { Text(style.displayName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AmberGold.copy(alpha = 0.22f),
                                        selectedLabelColor = AmberGold
                                    )
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AspectRatio.entries.forEach { ratio ->
                                FilterChip(
                                    selected = clip.aspectRatioEnum == ratio,
                                    onClick = { viewModel.applyAspectRatio(clip, ratio) },
                                    label = { Text(ratio.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                                        selectedLabelColor = CyberCyan
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 3. Stream Waveform & Trim Window
            item {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Clip Trim Window",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = "${formatTimecode(startSec)} → ${formatTimecode(endSec)} (${clipDuration}s)",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberCyan
                            )
                        }

                        WaveformTimelineCanvas(
                            durationSeconds = streamDuration,
                            startSec = startSec,
                            endSec = endSec,
                            playheadOffsetSec = playheadOffsetSec
                        )

                        RangeSlider(
                            value = startSec.toFloat()..endSec.toFloat(),
                            onValueChange = { range ->
                                val newStart = range.start.toLong().coerceIn(0L, streamDuration - 5L)
                                val newEnd = range.endInclusive.toLong().coerceIn(newStart + 5L, streamDuration)
                                startSec = newStart
                                endSec = newEnd
                                playheadOffsetSec = 0L
                            },
                            valueRange = 0f..streamDuration.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = CoralPulse,
                                activeTrackColor = CyberCyan,
                                inactiveTrackColor = StudioObsidian
                            )
                        )
                    }
                }
            }

            // 4. AI Virality Scorecard
            item {
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
                            Text(
                                text = "AI Virality Scorecard",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            ViralScoreBadge(score = clip.viralScore)
                        }
                        ViralityBreakdownBars(
                            hookScore = clip.hookScore,
                            retentionScore = clip.retentionScore,
                            shareabilityScore = clip.shareabilityScore
                        )
                        Text(
                            text = clip.whyViral,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            // 5. Viral Title, Caption & Hashtag Remix Card
            item {
                ViralCopyRemixCard(
                    selectedPlatform = editorState.selectedPlatform,
                    selectedTone = editorState.selectedTone,
                    isRemixing = editorState.isRemixing,
                    titleSuggestions = editorState.titleSuggestions,
                    hookTitle = hookTitle,
                    viralCaption = viralCaption,
                    hashtagsInput = hashtagsInput,
                    thumbnailText = thumbnailText,
                    pinnedComment = pinnedComment,
                    onSelectPlatform = viewModel::selectPlatform,
                    onSelectTone = viewModel::selectTone,
                    onRemixWithAi = { viewModel.remixMetadataWithAi(clip) },
                    onApplyTitleOption = { chosenTitle ->
                        hookTitle = chosenTitle
                        viewModel.applyTitleOption(clip, chosenTitle)
                    },
                    onHookTitleChange = { hookTitle = it },
                    onViralCaptionChange = { viralCaption = it },
                    onHashtagsChange = { hashtagsInput = it },
                    onThumbnailTextChange = { thumbnailText = it },
                    onPinnedCommentChange = { pinnedComment = it }
                )
            }

            // 6. Kinetic Captions Editor
            item {
                KineticCaptionsEditorCard(
                    segments = segments,
                    isRegenerating = editorState.isRegeneratingCaptions,
                    onRegenerateCaptions = { viewModel.regenerateCaptionsWithAi(clip) },
                    onSelectOffset = { offset ->
                        playheadOffsetSec = offset
                        isPlaying = false
                    },
                    onEditSegmentText = { idx, newText ->
                        segments = segments.toMutableList().also { list ->
                            list[idx] = list[idx].copy(text = newText)
                        }
                    }
                )
            }

            // 7. Save, Copy & Share Pack Card
            item {
                val liveDraftClip = clip.copy(
                    hookTitle = hookTitle,
                    startTimeSec = startSec,
                    endTimeSec = endSec,
                    viralCaption = viralCaption,
                    hashtags = hashtagsInput.split(Regex("[\\s,]+"))
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .map { if (it.startsWith("#")) it else "#$it" },
                    thumbnailText = thumbnailText,
                    pinnedComment = pinnedComment,
                    captionSegments = segments
                )

                ExportAndSaveCard(
                    isSaving = editorState.isSaving,
                    onSaveToCloud = {
                        viewModel.saveManualEdits(
                            clip = clip,
                            hookTitle = hookTitle,
                            startSec = startSec,
                            endSec = endSec,
                            viralCaption = viralCaption,
                            hashtagsRaw = hashtagsInput,
                            thumbnailText = thumbnailText,
                            pinnedComment = pinnedComment,
                            segments = segments
                        )
                    },
                    onCopyViralPack = {
                        copyText(context, "Viral Pack", liveDraftClip.toShareableSocialPack())
                        Toast.makeText(context, "Viral title, caption & hashtags copied!", Toast.LENGTH_SHORT).show()
                    },
                    onCopySrt = {
                        val srt = liveDraftClip.toSrtString()
                        if (srt.isBlank()) {
                            Toast.makeText(context, "No captions available to export.", Toast.LENGTH_SHORT).show()
                        } else {
                            copyText(context, "SRT Subtitles", srt)
                            Toast.makeText(context, ".SRT captions copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSharePack = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, liveDraftClip.hookTitle)
                            putExtra(Intent.EXTRA_TEXT, liveDraftClip.toShareableSocialPack())
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Viral Clip Pack"))
                    }
                )
            }
        }
    }
}

private fun copyText(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
}
