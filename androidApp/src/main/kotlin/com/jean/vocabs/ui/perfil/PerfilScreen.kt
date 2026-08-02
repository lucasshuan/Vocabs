package com.jean.vocabs.ui.perfil

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.ui.components.BotaoCircular
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.escalaAoPressionar
import com.jean.vocabs.ui.idiomas.Idiomas

/**
 * Virou aba, então não tem botão de voltar: a própria barra é a saída, e uma
 * seta que faz o mesmo que a aba ao lado só ocuparia espaço.
 */
@Composable
fun PerfilScreen(vm: PerfilViewModel = viewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var escolhendoIdioma by remember { mutableStateOf(false) }

    if (escolhendoIdioma) {
        SeletorDeIdiomaNativo(aoFechar = { escolhendoIdioma = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            BarraDeIdiomaNativo(aoTrocar = { escolhendoIdioma = true })
            Text(
                text = "Seu progresso",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = "Tudo isto vive só neste aparelho.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 24.dp),
            ) {
                Numero(
                    valor = "${estado.totalPalavras}",
                    rotulo = if (estado.totalPalavras == 1) "palavra" else "palavras",
                    modifier = Modifier.weight(1f),
                )
                Numero(
                    valor = "${estado.dominadas}",
                    rotulo = "dominadas",
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Numero(
                    valor = "${estado.diasSeguidos}",
                    rotulo = if (estado.diasSeguidos == 1) "dia seguido" else "dias seguidos",
                    modifier = Modifier.weight(1f),
                )
                Numero(
                    // Sem resposta registrada, um traço — 0% diria que você errou tudo.
                    valor = estado.taxaDeAcerto?.let { "${(it * 100).toInt()}%" } ?: "—",
                    rotulo = "de acerto",
                    modifier = Modifier.weight(1f),
                )
            }

            estado.taxaDeAcerto?.let {
                val acertos = if (estado.acertos == 1) "1 acerto" else "${estado.acertos} acertos"
                val respostas =
                    if (estado.respondidas == 1) "1 resposta" else "${estado.respondidas} respostas"
                Text(
                    text = "$acertos em $respostas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            if (estado.pendentes > 0) {
                Text(
                    text = if (estado.pendentes == 1) {
                        "1 captura esperando transcrição."
                    } else {
                        "${estado.pendentes} capturas esperando transcrição."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // Espaço da barra, que flutua por cima desta coluna.
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

/**
 * O idioma nativo é o que troca de projeto.
 *
 * Fica no topo, antes do título, porque tudo abaixo dele — as contagens, as
 * fichas, a fila de revisão — pertence a esse idioma. Trocar aqui é mudar o que
 * a Início vai mostrar quando você voltar, então não é um ajuste escondido em
 * configurações: é o cabeçalho da tela.
 */
@Composable
private fun BarraDeIdiomaNativo(aoTrocar: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    val interacao = remember { MutableInteractionSource() }
    val nativo = Idiomas.nativoAtual

    Surface(
        onClick = aoTrocar,
        shape = MaterialTheme.shapes.large,
        color = cores.surface,
        interactionSource = interacao,
        modifier = Modifier
            .fillMaxWidth()
            .escalaAoPressionar(interacao),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .background(cores.secondaryContainer, CircleShape),
            ) {
                Text(text = nativo.bandeira, fontSize = 19.sp)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            ) {
                Text(
                    text = nativo.nome,
                    style = MaterialTheme.typography.titleSmall
                        .copy(fontWeight = FontWeight.SemiBold),
                    color = cores.onSurface,
                )
                Text(
                    text = "idioma base das suas fichas",
                    style = MaterialTheme.typography.bodySmall,
                    color = cores.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(
                imageVector = Icones.Expandir,
                contentDescription = "Trocar idioma base",
                tint = cores.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Só o português por enquanto — e a lista diz isso em vez de fingir escolha.
 *
 * Um seletor com um item só parece quebrado; a linha final explica que não está.
 */
@Composable
private fun SeletorDeIdiomaNativo(aoFechar: () -> Unit) {
    val cores = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Idioma base") },
        text = {
            Column {
                Text(
                    text = "É a língua em que as traduções e definições são escritas. " +
                        "Trocar abre outro conjunto de palavras.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cores.onSurfaceVariant,
                )
                Idiomas.nativos.forEach { idioma ->
                    val atual = idioma.codigo == Idiomas.nativoAtual.codigo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    ) {
                        Text(text = idioma.bandeira, fontSize = 20.sp)
                        Text(
                            text = idioma.nome,
                            style = MaterialTheme.typography.titleSmall,
                            color = cores.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                        )
                        if (atual) {
                            Icon(
                                imageVector = Icones.Check,
                                contentDescription = "Em uso",
                                tint = cores.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                Text(
                    text = "Outros idiomas base entram quando a geração de fichas " +
                        "passar a receber o par de idiomas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cores.onSurfaceVariant,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = aoFechar) { Text("Fechar") }
        },
    )
}

@Composable
private fun Numero(valor: String, rotulo: String, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = valor,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = rotulo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
