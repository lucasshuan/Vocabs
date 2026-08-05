package com.jean.vocabs.ui.capture

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.jean.vocabs.media.AudioRecorder
import com.jean.vocabs.shared.domain.CaptureFormat
import com.jean.vocabs.shared.media.MediaFiles
import java.io.File
import kotlinx.coroutines.delay

/**
 * Audio and photo capture, with no screen of its own.
 *
 * The wiring — recorder, camera, microphone permission — is separate from the
 * buttons because what triggers these actions is the bottom bar, which lives
 * outside any screen. The state has to survive a tab change: inside a navigation
 * destination, leaving the tab mid-recording would lose it.
 *
 * Two things the previous version did not give:
 *
 * - **duration in milliseconds**, not a whole-second counter. The
 *   [MIN_RECORDING_MS] cut is 0.8 s; against a counter that only ticks at 1 s it
 *   discarded every short recording and nothing else.
 * - **microphone level**, so the recording screen's wave is speech and not an
 *   animation that runs the same through silence.
 */
@Stable
class QuickCapture internal constructor() {

    var isRecording by mutableStateOf(false)
        internal set

    /**
     * The recording clock, counted from the start rather than accumulated per
     * tick.
     *
     * Deliberately outside Compose state: what needs it is the save-or-discard
     * decision. Publishing every millisecond as state would recompose the screen
     * 20 times a second to write the same "0:07" — [seconds] exists for that.
     */
    val durationMs: Long
        get() = if (isRecording) SystemClock.elapsedRealtime() - startedAt else lastDurationMs

    var seconds by mutableLongStateOf(0L)
        internal set

    /** Whether the microphone is already granted — re-evaluated each composition. */
    var hasAudioPermission by mutableStateOf(false)
        internal set

    internal var startedAt = 0L
    internal var lastDurationMs = 0L

    internal var onRequestAudio: () -> Unit = {}
    internal var onFinish: (Boolean) -> Unit = {}
    internal var onOpenCamera: () -> Unit = {}
    internal var onRequestPermission: () -> Unit = {}
    internal var readLevel: () -> Float = { 0f }

    /**
     * The microphone's peak right now, 0 to 1.
     *
     * A function rather than observable state: the wave samples this at its own
     * rate, and state changing at 60 Hz would recompose the whole recording
     * screen to move nineteen rectangles.
     */
    fun levelNow(): Float = readLevel()

    /** Starts recording immediately — the gesture only arrives with permission. */
    fun recordAudio() = onRequestAudio()

    /** Ends and saves, if it ran past [MIN_RECORDING_MS]. */
    fun saveAudio() = onFinish(true)

    /**
     * Ends without saving. A queue full of half-second audio would cost more to
     * clean up than the gesture saves.
     */
    fun cancelAudio() = onFinish(false)

    fun takePhoto() = onOpenCamera()

    fun requestAudioPermission() = onRequestPermission()
}

/**
 * Below this the recording was a mistake, not a capture.
 *
 * Recording starts on releasing at the audio target and ends on a tap; the
 * shortest path between the two is someone who released on the wrong target and
 * went straight to undo. It measures in milliseconds for that reason.
 */
const val MIN_RECORDING_MS = 800L

/**
 * [onSave] receives the finished capture and decides what to do with it, without
 * a second callback just to notify the screen. The bottom notice needs the
 * duration, the format and the course in the same call that records the capture,
 * because the id the database returns is what links the card to the "Select"
 * shortcut.
 */
@Composable
fun rememberQuickCapture(
    target: String,
    onSave: (format: CaptureFormat, path: String, durationMs: Long?, target: String) -> Unit,
    onNotice: (String) -> Unit,
): QuickCapture {
    val context = LocalContext.current
    val recorder = remember { AudioRecorder(context) }
    val state = remember { QuickCapture() }

    state.hasAudioPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    // The destination file has to exist before calling the camera: the camera app
    // writes into it, it does not hand back a path.
    var pendingPhoto by remember { mutableStateOf<File?>(null) }
    var photoTarget by remember { mutableStateOf(target) }
    val camera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { succeeded ->
        val file = pendingPhoto
        pendingPhoto = null
        if (succeeded && file != null) {
            onSave(CaptureFormat.PHOTO, file.absolutePath, null, photoTarget)
        } else {
            file?.delete()
        }
    }

    /**
     * The destination course is frozen the instant the capture starts.
     *
     * The carousel page can change while the audio runs or the camera is open,
     * and the destination has to be what was marked when the finger went down,
     * not what is on screen when it comes back.
     */
    var recordingTarget by remember { mutableStateOf(target) }

    val audioPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        state.hasAudioPermission = granted
        onNotice(
            if (granted) "Microfone liberado. Segure o + e arraste até o microfone."
            else "Sem microfone não dá para gravar. Dá para capturar por texto ou foto.",
        )
    }

    state.readLevel = { recorder.level }

    state.onOpenCamera = {
        photoTarget = target
        val file = MediaFiles.newPhoto(context)
        pendingPhoto = file
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        camera.launch(uri)
    }

    state.onRequestPermission = { audioPermission.launch(Manifest.permission.RECORD_AUDIO) }

    state.onRequestAudio = {
        if (state.hasAudioPermission && recorder.begin()) {
            recordingTarget = target
            state.startedAt = SystemClock.elapsedRealtime()
            state.seconds = 0
            state.isRecording = true
        }
    }

    state.onFinish = { save ->
        val duration = state.durationMs
        state.lastDurationMs = duration
        state.isRecording = false
        val short = duration < MIN_RECORDING_MS
        if (!save || short) {
            recorder.cancel()
            if (save && short) onNotice("Curto demais para guardar.")
        } else {
            recorder.stop()?.let { file ->
                onSave(CaptureFormat.AUDIO, file.absolutePath, duration, recordingTarget)
            }
        }
    }

    // If the app dies mid-recording the half file is discarded rather than
    // becoming a mute audio in the pending list.
    DisposableEffect(Unit) {
        onDispose { if (recorder.isRecording) recorder.cancel() }
    }

    // The timer publishes only the turn of the second. The real precision is in
    // `durationMs`, read straight off the clock when someone asks.
    LaunchedEffect(state.isRecording) {
        while (state.isRecording) {
            state.seconds = state.durationMs / 1_000L
            delay(120)
        }
    }

    return state
}
