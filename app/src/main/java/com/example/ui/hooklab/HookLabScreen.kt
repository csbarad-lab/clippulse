package com.example.ui.hooklab

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TargetPlatform
import com.example.ui.studio.UiState
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralPulse
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldViral
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HookLabScreen(
    viewModel: HookLabViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val inputs by viewModel.inputs.collectAsStateWithLifecycle()
    val streamState by viewModel.streamState.collectAsStateWithLifecycle()
    val draftsState by viewModel.draftsState.collectAsStateWithLifecycle()

    val tones = listOf(
        "Contrarian & Bold",
        "Curiosity Gap",
        "Fast Tutorial",
        "Storytelling",
        "Hot Take"
    )

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "AI Hook & Script Lab",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Stream high-retention 3-second openers, B-roll cues, and viral captions in real time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

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
                        OutlinedTextField(
                            value = inputs.topic,
                            onValueChange = viewModel::updateTopic,
                            label = { Text("Stream Moment or Clip Idea") },
                            placeholder = {
                                Text("e.g. Why solo creators burn out and my 2-hour batching workflow", color = TextMuted)
                            },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = StudioOutline,
                                focusedContainerColor = StudioObsidian,
                                unfocusedContainerColor = StudioObsidian
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("hook_lab_topic_input")
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TargetPlatform.entries.forEach { platform ->
                                FilterChip(
                                    selected = inputs.selectedPlatform == platform,
                                    onClick = { viewModel.selectPlatform(platform) },
                                    label = { Text(platform.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CoralPulse.copy(alpha = 0.22f),
                                        selectedLabelColor = CoralPulse
                                    )
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tones.forEach { tone ->
                                FilterChip(
                                    selected = inputs.selectedTone == tone,
                                    onClick = { viewModel.selectTone(tone) },
                                    label = { Text(tone) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                        selectedLabelColor = CyberCyan
                                    )
                                )
                            }
                        }

                        Button(
                            onClick = viewModel::generateHooksStreaming,
                            enabled = streamState !is HookStreamState.Loading && streamState !is HookStreamState.Streaming,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CoralPulse,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("generate_hooks_button")
                        ) {
                            if (streamState is HookStreamState.Loading || streamState is HookStreamState.Streaming) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Streaming Viral Hooks…")
                            } else {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Stream Viral Hooks",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            // Streaming Output Card
            val activeOutput = when (val s = streamState) {
                is HookStreamState.Streaming -> s.text
                is HookStreamState.Success -> s.text
                else -> null
            }

            if (streamState is HookStreamState.Error) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = (streamState as HookStreamState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            if (activeOutput != null) {
                item {
                    Surface(
                        color = StudioCard,
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.55f)),
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
                                    text = if (streamState is HookStreamState.Streaming) "Live AI Output…" else "Generated Hook Playbook",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CyberCyan
                                )
                                IconButton(
                                    onClick = {
                                        copyToClipboard(context, "Viral Hooks", activeOutput)
                                        Toast.makeText(context, "Hook script copied!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy hooks", tint = CyberCyan)
                                }
                            }

                            Text(
                                text = activeOutput,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )

                            inputs.bannerMessage?.let { msg ->
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldViral
                                )
                            }

                            if (streamState is HookStreamState.Success) {
                                OutlinedButton(
                                    onClick = viewModel::saveCurrentHookToCloud,
                                    enabled = !inputs.isSavingDraft,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, AmberGold),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("save_hook_draft_button")
                                ) {
                                    Icon(Icons.Filled.BookmarkAdd, contentDescription = null, tint = AmberGold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Save to Hook Vault", color = AmberGold)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Saved Hook Drafts",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }

            when (val state = draftsState) {
                is UiState.Loading -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = CoralPulse)
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }

                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        item {
                            Text(
                                text = "Saved Hook Lab playbooks will appear here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    } else {
                        items(state.data, key = { it.id }) { draft ->
                            Surface(
                                color = StudioCard,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, StudioOutline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = draft.tone.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = AmberGold
                                            )
                                            Text(
                                                text = draft.topic,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Color.White
                                            )
                                        }
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    copyToClipboard(context, draft.topic, draft.generatedHooks)
                                                    Toast.makeText(context, "Copied draft!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy draft", tint = CyberCyan)
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteDraft(draft.id) },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete draft", tint = TextMuted)
                                            }
                                        }
                                    }
                                    Text(
                                        text = draft.generatedHooks,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        maxLines = 5
                                    )
                                }
                            }
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
