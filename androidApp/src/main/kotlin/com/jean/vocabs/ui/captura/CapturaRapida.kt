package com.jean.vocabs.ui.captura

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
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
import com.jean.vocabs.shared.domain.FormatoCaptura
import com.jean.vocabs.shared.media.ArquivosDeMidia
import kotlinx.coroutines.delay
import java.io.File

/**
 * Captura de áudio e foto sem tela própria.
 *
 * A fiação — gravador, câmera, permissão de microfone — foi separada dos botões
 * porque quem dispara essas ações agora é a barra inferior, que vive fora de
 * qualquer tela. O estado precisa sobreviver à troca de aba: se morasse dentro
 * de um destino de navegação, sair da aba no meio de uma gravação a perderia.
 */
@Stable
class CapturaRapida internal constructor() {

    var gravando by mutableStateOf(false)
        internal set

    var segundos by mutableLongStateOf(0L)
        internal set

    internal var aoPedirAudio: () -> Unit = {}
    internal var aoParar: () -> Unit = {}
    internal var aoAbrirCamera: () -> Unit = {}

    /** Pede a permissão se precisar; com ela em mãos, grava já no primeiro toque. */
    fun gravarAudio() = aoPedirAudio()

    fun pararAudio() = aoParar()

    fun tirarFoto() = aoAbrirCamera()
}

@Composable
fun rememberCapturaRapida(
    aoSalvarMidia: (FormatoCaptura, String, Long?) -> Unit,
    aoCapturado: (FormatoCaptura) -> Unit,
): CapturaRapida {
    val contexto = LocalContext.current
    val gravador = remember { GravadorDeAudio(contexto) }
    val estado = remember { CapturaRapida() }

    // O arquivo de destino precisa existir antes de chamar a câmera: o app de
    // câmera escreve nele, não devolve um caminho.
    var fotoPendente by remember { mutableStateOf<File?>(null) }
    val camera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { deuCerto ->
        val arquivo = fotoPendente
        fotoPendente = null
        if (deuCerto && arquivo != null) {
            aoSalvarMidia(FormatoCaptura.FOTO, arquivo.absolutePath, null)
            aoCapturado(FormatoCaptura.FOTO)
        } else {
            arquivo?.delete()
        }
    }

    val permissaoAudio = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedida -> if (concedida) estado.gravando = gravador.iniciar() }

    estado.aoAbrirCamera = {
        val arquivo = ArquivosDeMidia.novaFoto(contexto)
        fotoPendente = arquivo
        val uri: Uri = FileProvider.getUriForFile(
            contexto,
            "${contexto.packageName}.fileprovider",
            arquivo,
        )
        camera.launch(uri)
    }

    estado.aoPedirAudio = {
        val jaTem = ContextCompat.checkSelfPermission(
            contexto,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

        if (jaTem) {
            estado.gravando = gravador.iniciar()
        } else {
            permissaoAudio.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    estado.aoParar = {
        val duracaoMs = estado.segundos * 1_000L
        estado.gravando = false
        gravador.parar()?.let { arquivo ->
            aoSalvarMidia(FormatoCaptura.AUDIO, arquivo.absolutePath, duracaoMs)
            aoCapturado(FormatoCaptura.AUDIO)
        }
    }

    // Se o app morrer gravando, o arquivo pela metade é descartado em vez de
    // virar um áudio mudo na lista de pendentes.
    DisposableEffect(Unit) {
        onDispose { if (gravador.gravando) gravador.cancelar() }
    }

    LaunchedEffect(estado.gravando) {
        estado.segundos = 0
        while (estado.gravando) {
            delay(1_000)
            estado.segundos++
        }
    }

    return estado
}
