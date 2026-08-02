package com.jean.vocabs.ui.inbox

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.FormatoCaptura
import com.jean.vocabs.shared.domain.StatusEntrada
import com.jean.vocabs.ui.components.AnelProgresso
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.PontoStatus
import com.jean.vocabs.ui.components.escalaAoPressionar
import com.jean.vocabs.ui.components.tempoRelativo

@Composable
fun InboxScreen(
    aoAbrirEntrada: (Entrada) -> Unit,
    vm: InboxViewModel = viewModel(),
) {
    val entradas by vm.entradas.collectAsStateWithLifecycle()

    if (entradas.isEmpty()) {
        InboxVazio()
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Cabecalho(entradas = entradas)
        }

        items(entradas, key = { it.id }) { entrada ->
            ItemInbox(
                entrada = entrada,
                aoClicar = { aoAbrirEntrada(entrada) },
                modifier = Modifier.animateItem(
                    fadeOutSpec = tween(200),
                    placementSpec = tween(300),
                ),
            )
        }
    }
}

@Composable
private fun Cabecalho(entradas: List<Entrada>) {
    val aTranscrever = entradas.count { it.precisaTranscricao }
    val maisAntiga = entradas.minByOrNull { it.criadoEm }

    Column(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)) {
        Text(
            text = "Inbox",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        val resumo = buildString {
            append(if (entradas.size == 1) "1 captura crua" else "${entradas.size} capturas cruas")
            if (aTranscrever > 0) append("  ·  $aTranscrever esperando transcrição")
        }
        Text(
            text = resumo,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        // O sinal que o documento de métricas chama de mais importante: se o
        // item mais velho não anda, você captura mais rápido do que processa.
        maisAntiga?.let {
            Text(
                text = "A mais antiga está parada ${tempoRelativo(it.criadoEm)}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun ItemInbox(
    entrada: Entrada,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    val interacao = remember { MutableInteractionSource() }

    val legenda = when (entrada.status) {
        StatusEntrada.RASCUNHO -> when (entrada.formato) {
            FormatoCaptura.FOTO -> "Toque para transcrever a foto"
            FormatoCaptura.AUDIO -> "Toque para ouvir e transcrever"
            FormatoCaptura.TEXTO -> "Toque para completar"
        }
        StatusEntrada.PENDENTE -> "na fila"
        StatusEntrada.GERANDO -> "escrevendo ficha…"
        StatusEntrada.ERRO -> entrada.erro ?: "falhou — toque para tentar de novo"
        StatusEntrada.PRONTA -> entrada.trecho.orEmpty()
    }

    Surface(
        onClick = aoClicar,
        shape = MaterialTheme.shapes.medium,
        color = cores.surface,
        shadowElevation = 1.dp,
        interactionSource = interacao,
        modifier = modifier
            .fillMaxWidth()
            .escalaAoPressionar(interacao),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            SeloDeFormato(entrada = entrada)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
            ) {
                Text(
                    text = entrada.titulo,
                    style = MaterialTheme.typography.titleSmall,
                    color = cores.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = legenda,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (entrada.status) {
                        StatusEntrada.ERRO -> cores.error
                        StatusEntrada.GERANDO -> cores.primary
                        else -> cores.onSurfaceVariant
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            Text(
                text = tempoRelativo(entrada.criadoEm),
                style = MaterialTheme.typography.labelMedium,
                color = cores.onSurfaceVariant,
            )
        }
    }
}

/** Círculo com o ícone do formato; vira anel girando enquanto a ficha é escrita. */
@Composable
private fun SeloDeFormato(entrada: Entrada) {
    val cores = MaterialTheme.colorScheme

    if (entrada.status == StatusEntrada.GERANDO) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp),
        ) {
            AnelProgresso(girando = true, cor = cores.primary, tamanho = 26.dp)
        }
        return
    }

    val (icone, corIcone, corFundo) = when (entrada.formato) {
        FormatoCaptura.FOTO -> Triple(Icones.Camera, cores.onTertiaryContainer, cores.tertiaryContainer)
        FormatoCaptura.AUDIO -> Triple(Icones.Microfone, cores.onPrimaryContainer, cores.primaryContainer)
        FormatoCaptura.TEXTO -> Triple(Icones.Cartas, cores.onSecondaryContainer, cores.secondaryContainer)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .background(if (entrada.status == StatusEntrada.ERRO) cores.errorContainer else corFundo, CircleShape),
    ) {
        Icon(
            imageVector = icone,
            contentDescription = null,
            tint = if (entrada.status == StatusEntrada.ERRO) cores.onErrorContainer else corIcone,
            modifier = Modifier.size(19.dp),
        )
        if (entrada.status == StatusEntrada.PENDENTE) {
            PontoStatus(
                cor = cores.primary,
                pulsando = false,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
private fun InboxVazio() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(76.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icones.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        Text(
            text = "Inbox limpo",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = "Nada parado esperando você. Tudo que foi capturado já virou ficha.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
