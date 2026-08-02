package com.jean.vocabs.ui.captura

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.media.GravadorDeAudio
import com.jean.vocabs.shared.domain.FormatoCaptura
import com.jean.vocabs.shared.media.ArquivosDeMidia
import com.jean.vocabs.ui.components.AvisoDuplicata
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.escalaAoPressionar
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun CapturaScreen(
    aoCapturarTexto: () -> Unit,
    aoCapturarMidia: (FormatoCaptura) -> Unit,
    vm: CapturaViewModel = viewModel(),
) {
    val contexto = LocalContext.current

    var trecho by rememberSaveable { mutableStateOf("") }
    var alvo by rememberSaveable { mutableStateOf("") }
    var origem by rememberSaveable { mutableStateOf("") }

    val duplicata by vm.duplicata.collectAsStateWithLifecycle()

    val focoTrecho = remember { FocusRequester() }
    val gerenciadorFoco = LocalFocusManager.current

    // É isto que faz a captura caber em 10 segundos: o teclado já sobe com o
    // cursor no campo certo, sem um toque a mais.
    LaunchedEffect(Unit) { focoTrecho.requestFocus() }

    LaunchedEffect(alvo) { vm.procurarDuplicata(alvo) }

    val podeSalvar = trecho.isNotBlank() && alvo.isNotBlank()

    fun salvarTexto() {
        if (!podeSalvar) return
        vm.salvarTexto(trecho = trecho, alvo = alvo, origem = origem)
        trecho = ""
        alvo = ""
        gerenciadorFoco.clearFocus()
        aoCapturarTexto()
    }

    // --- Foto -------------------------------------------------------------
    // O arquivo de destino precisa existir antes de chamar a câmera: o app de
    // câmera escreve nele, não devolve um caminho.
    var fotoPendente by remember { mutableStateOf<File?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { deuCerto ->
        val arquivo = fotoPendente
        fotoPendente = null
        if (deuCerto && arquivo != null) {
            vm.salvarMidia(FormatoCaptura.FOTO, arquivo.absolutePath, origem)
            aoCapturarMidia(FormatoCaptura.FOTO)
        } else {
            arquivo?.delete()
        }
    }

    fun abrirCamera() {
        val arquivo = ArquivosDeMidia.novaFoto(contexto)
        fotoPendente = arquivo
        val uri: Uri = FileProvider.getUriForFile(
            contexto,
            "${contexto.packageName}.fileprovider",
            arquivo,
        )
        camera.launch(uri)
    }

    // --- Áudio ------------------------------------------------------------
    val gravador = remember { GravadorDeAudio(contexto) }
    var gravando by remember { mutableStateOf(false) }
    var segundos by remember { mutableLongStateOf(0L) }

    // Se a tela morrer gravando, o arquivo pela metade é descartado em vez de
    // virar um áudio mudo no inbox.
    DisposableEffect(Unit) {
        onDispose { if (gravador.gravando) gravador.cancelar() }
    }

    LaunchedEffect(gravando) {
        segundos = 0
        while (gravando) {
            delay(1_000)
            segundos++
        }
    }

    fun iniciarGravacao() {
        gerenciadorFoco.clearFocus()
        gravando = gravador.iniciar()
    }

    val permissaoAudio = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedida -> if (concedida) iniciarGravacao() }

    fun pedirAudio() {
        val jaTem = ContextCompat.checkSelfPermission(contexto, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (jaTem) iniciarGravacao() else permissaoAudio.launch(Manifest.permission.RECORD_AUDIO)
    }

    fun pararGravacao() {
        gravando = false
        val arquivo = gravador.parar()
        if (arquivo != null) {
            vm.salvarMidia(FormatoCaptura.AUDIO, arquivo.absolutePath, origem)
            aoCapturarMidia(FormatoCaptura.AUDIO)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = "Capturar",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = "O que te pegou?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        // Mídia primeiro: é a captura mais rápida, e o roadmap mede justamente
        // o tempo de jogar o sinal cru no app.
        AnimatedContent(
            targetState = gravando,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
            label = "areaDeMidia",
            modifier = Modifier.padding(top = 20.dp),
        ) { estaGravando ->
            if (estaGravando) {
                PainelGravando(segundos = segundos, aoParar = ::pararGravacao)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BotaoMidia(
                        rotulo = "Áudio",
                        icone = Icones.Microfone,
                        cor = MaterialTheme.colorScheme.primary,
                        aoClicar = ::pedirAudio,
                        modifier = Modifier.weight(1f),
                    )
                    BotaoMidia(
                        rotulo = "Foto",
                        icone = Icones.Camera,
                        cor = MaterialTheme.colorScheme.tertiary,
                        aoClicar = ::abrirCamera,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Text(
            text = "Foto e áudio entram crus no inbox. Você transcreve depois, com calma.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )

        SeparadorOu(modifier = Modifier.padding(vertical = 20.dp))

        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                CampoCaptura(
                    valor = trecho,
                    aoMudar = { trecho = it },
                    rotulo = "Trecho",
                    dica = "A frase onde apareceu",
                    minLinhas = 3,
                    opcoesTeclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.focusRequester(focoTrecho),
                )
                DivisorSuave()
                CampoCaptura(
                    valor = alvo,
                    aoMudar = { alvo = it },
                    rotulo = "Alvo",
                    dica = "O que te pegou (1 palavra ou várias)",
                    umaLinha = true,
                    opcoesTeclado = KeyboardOptions(imeAction = ImeAction.Next),
                    acoesTeclado = KeyboardActions(
                        onNext = { gerenciadorFoco.moveFocus(FocusDirection.Down) },
                    ),
                )
                DivisorSuave()
                CampoCaptura(
                    valor = origem,
                    aoMudar = { origem = it },
                    rotulo = "Origem (opcional)",
                    dica = "jogo, livro, série…",
                    umaLinha = true,
                    opcoesTeclado = KeyboardOptions(imeAction = ImeAction.Done),
                    acoesTeclado = KeyboardActions(onDone = { salvarTexto() }),
                )
            }
        }

        AnimatedVisibility(
            visible = duplicata != null,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(140)),
        ) {
            duplicata?.let { entrada ->
                AvisoDuplicata(
                    entrada = entrada,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        Button(
            onClick = ::salvarTexto,
            enabled = podeSalvar,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .height(56.dp),
        ) {
            Icon(
                imageVector = Icones.Mais,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Salvar captura",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Text(
            text = "A ficha é gerada sozinha depois. Você pode voltar ao que estava fazendo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )

        Spacer(modifier = Modifier.height(150.dp))
    }
}

@Composable
private fun BotaoMidia(
    rotulo: String,
    icone: ImageVector,
    cor: Color,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interacao = remember { MutableInteractionSource() }
    Surface(
        onClick = aoClicar,
        shape = MaterialTheme.shapes.large,
        color = cor,
        interactionSource = interacao,
        modifier = modifier
            .height(58.dp)
            .escalaAoPressionar(interacao),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = icone,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = rotulo,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** Estado de gravação: um alvo grande e óbvio para parar, e o tempo correndo. */
@Composable
private fun PainelGravando(segundos: Long, aoParar: () -> Unit) {
    val pulso by rememberInfiniteTransition(label = "gravando").animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulsoGravacao",
    )
    val vermelho = Color(0xFFE5484D)

    Surface(
        onClick = aoParar,
        shape = MaterialTheme.shapes.large,
        color = vermelho.copy(alpha = 0.12f),
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .graphicsLayer { alpha = pulso }
                    .background(vermelho, CircleShape),
            )
            Text(
                text = "Gravando  ${formatarDuracao(segundos)}",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = vermelho,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            )
            Icon(
                imageVector = Icones.Parar,
                contentDescription = "Parar gravação",
                tint = vermelho,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Parar",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = vermelho,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

private fun formatarDuracao(segundos: Long): String =
    "%d:%02d".format(segundos / 60, segundos % 60)

@Composable
private fun SeparadorOu(modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "ou escreva",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Campo sem borda dentro do cartão — a moldura é o próprio cartão. */
@Composable
private fun CampoCaptura(
    valor: String,
    aoMudar: (String) -> Unit,
    rotulo: String,
    dica: String,
    modifier: Modifier = Modifier,
    minLinhas: Int = 1,
    umaLinha: Boolean = false,
    opcoesTeclado: KeyboardOptions = KeyboardOptions.Default,
    acoesTeclado: KeyboardActions = KeyboardActions.Default,
) {
    TextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        placeholder = { Text(dica) },
        minLines = minLinhas,
        singleLine = umaLinha,
        keyboardOptions = opcoesTeclado,
        keyboardActions = acoesTeclado,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun DivisorSuave() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}
