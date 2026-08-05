package io.github.lucasshuan.vocabu.ui.capture

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.lucasshuan.vocabu.media.AudioTranscriber
import io.github.lucasshuan.vocabu.media.PhotoTranscriber
import io.github.lucasshuan.vocabu.shared.AppContainer
import io.github.lucasshuan.vocabu.shared.domain.CaptureFormat
import io.github.lucasshuan.vocabu.shared.domain.LanguagePair
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * What the `+` sheet needs to know: which languages can be saved to, and which
 * one arrives marked.
 *
 * The marked one is the open course. There is no third notion of "last capture
 * language" — it would be one more state to diverge from what the screen shows.
 */
data class CaptureState(
    val languagePair: LanguagePair = LanguagePair.DEFAULT,
    val courses: List<String> = emptyList(),
)

class CaptureViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = AppContainer.repository(app)
    private val preferences = AppContainer.preferences(app)
    private val photoTranscriber = PhotoTranscriber(app)
    private val audioTranscriber = AudioTranscriber(app)

    val state: StateFlow<CaptureState> = combine(
        preferences.observeLanguagePair(),
        preferences.observeCourses(),
        ::CaptureState,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CaptureState())

    /**
     * Saves the snippet and returns the capture, which is where the selection
     * goes.
     *
     * "Continue" records before there is any selection on purpose: from here on,
     * abandoning the marking, closing the app or losing the connection leaves the
     * capture in Pending with the language already chosen, instead of throwing
     * away what was pasted.
     */
    fun saveSnippet(snippet: String, target: String, onReady: (Long) -> Unit) {
        val languagePair = LanguagePair(native = state.value.languagePair.native, target = target)
        viewModelScope.launch {
            onReady(repository.captureSnippet(snippet, languagePair))
        }
    }

    /**
     * OCR and speech are local and slow; the capture is already saved before them.
     *
     * Runs in the application scope because the sheet closes immediately: tied to
     * `viewModelScope`, the screen's cancellation would leave the capture in
     * TRANSCRIBING forever.
     */
    fun saveMedia(
        format: CaptureFormat,
        path: String,
        durationMs: Long?,
        target: String,
        onIdentify: (Long) -> Unit = {},
    ) {
        val languagePair = LanguagePair(native = state.value.languagePair.native, target = target)
        AppContainer.scope.launch {
            val id = repository.captureMedia(format, path, durationMs, languagePair)
            onIdentify(id)
            val result = when (format) {
                CaptureFormat.PHOTO -> photoTranscriber.transcribe(path)
                CaptureFormat.AUDIO -> audioTranscriber.transcribe(path)
                CaptureFormat.TEXT -> return@launch
            }
            repository.recordTranscription(id, result.text, result.error)
        }
    }
}
