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
 * Captura de áudio e foto sem tela própria.
 *
 * A fiação — gravador, câmera, permissão de microfone — foi separada dos botões
 * porque quem dispara essas ações agora é a barra inferior, que vive fora de
 * qualquer tela. O estado precisa sobreviver à troca de aba: se morasse dentro
 * de um destino de navegação, sair da aba no meio de uma gravação a perderia.
 *
 * O gesto do handoff pede duas coisas que a versão anterior não dava:
 *
 * - **duração em milissegundos**, e não o contador de segundos inteiros. O corte
 *   de [MINIMO_DE_GRAVACAO_MS] mede 0,8 s; comparado contra um contador que só
 *   vira em 1 s, ele descartava toda gravação curta e nenhuma outra.
 * - **nível do microfone**, para a onda da tela de gravação ser a fala e não uma
 *   animação que corre igual no silêncio.
 */
@Stable
class QuickCapture internal constructor() {

    var isRecording by mutableStateOf(false)
        internal set

    /**
     * O relógio da gravação, contado do início e não acumulado a cada tique.
     *
     * Fica fora do estado do Compose de propósito: quem precisa dele é a decisão
     * de guardar ou descartar, no instante em que o dedo sai da tela. Publicar
     * cada milissegundo como estado obrigaria a tela a recompor 20 vezes por
     * segundo para escrever o mesmo "0:07" — [segundos] existe para isso.
     */
    val durationMs: Long
        get() = if (isRecording) SystemClock.elapsedRealtime() - startedAt else lastDurationMs

    var seconds by mutableLongStateOf(0L)
        internal set

    /** Se o microfone já está liberado — reavaliado a cada composição. */
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
     * O pico do microfone agora, de 0 a 1.
     *
     * Função e não propriedade observável: a onda amostra isto no ritmo dela, e
     * um estado que mudasse a 60 Hz recomporia a tela de gravação inteira para
     * mexer dezenove retângulos.
     */
    fun levelNow(): Float = readLevel()

    /** Começa a gravar já — o gesto só chega aqui com a permissão em mãos. */
    fun recordAudio() = onRequestAudio()

    /** Encerra e guarda, se passou de [MINIMO_DE_GRAVACAO_MS]. */
    fun saveAudio() = onFinish(true)

    /**
     * Encerra sem guardar — o botão de descartar da tela de gravação. Uma fila
     * cheia de áudios de meio segundo custaria mais para limpar do que o gesto
     * economiza.
     */
    fun cancelAudio() = onFinish(false)

    fun takePhoto() = onOpenCamera()

    fun requestAudioPermission() = onRequestPermission()
}

/**
 * Abaixo disto a gravação foi um engano, e não uma captura.
 *
 * A gravação começa ao soltar no alvo do áudio e termina num toque; o caminho
 * mais curto entre os dois é alguém que soltou no alvo errado e foi direto
 * desfazer. O áudio de alguns décimos que sai daí é o que este número joga fora,
 * e é por isso que ele mede em milissegundos.
 */
const val MIN_RECORDING_MS = 800L

/**
 * [aoGuardar] recebe a captura pronta e é quem decide o que fazer com ela — sem
 * um segundo retorno de chamada só para avisar a tela. Os dois existiam antes e
 * eram a mesma coisa dita em dois lugares; agora o aviso de baixo precisa da
 * duração, do formato e do curso na mesma chamada em que a captura é gravada,
 * porque é o id devolvido pelo banco que liga o cartão ao atalho "Selecionar".
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

    // O arquivo de destino precisa existir antes de chamar a câmera: o app de
    // câmera escreve nele, não devolve um caminho.
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
     * O curso de destino é congelado no instante em que a captura começa.
     *
     * A pessoa pode trocar de página do carrossel enquanto o áudio corre ou
     * enquanto a câmera está aberta — e o destino tem que ser o que estava
     * marcado quando o dedo desceu, não o que estiver na tela quando voltar.
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

    // Se o app morrer gravando, o arquivo pela metade é descartado em vez de
    // virar um áudio mudo na lista de pendentes.
    DisposableEffect(Unit) {
        onDispose { if (recorder.isRecording) recorder.cancel() }
    }

    // O cronômetro publica só a virada do segundo. A precisão de verdade está em
    // `duracaoMs`, que é lido direto do relógio quando alguém pergunta.
    LaunchedEffect(state.isRecording) {
        while (state.isRecording) {
            state.seconds = state.durationMs / 1_000L
            delay(120)
        }
    }

    return state
}
