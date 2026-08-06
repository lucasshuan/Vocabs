package io.github.lucasshuan.vocabu.ui.capture

import androidx.compose.ui.platform.LocalResources
import io.github.lucasshuan.vocabu.R
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
import io.github.lucasshuan.vocabu.media.AudioRecorder
import io.github.lucasshuan.vocabu.shared.domain.CaptureFormat
import io.github.lucasshuan.vocabu.shared.media.MediaFiles
import java.io.File
import kotlinx.coroutines.delay

/**
 * Held outside any navigation destination, because the bottom bar triggers it:
 * inside one, leaving the tab mid-recording would lose the state.
 *
 * Duration in millis, not whole seconds: against a 1s counter the 0.8s
 * [MIN_RECORDING_MS] cut discarded every short recording and nothing else.
 */
@Stable
class QuickCapture internal constructor() {

    var isRecording by mutableStateOf(false)
        internal set

    /**
     * Outside Compose state: only the save-or-discard decision reads it, and
     * publishing it would recompose 20 times a second to write the same "0:07".
     */
    val durationMs: Long
        get() = if (isRecording) SystemClock.elapsedRealtime() - startedAt else lastDurationMs

    var seconds by mutableLongStateOf(0L)
        internal set

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
     * A function, not observable state: the wave samples at its own rate, and
     * 60Hz state would recompose the screen to move nineteen rectangles.
     */
    fun levelNow(): Float = readLevel()

    fun recordAudio() = onRequestAudio()

    fun saveAudio() = onFinish(true)

    fun cancelAudio() = onFinish(false)

    fun takePhoto() = onOpenCamera()

    fun requestAudioPermission() = onRequestPermission()
}

/** Below this it was a slip: someone released on the wrong target. */
const val MIN_RECORDING_MS = 800L

/**
 * [onSave] gets duration, format and course in the same call that records the
 * capture: the id the database returns is what links the notice to "Select".
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

    // The file has to exist first: the camera app writes into it and hands back
    // no path.
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
     * Frozen at the start: the carousel page can change while the audio runs,
     * and the destination is what was marked when the finger went down.
     */
    var recordingTarget by remember { mutableStateOf(target) }

    // LocalResources, not LocalContext: only it invalidates on configuration
    // change, and these strings are read from a callback.
    val resources = LocalResources.current

    val audioPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        state.hasAudioPermission = granted
        onNotice(
            if (granted) resources.getString(R.string.capture_mic_granted)
            else resources.getString(R.string.capture_mic_denied),
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
            if (save && short) onNotice(resources.getString(R.string.capture_too_short))
        } else {
            recorder.stop()?.let { file ->
                onSave(CaptureFormat.AUDIO, file.absolutePath, duration, recordingTarget)
            }
        }
    }

    // A half file is discarded rather than becoming mute audio in Pending.
    DisposableEffect(Unit) {
        onDispose { if (recorder.isRecording) recorder.cancel() }
    }

    // Publishes only the turn of the second; `durationMs` holds the precision.
    LaunchedEffect(state.isRecording) {
        while (state.isRecording) {
            state.seconds = state.durationMs / 1_000L
            delay(120)
        }
    }

    return state
}
