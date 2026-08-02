package com.jean.vocabs.ui.processar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.media.lembrarFoto
import com.jean.vocabs.media.rememberReprodutor
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.FormatoCaptura
import com.jean.vocabs.ui.components.AvisoDuplicata
import com.jean.vocabs.ui.components.BotaoCircular
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.RotuloSecao
import com.jean.vocabs.ui.components.escalaAoPressionar
import com.jean.vocabs.ui.components.tempoRelativo

@Composable
fun ProcessarScreen(
    id: Long,
    aoVoltar: () -> Unit,
    vm: ProcessarViewModel = viewModel(),
) {
    val fluxo = remember(id) { vm.observar(id) }
    val entrada by fluxo.collectAsStateWithLifecycle()
    val duplicata by vm.duplicata.collectAsStateWithLifecycle()
    val atual = entrada

    var trecho by remember { mutableStateOf("") }
    var alvo by remember { mutableStateOf("") }
    var origem by remember { mutableStateOf("") }
    var confirmandoDescarte by remember { mutableStateOf(false) }

    val gerenciadorFoco = LocalFocusManager.current

    // A origem pode ter sido preenchida no momento da captura; o resto nasce vazio.
    LaunchedEffect(atual?.id) {
        atual?.let {
            trecho = it.trecho.orEmpty()
            alvo = it.alvo.orEmpty()
            origem = it.origem.orEmpty()
        }
    }

    LaunchedEffect(alvo, id) { vm.procurarDuplicata(alvo = alvo, ignorarId = id) }

    val podeGerar = trecho.isNotBlank() && alvo.isNotBlank()

    fun gerar() {
        if (!podeGerar) return
        gerenciadorFoco.clearFocus()
        vm.transcrever(id = id, trecho = trecho, alvo = alvo, origem = origem)
        aoVoltar()
    }

    if (confirmandoDescarte) {
        AlertDialog(
            onDismissRequest = { confirmandoDescarte = false },
            title = { Text("Descartar captura?") },
            text = {
                Text(
                    "A gravação ou foto vai junto, e isso não tem volta. " +
                        "Se ainda tiver dúvida, deixe em Pendentes — não custa nada esperar.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmandoDescarte = false
                        vm.excluir(id)
                        aoVoltar()
                    },
                ) {
                    Text("Descartar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoDescarte = false }) { Text("Manter") }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            BotaoCircular(icone = Icones.Voltar, descricao = "Voltar", aoClicar = aoVoltar)
            Spacer(modifier = Modifier.weight(1f))
            BotaoCircular(
                icone = Icones.Lixeira,
                descricao = "Descartar captura",
                cor = MaterialTheme.colorScheme.error,
                aoClicar = { confirmandoDescarte = true },
            )
        }

        if (atual == null) return@Column

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "Transcrever",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = when (atual.formato) {
                    FormatoCaptura.FOTO -> "Capturado ${tempoRelativo(atual.criadoEm)} · foto"
                    FormatoCaptura.AUDIO -> "Capturado ${tempoRelativo(atual.criadoEm)} · áudio"
                    FormatoCaptura.TEXTO -> "Capturado ${tempoRelativo(atual.criadoEm)}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            atual.midiaCaminho?.let { caminho ->
                Box(modifier = Modifier.padding(top = 20.dp)) {
                    when (atual.formato) {
                        FormatoCaptura.FOTO -> PreviaDaFoto(caminho)
                        FormatoCaptura.AUDIO -> PlayerDeAudio(caminho)
                        FormatoCaptura.TEXTO -> Unit
                    }
                }
            }

            RotuloSecao("O QUE ESTAVA ESCRITO", modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))

            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Campo(
                        valor = trecho,
                        aoMudar = { trecho = it },
                        rotulo = "Trecho",
                        dica = "A frase que aparece na captura",
                        minLinhas = 3,
                        opcoesTeclado = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                        ),
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Campo(
                        valor = alvo,
                        aoMudar = { alvo = it },
                        rotulo = "Alvo",
                        dica = "O que te pegou",
                        umaLinha = true,
                        opcoesTeclado = KeyboardOptions(imeAction = ImeAction.Next),
                        acoesTeclado = KeyboardActions(
                            onNext = { gerenciadorFoco.moveFocus(FocusDirection.Down) },
                        ),
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Campo(
                        valor = origem,
                        aoMudar = { origem = it },
                        rotulo = "Origem (opcional)",
                        dica = "jogo, livro, série…",
                        umaLinha = true,
                        opcoesTeclado = KeyboardOptions(imeAction = ImeAction.Done),
                        acoesTeclado = KeyboardActions(onDone = { gerar() }),
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
                onClick = ::gerar,
                enabled = podeGerar,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(56.dp),
            ) {
                Icon(
                    imageVector = Icones.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Gerar ficha",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            Spacer(
                modifier = Modifier
                    .navigationBarsPadding()
                    .height(40.dp),
            )
        }
    }
}

@Composable
private fun PreviaDaFoto(caminho: String) {
    val foto by lembrarFoto(caminho)

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        AnimatedContent(
            targetState = foto,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(150)) },
            label = "previaDaFoto",
        ) { imagem ->
            if (imagem == null) {
                Box(modifier = Modifier.height(220.dp))
            } else {
                Image(
                    bitmap = imagem,
                    contentDescription = "Foto capturada",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                )
            }
        }
    }
}

@Composable
private fun PlayerDeAudio(caminho: String) {
    val reprodutor by rememberReprodutor(caminho)
    val cores = MaterialTheme.colorScheme
    val interacao = remember { MutableInteractionSource() }

    Surface(
        onClick = reprodutor::alternar,
        shape = MaterialTheme.shapes.large,
        color = cores.surface,
        shadowElevation = 1.dp,
        interactionSource = interacao,
        modifier = Modifier
            .fillMaxWidth()
            .escalaAoPressionar(interacao),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(18.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .background(cores.primary, CircleShape),
            ) {
                Icon(
                    imageVector = if (reprodutor.tocando) Icones.Parar else Icones.Tocar,
                    contentDescription = if (reprodutor.tocando) "Parar" else "Ouvir gravação",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
            ) {
                Text(
                    text = if (reprodutor.tocando) "Tocando…" else "Ouvir a gravação",
                    style = MaterialTheme.typography.titleSmall,
                    color = cores.onSurface,
                )
                Text(
                    text = "Escute e escreva o que você disse abaixo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cores.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun Campo(
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
