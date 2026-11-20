package com.example.ui.editor

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.ClipPulseAiService
import com.example.data.model.AspectRatio
import com.example.data.model.CaptionSegment
import com.example.data.model.CaptionStyle
import com.example.data.model.StreamProject
import com.example.data.model.TargetPlatform
import com.example.data.model.TitleOption
import com.example.data.model.ViralClip
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

data class EditorRemixState(
    val parentProject: StreamProject? = null,
    val selectedPlatform: TargetPlatform = TargetPlatform.SHORTS,
    val selectedTone: String = "Bold & Contrarian",
    val isRemixing: Boolean = false,
    val isRegeneratingCaptions: Boolean = false,
    val isSaving: Boolean = false,
    val titleSuggestions: List<TitleOption> = emptyList(),
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
)

class ClipEditorViewModel(
    private val repository: ClipPulseRepository,
    private val aiService: ClipPulseAiService,
    private val currentUserId: String,
    private val projectId: String,
    private val clipId: String
) : ViewModel() {

    companion object {
        private const val TAG = "ClipEditorVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }

    private val _editorState = MutableStateFlow(EditorRemixState())
    val editorState: StateFlow<EditorRemixState> = _editorState.asStateFlow()

    val clipState: StateFlow<UiState<ViralClip?>> =
        repository.observeClip(currentUserId, projectId, clipId)
            .map<ViralClip?, UiState<ViralClip?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing clip", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load clip."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    init {
        viewModelScope.launch {
            repository.getProjectOnce(currentUserId, projectId)
                .onSuccess { project ->
                    if (project != null) {
                        _editorState.update {
                            it.copy(
                                parentProject = project,
                                selectedPlatform = project.platformEnum
                            )
                        }
                    }
                }
        }
    }

    fun selectPlatform(platform: TargetPlatform) {
        _editorState.update { it.copy(selectedPlatform = platform) }
    }

    fun selectTone(tone: String) {
        _editorState.update { it.copy(selectedTone = tone) }
    }

    fun clearBanner() {
        _editorState.update { it.copy(feedbackMessage = null, errorMessage = null) }
    }

    fun remixMetadataWithAi(clip: ViralClip) {
        val state = _editorState.value
        val project = state.parentProject
        viewModelScope.launch {
            _editorState.update {
                it.copy(isRemixing = true, errorMessage = null, feedbackMessage = null)
            }
            try {
                val remix = aiService.remixViralPack(
                    sourceTitle = project?.sourceTitle ?: clip.hookTitle,
                    currentHookTitle = clip.hookTitle,
                    currentCaption = clip.viralCaption,
                    transcriptExcerpt = project?.transcriptText.orEmpty(),
                    platform = state.selectedPlatform,
                    tone = state.selectedTone
                )

                val bestTitle = remix.titleOptions.firstOrNull()?.title ?: clip.hookTitle
                val updatedThumb = remix.thumbnailText.ifBlank { clip.thumbnailText }
                val updatedPinned = remix.pinnedComment.ifBlank { clip.pinnedComment }

                repository.updateClip(
                    userId = currentUserId,
                    projectId = projectId,
                    clipId = clipId,
                    hookTitle = bestTitle,
                    startTimeSec = clip.startTimeSec,
                    endTimeSec = clip.endTimeSec,
                    viralCaption = remix.viralCaption,
                    hashtags = remix.hashtags.ifEmpty { clip.hashtags },
                    thumbnailText = updatedThumb,
                    pinnedComment = updatedPinned,
                    captionStyle = clip.captionStyleEnum,
                    aspectRatio = clip.aspectRatioEnum,
                    captionSegments = clip.captionSegments,
                    favorite = clip.favorite
                ).onSuccess {
                    _editorState.update {
                        it.copy(
                            isRemixing = false,
                            titleSuggestions = remix.titleOptions,
                            feedbackMessage = "Remixed for ${state.selectedPlatform.label}!"
                        )
                    }
                }.onFailure { err ->
                    _editorState.update {
                        it.copy(
                            isRemixing = false,
                            errorMessage = err.localizedMessage ?: "Failed to save remixed pack."
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Remix timed out", e)
                _editorState.update {
                    it.copy(
                        isRemixing = false,
                        errorMessage = "AI remix timed out after 60s. Please try again."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Remix failed", e)
                _editorState.update {
                    it.copy(
                        isRemixing = false,
                        errorMessage = e.localizedMessage ?: "AI remix failed."
                    )
                }
            }
        }
    }

    fun regenerateCaptionsWithAi(clip: ViralClip) {
        val project = _editorState.value.parentProject
        viewModelScope.launch {
            _editorState.update {
                it.copy(isRegeneratingCaptions = true, errorMessage = null, feedbackMessage = null)
            }
            try {
                val freshSegments = aiService.regenerateTimedCaptions(
                    hookTitle = clip.hookTitle,
                    clipDurationSec = clip.clipDurationSec,
                    transcriptContext = project?.transcriptText.orEmpty(),
                    styleHint = clip.captionStyleEnum.displayName
                )
                saveClipEdits(
                    clip = clip.copy(captionSegments = freshSegments),
                    message = "Fresh kinetic captions synced!"
                )
            } catch (e: TimeoutCancellationException) {
                _editorState.update {
                    it.copy(
                        isRegeneratingCaptions = false,
                        errorMessage = "Caption generation timed out."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Caption regeneration failed", e)
                _editorState.update {
                    it.copy(
                        isRegeneratingCaptions = false,
                        errorMessage = e.localizedMessage ?: "Failed to regenerate captions."
                    )
                }
            } finally {
                _editorState.update { it.copy(isRegeneratingCaptions = false) }
            }
        }
    }

    fun applyStyle(clip: ViralClip, style: CaptionStyle) {
        saveClipEdits(clip.copy(captionStyle = style.name), "Caption style: ${style.displayName}")
    }

    fun applyAspectRatio(clip: ViralClip, ratio: AspectRatio) {
        saveClipEdits(clip.copy(aspectRatio = ratio.name), "Frame set to ${ratio.label}")
    }

    fun applyTitleOption(clip: ViralClip, title: String) {
        saveClipEdits(clip.copy(hookTitle = title), "Applied viral hook title!")
    }

    fun saveManualEdits(
        clip: ViralClip,
        hookTitle: String,
        startSec: Long,
        endSec: Long,
        viralCaption: String,
        hashtagsRaw: String,
        thumbnailText: String,
        pinnedComment: String,
        segments: List<CaptionSegment>
    ) {
        val parsedTags = hashtagsRaw
            .split(Regex("[\\s,]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { if (it.startsWith("#")) it else "#$it" }
            .distinct()
            .take(20)

        saveClipEdits(
            clip.copy(
                hookTitle = hookTitle,
                startTimeSec = startSec,
                endTimeSec = endSec,
                viralCaption = viralCaption,
                hashtags = parsedTags,
                thumbnailText = thumbnailText,
                pinnedComment = pinnedComment,
                captionSegments = segments
            ),
            message = "Changes saved to cloud!"
        )
    }

    fun toggleFavorite(clip: ViralClip) {
        viewModelScope.launch {
            repository.toggleFavoriteClip(currentUserId, projectId, clipId, !clip.favorite)
        }
    }

    private fun saveClipEdits(clip: ViralClip, message: String) {
        viewModelScope.launch {
            _editorState.update { it.copy(isSaving = true, errorMessage = null) }
            repository.updateClip(
                userId = currentUserId,
                projectId = projectId,
                clipId = clipId,
                hookTitle = clip.hookTitle,
                startTimeSec = clip.startTimeSec,
                endTimeSec = clip.endTimeSec,
                viralCaption = clip.viralCaption,
                hashtags = clip.hashtags,
                thumbnailText = clip.thumbnailText,
                pinnedComment = clip.pinnedComment,
                captionStyle = clip.captionStyleEnum,
                aspectRatio = clip.aspectRatioEnum,
                captionSegments = clip.captionSegments,
                favorite = clip.favorite
            ).onSuccess {
                _editorState.update { it.copy(isSaving = false, feedbackMessage = message) }
            }.onFailure { err ->
                _editorState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = err.localizedMessage ?: "Could not save edits."
                    )
                }
            }
        }
    }
}
