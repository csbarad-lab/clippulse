package com.example.ui.hooklab

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.ClipPulseAiService
import com.example.data.model.HookLabDraft
import com.example.data.model.TargetPlatform
import com.example.data.repository.ClipPulseRepository
import com.example.ui.studio.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface HookStreamState {
    data object Idle : HookStreamState
    data object Loading : HookStreamState
    data class Streaming(val text: String) : HookStreamState
    data class Success(val text: String) : HookStreamState
    data class Error(val message: String) : HookStreamState
}

data class HookLabUiInputs(
    val topic: String = "",
    val selectedTone: String = "Contrarian & Bold",
    val selectedPlatform: TargetPlatform = TargetPlatform.SHORTS,
    val isSavingDraft: Boolean = false,
    val bannerMessage: String? = null
)

class HookLabViewModel(
    private val repository: ClipPulseRepository,
    private val aiService: ClipPulseAiService,
    private val currentUserId: String
) : ViewModel() {

    companion object {
        private const val TAG = "HookLabVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }

    private val _inputs = MutableStateFlow(HookLabUiInputs())
    val inputs: StateFlow<HookLabUiInputs> = _inputs.asStateFlow()

    private val _streamState = MutableStateFlow<HookStreamState>(HookStreamState.Idle)
    val streamState: StateFlow<HookStreamState> = _streamState.asStateFlow()

    val draftsState: StateFlow<UiState<List<HookLabDraft>>> =
        repository.observeHookDrafts(currentUserId)
            .map<List<HookLabDraft>, UiState<List<HookLabDraft>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing hook drafts", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load saved hook drafts."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    fun updateTopic(topic: String) {
        _inputs.update { it.copy(topic = topic, bannerMessage = null) }
    }

    fun selectTone(tone: String) {
        _inputs.update { it.copy(selectedTone = tone) }
    }

    fun selectPlatform(platform: TargetPlatform) {
        _inputs.update { it.copy(selectedPlatform = platform) }
    }

    fun generateHooksStreaming() {
        val current = _inputs.value
        if (current.topic.isBlank()) {
            _streamState.value = HookStreamState.Error("Describe your stream moment or clip topic first.")
            return
        }

        viewModelScope.launch {
            _streamState.value = HookStreamState.Loading
            _inputs.update { it.copy(bannerMessage = null) }
            var latestText = ""
            try {
                aiService.streamHookLab(
                    topic = current.topic.trim(),
                    tone = current.selectedTone,
                    platform = current.selectedPlatform
                ).collect { accumulated ->
                    latestText = accumulated
                    _streamState.value = HookStreamState.Streaming(accumulated)
                }
                if (latestText.isNotBlank()) {
                    _streamState.value = HookStreamState.Success(latestText)
                } else {
                    _streamState.value = HookStreamState.Error("AI returned an empty hook script.")
                }
            } catch (e: TimeoutCancellationException) {
                _streamState.value = HookStreamState.Error("Hook generation timed out after 60s.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Hook Lab streaming failed", e)
                _streamState.value = HookStreamState.Error(
                    e.localizedMessage ?: "Failed to generate viral hooks."
                )
            }
        }
    }

    fun saveCurrentHookToCloud() {
        val text = when (val s = _streamState.value) {
            is HookStreamState.Success -> s.text
            is HookStreamState.Streaming -> s.text
            else -> ""
        }
        if (text.isBlank()) return
        val current = _inputs.value

        viewModelScope.launch {
            _inputs.update { it.copy(isSavingDraft = true, bannerMessage = null) }
            repository.saveHookDraft(
                userId = currentUserId,
                topic = current.topic.ifBlank { "Viral Hook Draft" },
                tone = current.selectedTone,
                generatedHooks = text
            ).onSuccess {
                _inputs.update {
                    it.copy(isSavingDraft = false, bannerMessage = "Saved to Hook Lab Vault!")
                }
            }.onFailure { err ->
                _inputs.update {
                    it.copy(
                        isSavingDraft = false,
                        bannerMessage = err.localizedMessage ?: "Failed to save draft."
                    )
                }
            }
        }
    }

    fun deleteDraft(draftId: String) {
        viewModelScope.launch {
            repository.deleteHookDraft(currentUserId, draftId)
        }
    }
}
