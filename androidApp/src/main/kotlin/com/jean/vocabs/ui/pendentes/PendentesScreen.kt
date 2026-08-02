package com.jean.vocabs.ui.pendentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.domain.Captura
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.FormatoCaptura
import com.jean.vocabs.shared.domain.StatusCaptura
import com.jean.vocabs.shared.domain.StatusEntrada
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.tempoRelativo

@Composable
fun PendentesScreen(
    aoAbrirCaptura: (Captura) -> Unit,
    aoAbrirFicha: (Entrada) -> Unit,
    vm: PendentesViewModel = viewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val capturaMaisAntiga = estado.capturas.minOfOrNull(Captura::criadoEm)
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
    ) {
        item {
            Text("Pendentes", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 22.dp))
            Text(
                when {
                    estado.capturas.isNotEmpty() -> buildString {
                        append(estado.capturas.size)
                        append(if (estado.capturas.size == 1) " captura crua" else " capturas cruas")
                        capturaMaisAntiga?.let { append(" · a mais antiga ${tempoRelativo(it)}") }
                    }
                    estado.fichas.isNotEmpty() ->
                        "${estado.fichas.size} ${if (estado.fichas.size == 1) "ficha em processamento" else "fichas em processamento"}"
                    else -> "Suas capturas e fichas em processamento aparecem aqui."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
            )
        }

        if (estado.total == 0) {
            item { EstadoVazio() }
        }

        if (estado.capturas.isNotEmpty()) {
            items(estado.capturas, key = { "c${it.id}" }) { captura ->
                CartaoCaptura(captura, { aoAbrirCaptura(captura) })
            }
        }

        if (estado.fichas.isNotEmpty()) {
            item { Rotulo("FICHAS SENDO GERADAS") }
            items(estado.fichas, key = { "e${it.id}" }) { entrada ->
                CartaoEntrada(entrada, { aoAbrirFicha(entrada) }, { vm.tentarDeNovo(entrada.id) })
            }
        }
    }
}

@Composable
private fun Rotulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
}

@Composable
private fun CartaoCaptura(captura: Captura, aoClicar: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
        modifier = Modifier.fillMaxWidth().clickable(onClick = aoClicar),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (captura.formato == FormatoCaptura.FOTO) MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.secondaryContainer,
                        CircleShape,
                    ),
            ) {
                Icon(
                    if (captura.formato == FormatoCaptura.FOTO) Icones.Camera else Icones.Microfone,
                    if (captura.formato == FormatoCaptura.FOTO) "Foto" else "Áudio",
                    tint = if (captura.formato == FormatoCaptura.FOTO) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.primary,
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    tituloDaCaptura(captura),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    when {
                        captura.status == StatusCaptura.TRANSCREVENDO -> "Transcrevendo no aparelho…"
                        captura.erroTranscricao != null -> "Transcrever manualmente"
                        else -> "Selecionar termos"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                tempoRelativo(captura.criadoEm).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun tituloDaCaptura(captura: Captura): String = when (captura.formato) {
    FormatoCaptura.AUDIO -> captura.duracaoMs?.let { "Áudio · ${formatarDuracao(it)}" } ?: "Áudio"
    FormatoCaptura.FOTO -> "Foto"
    FormatoCaptura.TEXTO -> "Texto"
}

private fun formatarDuracao(duracaoMs: Long): String {
    val totalSegundos = (duracaoMs / 1_000).coerceAtLeast(0)
    val horas = totalSegundos / 3_600
    val minutos = (totalSegundos % 3_600) / 60
    val segundos = totalSegundos % 60
    val minutosComZero = minutos.toString().padStart(2, '0')
    val segundosComZero = segundos.toString().padStart(2, '0')
    return if (horas > 0) {
        "$horas:$minutosComZero:$segundosComZero"
    } else {
        "$minutos:$segundosComZero"
    }
}

@Composable
private fun CartaoEntrada(entrada: Entrada, aoClicar: () -> Unit, tentar: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Column(Modifier.weight(1f).clickable(onClick = aoClicar)) {
                Text(entrada.titulo, style = MaterialTheme.typography.titleLarge)
                Text(
                    when (entrada.status) {
                        StatusEntrada.PENDENTE -> "Na fila"
                        StatusEntrada.GERANDO -> "Gerando ficha…"
                        StatusEntrada.ERRO -> entrada.erro ?: "Falha na geração"
                        StatusEntrada.PRONTA -> "Ficha pronta"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (entrada.status == StatusEntrada.ERRO) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (entrada.status == StatusEntrada.GERANDO) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            if (entrada.status == StatusEntrada.ERRO) {
                Surface(onClick = tentar, shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text("Tentar", modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

@Composable
private fun EstadoVazio() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 70.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape)) {
            Icon(Icones.Check, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(30.dp))
        }
        Text("Tudo em dia", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
        Text("Nenhuma captura ou ficha esperando.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
