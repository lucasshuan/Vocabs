package com.jean.vocabs.ui.processar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.media.lembrarFoto
import com.jean.vocabs.media.rememberReprodutor
import com.jean.vocabs.shared.domain.AlvoSelecionado
import com.jean.vocabs.shared.domain.Captura
import com.jean.vocabs.shared.domain.FormatoCaptura
import com.jean.vocabs.shared.domain.StatusCaptura
import com.jean.vocabs.ui.components.AvisoDuplicata
import com.jean.vocabs.ui.components.BotaoCircular
import com.jean.vocabs.ui.components.BotaoPrincipal
import com.jean.vocabs.ui.components.CartaoDaTela
import com.jean.vocabs.ui.components.ChipsDeSelecao
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.RotuloDeSecao
import com.jean.vocabs.ui.components.SeletorDeTermos
import com.jean.vocabs.ui.components.formatarDuracaoMs
import com.jean.vocabs.ui.components.tempoRelativo

@Composable
fun ProcessarScreen(
    id: Long,
    aoVoltar: () -> Unit,
    vm: ProcessarViewModel = viewModel(),
) {
    val fluxo = remember(id) { vm.observar(id) }
    val captura by fluxo.collectAsStateWithLifecycle()
    val duplicata by vm.duplicata.collectAsStateWithLifecycle()
    var trecho by remember { mutableStateOf("") }
    val selecoes = remember { mutableStateListOf<AlvoSelecionado>() }
    var confirmarExclusao by remember { mutableStateOf(false) }

    LaunchedEffect(captura?.id, captura?.trecho) {
        trecho = captura?.trecho.orEmpty()
        selecoes.clear()
    }
    LaunchedEffect(selecoes.lastOrNull()?.texto) {
        vm.procurarDuplicata(selecoes.lastOrNull()?.texto.orEmpty())
    }

    if (confirmarExclusao) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            title = { Text("Descartar captura?") },
            text = { Text("A mídia será removida porque ainda não há fichas ligadas a ela.") },
            confirmButton = {
                TextButton(onClick = { vm.excluir(id); confirmarExclusao = false; aoVoltar() }) {
                    Text("Descartar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmarExclusao = false }) { Text("Manter") } },
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
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = 14.dp, top = 12.dp, bottom = 4.dp),
        ) {
            BotaoCircular(Icones.Voltar, "Voltar", aoVoltar)
            Text(
                text = captura?.let(::tituloDaCaptura) ?: "Captura",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            BotaoCircular(Icones.Lixeira, "Descartar", { confirmarExclusao = true }, MaterialTheme.colorScheme.error)
        }

        val atual = captura
        if (atual == null) return@Column
        Column(Modifier.padding(horizontal = 20.dp)) {
            atual.midiaCaminho?.let { caminho ->
                when (atual.formato) {
                    FormatoCaptura.FOTO -> PreviaFoto(caminho)
                    FormatoCaptura.AUDIO -> PlayerAudio(caminho, atual.duracaoMs)
                    FormatoCaptura.TEXTO -> Unit
                }
            }

            when {
                atual.status == StatusCaptura.TRANSCREVENDO -> Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text("Transcrição local em andamento…", Modifier.padding(start = 12.dp))
                    }
                }
                atual.erroTranscricao != null -> Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                ) { Text(atual.erroTranscricao.orEmpty(), color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(14.dp)) }
            }

            RotuloDeSecao("Transcrição", Modifier.padding(top = 18.dp, bottom = 8.dp))
            OutlinedTextField(
                value = trecho,
                onValueChange = { trecho = it; selecoes.clear() },
                placeholder = { Text("Digite o trecho manualmente") },
                minLines = 2,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            if (trecho.isNotBlank()) {
                RotuloDeSecao("Selecione o que chamou atenção", Modifier.padding(top = 18.dp, bottom = 9.dp))
                SeletorDeTermos(trecho) { alvo ->
                    if (selecoes.none { it.inicio == alvo.inicio && it.fim == alvo.fim }) selecoes += alvo
                }
                Text(
                    "Toque numa palavra ou arraste por várias.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (selecoes.isNotEmpty()) {
                RotuloDeSecao(
                    "${selecoes.size} ${if (selecoes.size == 1) "selecionada" else "selecionadas"}",
                    Modifier.padding(top = 18.dp, bottom = 9.dp),
                )
                ChipsDeSelecao(selecoes, selecoes::remove)
            }

            duplicata?.let { AvisoDuplicata(it, Modifier.padding(top = 14.dp)) }

            Text(
                "Nada vira ficha sem você confirmar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 10.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            BotaoPrincipal(
                texto = if (selecoes.isEmpty()) "Selecione o que guardar" else "Guardar ${selecoes.size} ${if (selecoes.size == 1) "captura" else "capturas"}",
                aoClicar = { vm.confirmar(id, trecho, selecoes.toList()); aoVoltar() },
                habilitado = trecho.isNotBlank() && selecoes.isNotEmpty(),
            )
        }
        Spacer(Modifier.navigationBarsPadding().height(40.dp))
    }
}

/** "Áudio · 0:12", "Foto · há 1h" — o mesmo nome que a linha de Pendentes deu a ela. */
private fun tituloDaCaptura(captura: Captura): String = when (captura.formato) {
    FormatoCaptura.AUDIO -> captura.duracaoMs?.let { "Áudio · ${formatarDuracaoMs(it)}" } ?: "Áudio"
    FormatoCaptura.FOTO -> "Foto · ${tempoRelativo(captura.criadoEm)}"
    FormatoCaptura.TEXTO -> "Texto · ${tempoRelativo(captura.criadoEm)}"
}

@Composable
private fun PreviaFoto(caminho: String) {
    val imagem by lembrarFoto(caminho)
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        imagem?.let { Image(it, "Foto capturada", contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth()) }
            ?: Box(Modifier.height(180.dp))
    }
}

/** Play, onda e duração — a barra do handoff, com a onda como cenário fixo. */
@Composable
private fun PlayerAudio(caminho: String, duracaoMs: Long?) {
    val player by rememberReprodutor(caminho)
    CartaoDaTela(
        forma = MaterialTheme.shapes.large,
        recheio = androidx.compose.foundation.layout.PaddingValues(14.dp),
        aoClicar = player::alternar,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape)) {
                Icon(
                    imageVector = if (player.tocando) Icones.Parar else Icones.Tocar,
                    contentDescription = if (player.tocando) "Parar" else "Ouvir",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
            OndaDeAudio(Modifier.weight(1f))
            Text(
                text = duracaoMs?.let(::formatarDuracaoMs) ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val alturasDaOnda = listOf(0.40f, 0.70f, 1f, 0.55f, 0.80f, 0.35f, 0.65f, 0.45f, 0.90f, 0.30f)

@Composable
private fun OndaDeAudio(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = modifier.height(24.dp),
    ) {
        alturasDaOnda.forEach { fracao ->
            Box(
                Modifier
                    .weight(1f)
                    .height(24.dp * fracao)
                    .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp)),
            )
        }
    }
}
