package com.jean.vocabs.ui.progresso

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.jean.vocabs.ui.components.AnelDeProgresso
import com.jean.vocabs.ui.components.BarraDeFaixas
import com.jean.vocabs.ui.components.CabecalhoDeDentro
import com.jean.vocabs.ui.components.CartaoDaTela
import com.jean.vocabs.ui.components.CartaoMetrica
import com.jean.vocabs.ui.components.DiaDaSemana
import com.jean.vocabs.ui.components.FaixaDaSemana
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.LinhaDeUsoDeIa

/**
 * Tela 4a do handoff — "Seu progresso".
 *
 * Página de dentro, aberta pelo cartão da tela Você. Três blocos: a semana com a
 * quota, o estoque de palavras e os números de apoio. Cada um dos dois primeiros
 * é a porta de uma tela mais funda.
 */
@Composable
fun ProgressoScreen(
    aoVoltar: () -> Unit,
    aoAbrirDiaADia: () -> Unit,
    aoAbrirOQueFalta: () -> Unit,
    vm: ProgressoViewModel = viewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val cores = MaterialTheme.colorScheme

    Column(
        verticalArrangement = Arrangement.spacedBy(13.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        CabecalhoDeDentro("Seu progresso", aoVoltar, Modifier.padding(top = 8.dp))

        CartaoDaTela(aoClicar = aoAbrirDiaADia, recheio = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${estado.mes} · esta semana",
                        style = MaterialTheme.typography.labelMedium,
                        color = cores.onSurfaceVariant,
                    )
                    Icon(
                        imageVector = Icones.Avancar,
                        contentDescription = null,
                        tint = cores.outline,
                        modifier = Modifier.size(16.dp).padding(start = 2.dp),
                    )
                }
                Text(
                    text = rotuloDeSequencia(estado.diasSeguidos),
                    style = MaterialTheme.typography.bodySmall,
                    color = cores.tertiary,
                )
            }

            FaixaDaSemana(
                dias = estado.semana.mapIndexed { indice, dia ->
                    DiaDaSemana(
                        sigla = SIGLAS_DA_SEMANA[indice],
                        numero = dia.data.dayOfMonth,
                        revisoes = dia.revisoes,
                        hoje = dia.hoje,
                        futuro = dia.futuro,
                    )
                },
                modifier = Modifier.padding(top = 13.dp),
            )

            Box(Modifier.fillMaxWidth().padding(vertical = 13.dp).height(1.dp).background(cores.outlineVariant))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                    Text("Quota de hoje", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text(
                        text = "${estado.quota.feita} de ${estado.quota.total} ${if (estado.quota.total == 1) "revisão" else "revisões"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = cores.onSurfaceVariant,
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(cores.outlineVariant, RoundedCornerShape(4.dp)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(estado.quota.fracao)
                            .height(8.dp)
                            .background(cores.tertiary, RoundedCornerShape(4.dp)),
                    )
                }
                Text(
                    text = textoDaQuota(estado.quota.batida, estado.quota.naFila, estado.total),
                    style = MaterialTheme.typography.bodySmall,
                    color = cores.onSurfaceVariant,
                )
            }
        }

        CartaoDaTela(aoClicar = aoAbrirOQueFalta, recheio = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                AnelDeProgresso(
                    fracao = if (estado.total == 0) 0f else estado.dominadas.toFloat() / estado.total,
                    tamanho = 78.dp,
                    espessura = 9.dp,
                ) {
                    Text("${estado.dominadas}", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "de ${estado.total}",
                        style = MaterialTheme.typography.labelSmall,
                        color = cores.onSurfaceVariant,
                    )
                }
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(tituloDoEstoque(estado.dominadas), style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = textoDoQuePertoDeVirar(estado.pertoDeVirar.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = cores.onSurfaceVariant,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
                Icon(Icones.Avancar, null, tint = cores.outline, modifier = Modifier.size(20.dp))
            }

            BarraDeFaixas(
                faixas = listOf(
                    estado.dominadas to cores.tertiary,
                    estado.familiares to cores.tertiary.copy(alpha = 0.55f),
                    estado.aprendendo to cores.outlineVariant,
                ),
                modifier = Modifier.padding(top = 14.dp),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) {
                LegendaDaFaixa("${estado.dominadas} dominadas", cores.tertiary)
                LegendaDaFaixa("${estado.familiares} familiares", cores.tertiary.copy(alpha = 0.55f))
                LegendaDaFaixa("${estado.aprendendo} aprendendo", cores.outlineVariant)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            CartaoMetrica(
                valor = estado.taxaDeAcerto?.let { "${(it * 100).toInt()}%" } ?: "—",
                rotulo = "de acerto",
                modifier = Modifier.weight(1f),
            )
            CartaoMetrica(
                valor = estado.palavrasPorDia?.let { formatarComVirgula(it) } ?: "—",
                rotulo = "palavras/dia",
                modifier = Modifier.weight(1f),
            )
            CartaoMetrica(
                valor = "${estado.melhorSequencia}",
                rotulo = "melhor sequência",
                modifier = Modifier.weight(1f),
            )
        }

        LinhaDeUsoDeIa(usadas = estado.usoIa.usadas, limite = estado.usoIa.limite)

        Spacer(Modifier.navigationBarsPadding().height(110.dp))
    }
}

@Composable
private fun LegendaDaFaixa(texto: String, cor: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Surface(shape = RoundedCornerShape(3.dp), color = cor, modifier = Modifier.size(8.dp)) {}
        Text(
            text = texto,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun rotuloDeSequencia(dias: Int): String =
    if (dias == 1) "1 dia seguido" else "$dias dias seguidos"

/**
 * O texto sob a barra da quota.
 *
 * O caso de estoque vazio é separado do de quota batida: "o resto de hoje é
 * bônus" para quem ainda não tem ficha nenhuma seria uma comemoração de nada.
 */
internal fun textoDaQuota(batida: Boolean, naFila: Int, totalDePalavras: Int): String = when {
    totalDePalavras == 0 -> "Nenhuma palavra ainda. Capture a primeira."
    batida -> "Quota batida. O resto de hoje é bônus."
    naFila == 1 -> "Falta 1 para bater a quota de hoje."
    else -> "Faltam $naFila para bater a quota de hoje."
}

private val NUMEROS_POR_EXTENSO = listOf(
    "Nenhuma", "Uma", "Duas", "Três", "Quatro", "Cinco", "Seis", "Sete", "Oito", "Nove", "Dez",
)

/**
 * "Nove palavras já são suas".
 *
 * Por extenso até dez porque é assim que se lê em voz alta uma conquista; a
 * partir daí o algarismo volta, que é como se lê um número grande.
 */
internal fun tituloDoEstoque(dominadas: Int): String = when {
    dominadas == 0 -> "Nenhuma palavra é sua ainda"
    dominadas == 1 -> "Uma palavra já é sua"
    dominadas <= 10 -> "${NUMEROS_POR_EXTENSO[dominadas]} palavras já são suas"
    else -> "$dominadas palavras já são suas"
}

internal fun textoDoQuePertoDeVirar(quantas: Int): String = when (quantas) {
    0 -> "Nenhuma está perto de virar."
    1 -> "1 está perto de virar."
    else -> "$quantas estão perto de virar."
}

/** Uma casa decimal, com vírgula — "4,2". */
internal fun formatarComVirgula(valor: Double): String {
    val arredondado = (valor * 10).toLong()
    return "${arredondado / 10},${arredondado % 10}"
}
