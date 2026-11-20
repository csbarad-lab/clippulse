package com.example.ui.studio

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.ClipPulseAiService
import com.example.data.model.FetchedStreamMetadata
import com.example.data.model.StreamProject
import com.example.data.model.TargetPlatform
import com.example.data.model.ViralClip
import com.example.data.repository.ClipPulseRepository
import com.example.data.youtube.YouTubeMetadataService
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class ProjectWithClips(
    val project: StreamProject,
    val clips: List<ViralClip>
)

data class StudioActionState(
    val urlInput: String = "",
    val notesInput: String = "",
    val focusAngle: String = "",
    val selectedPlatform: TargetPlatform = TargetPlatform.SHORTS,
    val targetLengthSec: Int = 45,
    val isFetchingStream: Boolean = false,
    val isGeneratingClips: Boolean = false,
    val fetchedPreview: FetchedStreamMetadata? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val latestGeneratedProjectId: String? = null
)

class StudioViewModel(
    private val repository: ClipPulseRepository,
    private val youtubeService: YouTubeMetadataService,
    private val aiService: ClipPulseAiService,
    private val currentUserId: String
) : ViewModel() {

    companion object {
        private const val TAG = "StudioVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }

    private val _actionState = MutableStateFlow(StudioActionState())
    val actionState: StateFlow<StudioActionState> = _actionState.asStateFlow()

    val projectsState: StateFlow<UiState<List<StreamProject>>> =
        repository.observeProjects(currentUserId)
            .map<List<StreamProject>, UiState<List<StreamProject>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing projects", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load stream projects."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val libraryClipsState: StateFlow<UiState<List<ProjectWithClips>>> =
        repository.observeProjects(currentUserId)
            .flatMapLatest { projects ->
                if (projects.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    val flows = projects.take(12).map { project ->
                        repository.observeClipsForProject(currentUserId, project.id)
                            .map { clips -> ProjectWithClips(project, clips) }
                    }
                    combine(flows) { array -> array.toList() }
                }
            }
            .map<List<ProjectWithClips>, UiState<List<ProjectWithClips>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing viral clips library", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load viral clips."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    fun updateUrlInput(value: String) {
        _actionState.update { it.copy(urlInput = value, errorMessage = null) }
    }

    fun updateNotesInput(value: String) {
        _actionState.update { it.copy(notesInput = value, errorMessage = null) }
    }

    fun updateFocusAngle(value: String) {
        _actionState.update { it.copy(focusAngle = value) }
    }

    fun selectPlatform(platform: TargetPlatform) {
        _actionState.update { it.copy(selectedPlatform = platform) }
    }

    fun selectClipLength(seconds: Int) {
        _actionState.update { it.copy(targetLengthSec = seconds) }
    }

    fun clearMessages() {
        _actionState.update {
            it.copy(errorMessage = null, statusMessage = null, latestGeneratedProjectId = null)
        }
    }

    fun fetchYouTubeStream(urlOverride: String? = null) {
        val targetUrl = (urlOverride ?: _actionState.value.urlInput).trim()
        if (targetUrl.isBlank()) {
            _actionState.update { it.copy(errorMessage = "Paste a YouTube stream or video link first.") }
            return
        }
        viewModelScope.launch {
            _actionState.update {
                it.copy(
                    urlInput = targetUrl,
                    isFetchingStream = true,
                    errorMessage = null,
                    statusMessage = "Fetching stream info & YouTube captions…"
                )
            }
            youtubeService.fetchYouTubeMetadata(targetUrl)
                .onSuccess { meta ->
                    _actionState.update {
                        it.copy(
                            isFetchingStream = false,
                            fetchedPreview = meta,
                            notesInput = if (meta.transcriptText.isNotBlank()) meta.transcriptText else it.notesInput,
                            statusMessage = if (meta.captionsAutoExtracted) {
                                "Stream & transcript extracted! Ready for AI clipping."
                            } else {
                                "Stream loaded! Add optional key topics below for sharper AI clips."
                            }
                        )
                    }
                }
                .onFailure { err ->
                    _actionState.update {
                        it.copy(
                            isFetchingStream = false,
                            errorMessage = err.localizedMessage ?: "Could not fetch YouTube video."
                        )
                    }
                }
        }
    }

    fun importLocalVideo(context: Context, uri: Uri) {
        viewModelScope.launch {
            _actionState.update {
                it.copy(
                    isFetchingStream = true,
                    errorMessage = null,
                    statusMessage = "Inspecting selected video file…"
                )
            }
            youtubeService.inspectLocalVideo(context, uri)
                .onSuccess { meta ->
                    _actionState.update {
                        it.copy(
                            isFetchingStream = false,
                            fetchedPreview = meta,
                            statusMessage = "Local video loaded (${meta.durationSeconds}s). Describe key moments or extract directly!"
                        )
                    }
                }
                .onFailure { err ->
                    _actionState.update {
                        it.copy(
                            isFetchingStream = false,
                            errorMessage = err.localizedMessage ?: "Failed to read selected video."
                        )
                    }
                }
        }
    }

    fun generateViralClipsFromPreview() {
        val state = _actionState.value
        val preview = state.fetchedPreview
        if (preview == null && state.urlInput.isNotBlank()) {
            // Fetch first then generate
            viewModelScope.launch {
                _actionState.update {
                    it.copy(
                        isFetchingStream = true,
                        errorMessage = null,
                        statusMessage = "Fetching YouTube stream…"
                    )
                }
                val fetched = youtubeService.fetchYouTubeMetadata(state.urlInput)
                fetched.onSuccess { meta ->
                    _actionState.update {
                        it.copy(
                            isFetchingStream = false,
                            fetchedPreview = meta,
                            notesInput = if (meta.transcriptText.isNotBlank()) meta.transcriptText else it.notesInput
                        )
                    }
                    runAiExtractionAndSave(meta)
                }.onFailure { err ->
                    _actionState.update {
                        it.copy(
                            isFetchingStream = false,
                            errorMessage = err.localizedMessage ?: "Could not fetch YouTube video."
                        )
                    }
                }
            }
            return
        }

        if (preview == null) {
            _actionState.update { it.copy(errorMessage = "Load a YouTube URL or pick a device video first.") }
            return
        }

        viewModelScope.launch {
            runAiExtractionAndSave(preview)
        }
    }

    private suspend fun runAiExtractionAndSave(preview: FetchedStreamMetadata) {
        val state = _actionState.value
        val combinedContext = state.notesInput.ifBlank { preview.transcriptText }
        _actionState.update {
            it.copy(
                isGeneratingClips = true,
                errorMessage = null,
                statusMessage = "Gemini AI is scanning hooks, pacing & viral captions…"
            )
        }
        try {
            val generatedClips = aiService.analyzeStreamForViralClips(
                sourceTitle = preview.title,
                channelName = preview.channelName,
                durationSeconds = preview.durationSeconds,
                transcriptOrNotes = combinedContext,
                targetPlatform = state.selectedPlatform,
                targetClipLengthSec = state.targetLengthSec,
                focusAngle = state.focusAngle
            )

            val project = StreamProject(
                sourceUrl = preview.sourceUrl,
                videoId = preview.videoId,
                sourceTitle = preview.title,
                channelName = preview.channelName,
                thumbnailUrl = preview.thumbnailUrl,
                durationSeconds = preview.durationSeconds,
                transcriptText = combinedContext.take(12000),
                targetPlatform = state.selectedPlatform.name,
                clipCount = generatedClips.size.toLong()
            )

            repository.createProjectWithClips(currentUserId, project, generatedClips)
                .onSuccess { projectId ->
                    _actionState.update {
                        it.copy(
                            isGeneratingClips = false,
                            statusMessage = "${generatedClips.size} viral clips extracted & saved to cloud!",
                            latestGeneratedProjectId = projectId
                        )
                    }
                }
                .onFailure { err ->
                    _actionState.update {
                        it.copy(
                            isGeneratingClips = false,
                            errorMessage = err.localizedMessage ?: "Failed to save clips to Firestore."
                        )
                    }
                }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI clip extraction timed out", e)
            _actionState.update {
                it.copy(
                    isGeneratingClips = false,
                    errorMessage = "AI analysis timed out after 60 seconds. Please retry."
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI clip extraction failed", e)
            _actionState.update {
                it.copy(
                    isGeneratingClips = false,
                    errorMessage = e.localizedMessage ?: "AI extraction failed."
                )
            }
        }
    }

    fun toggleFavorite(projectId: String, clip: ViralClip) {
        viewModelScope.launch {
            repository.toggleFavoriteClip(currentUserId, projectId, clip.id, !clip.favorite)
                .onFailure { err ->
                    _actionState.update {
                        it.copy(errorMessage = err.localizedMessage ?: "Failed to update favorite.")
                    }
                }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(currentUserId, projectId)
                .onFailure { err ->
                    _actionState.update {
                        it.copy(errorMessage = err.localizedMessage ?: "Failed to delete project.")
                    }
                }
        }
    }

    fun deleteClip(projectId: String, clipId: String) {
        viewModelScope.launch {
            repository.deleteClip(currentUserId, projectId, clipId)
                .onFailure { err ->
                    _actionState.update {
                        it.copy(errorMessage = err.localizedMessage ?: "Failed to delete clip.")
                    }
                }
        }
    }
}
