package com.jean.vocabs.ui.perfil

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.jean.vocabs.ui.components.CartaoDaTela
import com.jean.vocabs.ui.components.CartaoMetrica
import com.jean.vocabs.ui.components.ChevronDeLinha
import com.jean.vocabs.ui.components.LinhaDeLista
import com.jean.vocabs.ui.components.ParDeIdiomas
import com.jean.vocabs.ui.components.RotuloDeSecao
import java.io.File
import java.time.LocalDate

@Composable
fun PerfilScreen(vm: PerfilViewModel = viewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    Column(
        verticalArrangement = Arrangement.spacedBy(15.dp),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp),
    ) {
        Text("Seu progresso", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 22.dp))

        CartaoDaTela(recheio = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                RotuloDeSecao("Últimas 12 semanas", Modifier.weight(1f))
                Text(
                    text = "${estado.diasSeguidos} ${if (estado.diasSeguidos == 1) "dia seguido" else "dias seguidos"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            Heatmap(estado.atividade, Modifier.padding(top = 12.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            CartaoMetrica("${estado.totalPalavras}", "palavras", Modifier.weight(1f))
            CartaoMetrica("${estado.dominadas}", "dominadas", Modifier.weight(1f), destaque = true)
            CartaoMetrica(estado.taxaDeAcerto?.let { "${(it * 100).toInt()}%" } ?: "—", "de acerto", Modifier.weight(1f))
        }

        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            LinhaDeLista(fim = { ChevronDeLinha() }) {
                ParDeIdiomas(comNomes = true, tamanhoDaBandeira = 18.dp)
                Text(
                    "par de idiomas das fichas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            LinhaDeLista(
                titulo = "Gerações por IA",
                detalhe = "${estado.usoIa.usadas} de ${estado.usoIa.limite} este mês",
                fim = {
                    Box(
                        Modifier
                            .width(52.dp)
                            .height(6.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(estado.usoIa.fracao)
                                .height(6.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)),
                        )
                    }
                },
            )

            LinhaDeLista(
                titulo = "Exportar meus dados",
                detalhe = if (estado.exportando) "preparando ZIP…" else null,
                aoClicar = {
                    vm.exportar(
                        aoPronto = { arquivo -> compartilhar(contexto, arquivo) },
                        aoErro = { Toast.makeText(contexto, it, Toast.LENGTH_LONG).show() },
                    )
                },
                fim = {
                    if (estado.exportando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else ChevronDeLinha()
                },
            )
        }
        Spacer(Modifier.navigationBarsPadding().height(110.dp))
    }
}

/**
 * 84 dias em colunas de 12 — as "12 semanas" do handoff.
 *
 * Menta com opacidade crescente: o dia sem revisão fica no cinza-lilás de apoio,
 * e a intensidade é a única coisa que a cor comunica.
 */
@Composable
private fun Heatmap(atividade: List<com.jean.vocabs.shared.domain.AtividadeDiaria>, modifier: Modifier = Modifier) {
    val hoje = LocalDate.now().toEpochDay() + 2_440_588L
    val mapa = atividade.associate { it.dia to it.revisoes }
    FlowRow(
        maxItemsInEachRow = 12,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        (83L downTo 0L).forEach { distancia ->
            val quantidade = mapa[hoje - distancia] ?: 0
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(corAtividade(quantidade), RoundedCornerShape(3.dp)),
            )
        }
    }
}

@Composable
private fun corAtividade(quantidade: Int): Color = when (quantidade) {
    0 -> MaterialTheme.colorScheme.outlineVariant
    1 -> MaterialTheme.colorScheme.tertiary.copy(alpha = .28f)
    2 -> MaterialTheme.colorScheme.tertiary.copy(alpha = .5f)
    3 -> MaterialTheme.colorScheme.tertiary.copy(alpha = .75f)
    else -> MaterialTheme.colorScheme.tertiary
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
