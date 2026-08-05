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
import com.jean.vocabs.media.GravadorDeAudio
import com.jean.vocabs.shared.domain.CaptureFormat
import com.jean.vocabs.shared.media.MediaFiles
import kotlinx.coroutines.delay
import java.io.File

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
class CapturaRapida internal constructor() {

    var gravando by mutableStateOf(false)
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
        get() = if (gravando) SystemClock.elapsedRealtime() - comecouEm else ultimaDuracaoMs

    var segundos by mutableLongStateOf(0L)
        internal set

    /** Se o microfone já está liberado — reavaliado a cada composição. */
    var temPermissaoDeAudio by mutableStateOf(false)
        internal set

    internal var comecouEm = 0L
    internal var ultimaDuracaoMs = 0L

    internal var aoPedirAudio: () -> Unit = {}
    internal var aoEncerrar: (Boolean) -> Unit = {}
    internal var aoAbrirCamera: () -> Unit = {}
    internal var aoPedirPermissao: () -> Unit = {}
    internal var lerNivel: () -> Float = { 0f }

    /**
     * O pico do microfone agora, de 0 a 1.
     *
     * Função e não propriedade observável: a onda amostra isto no ritmo dela, e
     * um estado que mudasse a 60 Hz recomporia a tela de gravação inteira para
     * mexer dezenove retângulos.
     */
    fun nivelAgora(): Float = lerNivel()

    /** Começa a gravar já — o gesto só chega aqui com a permissão em mãos. */
    fun gravarAudio() = aoPedirAudio()

    /** Encerra e guarda, se passou de [MINIMO_DE_GRAVACAO_MS]. */
    fun guardarAudio() = aoEncerrar(true)

    /**
     * Encerra sem guardar — o botão de descartar da tela de gravação. Uma fila
     * cheia de áudios de meio segundo custaria mais para limpar do que o gesto
     * economiza.
     */
    fun cancelarAudio() = aoEncerrar(false)

    fun tirarFoto() = aoAbrirCamera()

    fun pedirPermissaoDeAudio() = aoPedirPermissao()
}

/**
 * Abaixo disto a gravação foi um engano, e não uma captura.
 *
 * A gravação começa ao soltar no alvo do áudio e termina num toque; o caminho
 * mais curto entre os dois é alguém que soltou no alvo errado e foi direto
 * desfazer. O áudio de alguns décimos que sai daí é o que este número joga fora,
 * e é por isso que ele mede em milissegundos.
 */
const val MINIMO_DE_GRAVACAO_MS = 800L

/**
 * [aoGuardar] recebe a captura pronta e é quem decide o que fazer com ela — sem
 * um segundo retorno de chamada só para avisar a tela. Os dois existiam antes e
 * eram a mesma coisa dita em dois lugares; agora o aviso de baixo precisa da
 * duração, do formato e do curso na mesma chamada em que a captura é gravada,
 * porque é o id devolvido pelo banco que liga o cartão ao atalho "Selecionar".
 */
@Composable
fun rememberCapturaRapida(
    target: String,
    aoGuardar: (format: CaptureFormat, path: String, durationMs: Long?, target: String) -> Unit,
    aoRecado: (String) -> Unit,
): CapturaRapida {
    val contexto = LocalContext.current
    val gravador = remember { GravadorDeAudio(contexto) }
    val estado = remember { CapturaRapida() }

    estado.temPermissaoDeAudio = ContextCompat.checkSelfPermission(
        contexto,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    // O arquivo de destino precisa existir antes de chamar a câmera: o app de
    // câmera escreve nele, não devolve um caminho.
    var fotoPendente by remember { mutableStateOf<File?>(null) }
    var alvoDaFoto by remember { mutableStateOf(target) }
    val camera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { deuCerto ->
        val file = fotoPendente
        fotoPendente = null
        if (deuCerto && file != null) {
            aoGuardar(CaptureFormat.PHOTO, file.absolutePath, null, alvoDaFoto)
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
    var alvoDaGravacao by remember { mutableStateOf(target) }

    val permissaoAudio = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedida ->
        estado.temPermissaoDeAudio = concedida
        aoRecado(
            if (concedida) "Microfone liberado. Segure o + e arraste até o microfone."
            else "Sem microfone não dá para gravar. Dá para capturar por texto ou foto.",
        )
    }

    estado.lerNivel = { gravador.level }

    estado.aoAbrirCamera = {
        alvoDaFoto = target
        val file = MediaFiles.newPhoto(contexto)
        fotoPendente = file
        val uri: Uri = FileProvider.getUriForFile(
            contexto,
            "${contexto.packageName}.fileprovider",
            file,
        )
        camera.launch(uri)
    }

    estado.aoPedirPermissao = { permissaoAudio.launch(Manifest.permission.RECORD_AUDIO) }

    estado.aoPedirAudio = {
        if (estado.temPermissaoDeAudio && gravador.iniciar()) {
            alvoDaGravacao = target
            estado.comecouEm = SystemClock.elapsedRealtime()
            estado.segundos = 0
            estado.gravando = true
        }
    }

    estado.aoEncerrar = { guardar ->
        val duracao = estado.durationMs
        estado.ultimaDuracaoMs = duracao
        estado.gravando = false
        val curta = duracao < MINIMO_DE_GRAVACAO_MS
        if (!guardar || curta) {
            gravador.cancelar()
            if (guardar && curta) aoRecado("Curto demais para guardar.")
        } else {
            gravador.parar()?.let { file ->
                aoGuardar(CaptureFormat.AUDIO, file.absolutePath, duracao, alvoDaGravacao)
            }
        }
    }

    // Se o app morrer gravando, o arquivo pela metade é descartado em vez de
    // virar um áudio mudo na lista de pendentes.
    DisposableEffect(Unit) {
        onDispose { if (gravador.gravando) gravador.cancelar() }
    }

    // O cronômetro publica só a virada do segundo. A precisão de verdade está em
    // `duracaoMs`, que é lido direto do relógio quando alguém pergunta.
    LaunchedEffect(estado.gravando) {
        while (estado.gravando) {
            estado.segundos = estado.durationMs / 1_000L
            delay(120)
        }
    }

    return estado
}
