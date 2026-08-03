package com.jean.vocabs.ui.inicio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.R
import com.jean.vocabs.ui.components.BotaoPrincipal
import com.jean.vocabs.ui.components.CartaoDaTela
import com.jean.vocabs.ui.components.CartaoMetrica
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.LinhaDeLista
import com.jean.vocabs.ui.components.PilulaDeIdiomas
import com.jean.vocabs.ui.components.RotuloDeSecao
import java.time.LocalTime

@Composable
fun InicioScreen(
    aoEscrever: () -> Unit,
    aoAbrirPalavras: () -> Unit,
    aoAbrirPendentes: () -> Unit,
    aoRevisar: () -> Unit,
    aoAbrirPerfil: () -> Unit,
    vm: InicioViewModel = viewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val naFila = estado.revisao?.naFila ?: 0
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Image(painterResource(R.drawable.logo_tagarara), stringResource(R.string.logo_description), Modifier.size(34.dp))
            Text("TAGARARA", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 9.dp))
            Spacer(Modifier.weight(1f))
            PilulaDeIdiomas(aoAbrirPerfil)
        }

        CartaoDaTela(
            forma = MaterialTheme.shapes.extraLarge,
            recheio = PaddingValues(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnelMemoria(estado.forcaMedia)
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(saudacao(), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = if (naFila == 0) "Sua memória está em dia.\nNada esfriou hoje."
                        else "$naFila ${if (naFila == 1) "palavra esfriou" else "palavras esfriaram"}.\n${minutosDeRevisao(naFila)} resolvem.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
            }
            BotaoPrincipal(
                texto = if (naFila > 0) "Revisar $naFila ${if (naFila == 1) "palavra" else "palavras"}" else "Nada para revisar",
                aoClicar = aoRevisar,
                habilitado = naFila > 0,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        estado.capturaMaisAntiga?.let {
            val total = estado.capturasPendentes
            LinhaDeLista(
                titulo = "$total ${if (total == 1) "captura esperando você" else "capturas esperando você"}",
                detalhe = "continue a transcrição",
                aoClicar = aoAbrirPendentes,
                fim = {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(30.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape),
                    ) {
                        Icon(
                            imageVector = Icones.Avancar,
                            contentDescription = "Abrir pendentes",
                            tint = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
            )
        }

        val dias = estado.revisao?.diasSeguidos ?: 0
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            CartaoMetrica(
                valor = "${estado.totalPalavras}",
                rotulo = if (estado.totalPalavras == 1) "vocabulário" else "vocabulários",
                modifier = Modifier.weight(1f),
                aoClicar = aoAbrirPalavras,
            )
            CartaoMetrica(
                valor = "${estado.dominadas}",
                rotulo = if (estado.dominadas == 1) "dominada" else "dominadas",
                modifier = Modifier.weight(1f),
                destaque = true,
            )
            CartaoMetrica(
                valor = "$dias",
                rotulo = if (dias == 1) "dia seguido" else "dias seguidos",
                modifier = Modifier.weight(1f),
                destaque = true,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            RotuloDeSecao("Capturadas hoje")
            if (estado.recentesHoje.isEmpty()) {
                CartaoDaTela(forma = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(), aoClicar = aoEscrever) {
                    Text("Ainda nenhuma ficha hoje", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Capture algo que encontrou no mundo real.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            } else {
                estado.recentesHoje.forEach { entrada ->
                    CartaoDaTela(
                        forma = MaterialTheme.shapes.small,
                        recheio = PaddingValues(horizontal = 15.dp, vertical = 12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(entrada.titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text(
                                entrada.ficha?.traducao.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(110.dp))
    }
}

/** "Bom dia" até as 12, "Boa tarde" até as 18, "Boa noite" depois. */
private fun saudacao(agora: LocalTime = LocalTime.now()): String = when (agora.hour) {
    in 5..11 -> "Bom dia"
    in 12..17 -> "Boa tarde"
    else -> "Boa noite"
}

/**
 * O custo da fila em minutos, arredondado para cima.
 *
 * Meio minuto por cartão é o que o handoff assume ("3 palavras … 10 min
 * resolvem" seria generoso demais para 3); manter a conta explícita evita
 * prometer um número que a sessão não cumpre.
 */
private fun minutosDeRevisao(naFila: Int): String {
    val minutos = ((naFila * 30 + 59) / 60).coerceAtLeast(1)
    return "$minutos min"
}

@Composable
private fun AnelMemoria(valor: Int) {
    val trilha = MaterialTheme.colorScheme.outlineVariant
    val cor = MaterialTheme.colorScheme.tertiary
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(74.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val traco = Stroke(8.dp.toPx(), cap = StrokeCap.Round)
            drawArc(trilha, -90f, 360f, false, style = traco)
            drawArc(cor, -90f, 360f * valor.coerceIn(0, 100) / 100f, false, style = traco)
        }
        Text("$valor%", style = MaterialTheme.typography.headlineSmall)
    }
}
