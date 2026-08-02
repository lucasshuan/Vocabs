package com.jean.vocabs.ui.perfil

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.time.LocalDate

@Composable
fun PerfilScreen(vm: PerfilViewModel = viewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp),
    ) {
        Text("Seu progresso", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 22.dp))
        Text("O que você pratica ganha cor.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))

        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("Últimos 84 dias", style = MaterialTheme.typography.titleLarge)
                Heatmap(estado.atividade, Modifier.padding(top = 14.dp))
                Text("Quanto mais revisões, mais intensa a menta.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Metrica("${estado.totalPalavras}", "vocabulário", Modifier.weight(1f))
            Metrica("${estado.dominadas}", "dominadas", Modifier.weight(1f))
            Metrica(estado.taxaDeAcerto?.let { "${(it * 100).toInt()}%" } ?: "—", "acertos", Modifier.weight(1f))
        }

        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Column(Modifier.padding(17.dp)) {
                Text("Idiomas", style = MaterialTheme.typography.titleMedium)
                Text("🇧🇷  Português   →   🇺🇸  Inglês", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 7.dp))
                Text("Este é o único par disponível nesta versão.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Column(Modifier.padding(17.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Uso mensal de IA", style = MaterialTheme.typography.titleMedium)
                    Text("${estado.usoIa.usadas}/${estado.usoIa.limite}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                LinearProgressIndicator(
                    progress = { estado.usoIa.fracao },
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(8.dp),
                )
                Text("Indicador local e informativo; não bloqueia novas fichas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
        }

        Button(
            onClick = {
                vm.exportar(
                    aoPronto = { arquivo -> compartilhar(contexto, arquivo) },
                    aoErro = { Toast.makeText(contexto, it, Toast.LENGTH_LONG).show() },
                )
            },
            enabled = !estado.exportando,
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp).height(56.dp),
        ) {
            if (estado.exportando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            Text(if (estado.exportando) "Preparando ZIP…" else "Exportar meus dados", modifier = Modifier.padding(start = if (estado.exportando) 10.dp else 0.dp))
        }
        Spacer(Modifier.navigationBarsPadding().height(120.dp))
    }
}

@Composable
private fun Heatmap(atividade: List<com.jean.vocabs.shared.domain.AtividadeDiaria>, modifier: Modifier = Modifier) {
    val hoje = LocalDate.now().toEpochDay() + 2_440_588L
    val mapa = atividade.associate { it.dia to it.revisoes }
    FlowRow(
        maxItemsInEachRow = 12,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        (83L downTo 0L).forEach { distancia ->
            val quantidade = mapa[hoje - distancia] ?: 0
            Box(
                Modifier
                    .size(15.dp)
                    .background(corAtividade(quantidade), RoundedCornerShape(4.dp)),
            )
        }
    }
}

@Composable
private fun corAtividade(quantidade: Int): Color = when (quantidade) {
    0 -> MaterialTheme.colorScheme.surfaceVariant
    1 -> MaterialTheme.colorScheme.tertiary.copy(alpha = .28f)
    2 -> MaterialTheme.colorScheme.tertiary.copy(alpha = .5f)
    3 -> MaterialTheme.colorScheme.tertiary.copy(alpha = .75f)
    else -> MaterialTheme.colorScheme.tertiary
}

@Composable
private fun Metrica(valor: String, rotulo: String, modifier: Modifier) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 15.dp, horizontal = 2.dp)) {
            Text(valor, style = MaterialTheme.typography.headlineSmall, color = if (rotulo == "dominadas") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface)
            Text(rotulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun compartilhar(contexto: android.content.Context, arquivo: File) {
    val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", arquivo)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    contexto.startActivity(Intent.createChooser(intent, "Exportar dados da Tagarara"))
}
