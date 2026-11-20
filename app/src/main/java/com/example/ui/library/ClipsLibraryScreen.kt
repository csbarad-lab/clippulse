package com.example.ui.library

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ViralClip
import com.example.ui.components.ViralClipCard
import com.example.ui.studio.ProjectWithClips
import com.example.ui.studio.StudioViewModel
import com.example.ui.studio.UiState
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralPulse
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun ClipsLibraryScreen(
    viewModel: StudioViewModel,
    initialProjectFilterId: String?,
    onOpenClipEditor: (projectId: String, clipId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val libraryState by viewModel.libraryClipsState.collectAsStateWithLifecycle()

    var selectedProjectId by remember(initialProjectFilterId) {
        mutableStateOf(initialProjectFilterId)
    }
    var favoritesOnly by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = libraryState) {
            is UiState.Loading -> {
                CircularProgressIndicator(
                    color = CoralPulse,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is UiState.Error -> {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            is UiState.Success -> {
                val projectsWithClips: List<ProjectWithClips> = state.data
                val flattenedClips: List<Pair<String, ViralClip>> = remember(
                    projectsWithClips,
                    selectedProjectId,
                    favoritesOnly
                ) {
                    projectsWithClips
                        .filter { selectedProjectId == null || it.project.id == selectedProjectId }
                        .flatMap { pwc ->
                            pwc.clips
                                .filter { !favoritesOnly || it.favorite }
                                .map { clip -> pwc.project.sourceTitle to clip }
                        }
                        .sortedByDescending { it.second.viralScore }
                }

                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .align(Alignment.TopCenter)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Viral Clips Vault",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Tap any clip to preview kinetic captions, trim bounds, or remix titles & hashtags.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedProjectId == null && !favoritesOnly,
                                    onClick = {
                                        selectedProjectId = null
                                        favoritesOnly = false
                                    },
                                    label = { Text("All Clips") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CoralPulse.copy(alpha = 0.22f),
                                        selectedLabelColor = CoralPulse
                                    )
                                )
                            }
                            item {
                                FilterChip(
                                    selected = favoritesOnly,
                                    onClick = { favoritesOnly = !favoritesOnly },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = AmberGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = { Text("Starred") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AmberGold.copy(alpha = 0.2f),
                                        selectedLabelColor = AmberGold
                                    )
                                )
                            }
                            items(projectsWithClips, key = { it.project.id }) { pwc ->
                                FilterChip(
                                    selected = selectedProjectId == pwc.project.id,
                                    onClick = {
                                        selectedProjectId = if (selectedProjectId == pwc.project.id) null else pwc.project.id
                                    },
                                    label = {
                                        Text(
                                            text = pwc.project.sourceTitle.take(22),
                                            maxLines = 1
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                        selectedLabelColor = CyberCyan
                                    )
                                )
                            }
                        }
                    }

                    if (flattenedClips.isEmpty()) {
                        item {
                            Surface(
                                color = StudioCard,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, StudioOutline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MovieFilter,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(
                                        text = "No viral clips match this filter",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Switch to Clip Studio to analyze a YouTube stream or clear your filter.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        items(flattenedClips, key = { it.second.id }) { (projectTitle, clip) ->
                            ViralClipCard(
                                clip = clip,
                                projectTitle = projectTitle,
                                onOpenStudio = { onOpenClipEditor(clip.projectId, clip.id) },
                                onToggleFavorite = { viewModel.toggleFavorite(clip.projectId, clip) },
                                onCopyPack = {
                                    copyToClipboard(context, "Viral Clip Pack", clip.toShareableSocialPack())
                                    Toast.makeText(context, "Copied title, caption & hashtags!", Toast.LENGTH_SHORT).show()
                                },
                                onDelete = { viewModel.deleteClip(clip.projectId, clip.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
