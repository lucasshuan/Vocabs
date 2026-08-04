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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.domain.Evento
import com.jean.vocabs.shared.domain.NivelMemoria
import com.jean.vocabs.shared.domain.TipoEvento
import com.jean.vocabs.ui.components.CabecalhoDeDentro
import com.jean.vocabs.ui.components.CartaoDaTela
import com.jean.vocabs.ui.components.DiaDaSemana
import com.jean.vocabs.ui.components.EstadoVazio
import com.jean.vocabs.ui.components.FaixaDaSemana
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.rotuloDoNivel
import java.time.LocalDate

/**
 * Tela 4b do handoff — "Dia a dia".
 *
 * A mesma faixa da semana do Progresso, e abaixo dela o que de fato aconteceu em
 * cada dia. Dia sem nada aparece assim mesmo, escrito: pular os dias vazios
 * deixaria a lista compacta e mentirosa, com terça encostada em sexta.
 */
@Composable
fun DiaADiaScreen(
    alvo: String?,
    aoVoltar: () -> Unit,
    aoAbrirFicha: (Long) -> Unit,
    vm: ProgressoViewModel = viewModel(),
) {
    LaunchedEffect(alvo) { vm.abrir(alvo) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val hoje = remember { LocalDate.now() }
    val dias = remember(estado.eventos, estado.quota) {
        agruparPorDia(
            eventos = estado.eventos,
            hoje = hoje,
            quotaDeHoje = "quota ${estado.quota.feita}/${estado.quota.total}",
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        CabecalhoDeDentro("Dia a dia", aoVoltar, Modifier.padding(top = 8.dp))

        CartaoDaTela(recheio = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${estado.mes} · esta semana",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = rotuloDeSequencia(estado.diasSeguidos),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
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
        }

        if (dias.isEmpty()) {
            EstadoVazio(
                icone = Icones.Relogio,
                titulo = "Nada aconteceu ainda",
                detalhe = "Capture uma palavra e ela aparece aqui.",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(dias, key = { it.dia }) { grupo ->
                    GrupoDoDia(
                        grupo = grupo,
                        ultimo = grupo.dia == dias.last().dia,
                        aoAbrirFicha = aoAbrirFicha,
                    )
                }
                item { Spacer(Modifier.navigationBarsPadding().height(110.dp)) }
            }
        }
    }
}

/**
 * Um dia da linha do tempo: o ponto no trilho e o que aconteceu ao lado dele.
 *
 * O trilho é o que faz esta tela ser uma linha do tempo e não uma lista com
 * subtítulos. Ele liga um dia ao seguinte inclusive por cima dos dias parados —
 * e é justamente aí que ele trabalha, porque a linha atravessando um "nada aqui"
 * é o que mostra o buraco em vez de escondê-lo.
 *
 * A linha é desenhada atrás da linha inteira, e não empilhada como um filho de
 * altura fixa: só no `drawBehind` se sabe até onde o conteúdo do dia foi, e é
 * assim que ela chega exatamente ao ponto do dia de baixo, com um cartão ou com
 * quatro. Fora isso, ela vive na fase de desenho e não remede ninguém.
 */
@Composable
private fun GrupoDoDia(grupo: DiaDeEventos, ultimo: Boolean, aoAbrirFicha: (Long) -> Unit) {
    val cores = MaterialTheme.colorScheme
    // Hoje é ameixa mesmo estando vazio: é o dia em que ainda dá para fazer
    // alguma coisa, e não mais um buraco no histórico.
    val ponto = when {
        grupo.hoje -> cores.primary
        grupo.eventos.isEmpty() -> cores.outline
        else -> cores.tertiary
    }
    val trilho = cores.outlineVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // O último dia não tem para onde continuar: a linha pararia no ar,
                // sugerindo um passado que a tela não tem como mostrar.
                if (ultimo) return@drawBehind
                val meio = LARGURA_DO_TRILHO.toPx() / 2f
                val comeco = (TOPO_DO_PONTO + TAMANHO_DO_PONTO + FOLGA_DO_TRILHO).toPx()
                drawLine(
                    color = trilho,
                    start = Offset(meio, comeco),
                    end = Offset(meio, size.height),
                    strokeWidth = 2.dp.toPx(),
                )
            },
    ) {
        Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.width(LARGURA_DO_TRILHO)) {
            Box(
                Modifier
                    .padding(top = TOPO_DO_PONTO)
                    .size(TAMANHO_DO_PONTO)
                    .background(ponto, CircleShape),
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f).padding(start = 11.dp, bottom = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                Text(grupo.titulo, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                grupo.quota?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = cores.primary)
                }
            }

            if (grupo.eventos.isEmpty()) {
                DiaParado()
            } else {
                grupo.eventos.forEach { evento ->
                    CartaoDoEvento(evento, aoClicar = { aoAbrirFicha(evento.entradaId) })
                }
            }
        }
    }
}

/**
 * O dia sem nada, como caixa e não como frase solta.
 *
 * Do lado de cartões brancos, um texto sem caixa se leria como legenda do dia de
 * cima. A caixa lilás ocupa o mesmo lugar de um cartão e diz que ali havia
 * espaço para alguma coisa — é o dia que está vazio, não a lista.
 */
@Composable
private fun DiaParado() {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Dia parado. Nada aqui.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
        )
    }
}

@Composable
private fun CartaoDoEvento(evento: Evento, aoClicar: () -> Unit) {
    CartaoDaTela(
        aoClicar = aoClicar,
        forma = MaterialTheme.shapes.medium,
        recheio = PaddingValues(horizontal = 15.dp, vertical = 13.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(evento.alvo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                text = descricaoDoEvento(evento),
                style = MaterialTheme.typography.bodySmall,
                color = corDoEvento(evento.tipo),
            )
        }
    }
}

/**
 * A cor do desfecho, agora no texto e não num ponto.
 *
 * O ponto colorido saiu de cada evento e virou o do dia, no trilho — dois níveis
 * de ponto na mesma coluna disputariam a leitura vertical que o trilho existe
 * para dar. Quem carrega o significado passou a ser a própria palavra à direita:
 * menta no que avançou, vermelho no que caiu, cinza no que só aconteceu.
 */
@Composable
private fun corDoEvento(tipo: TipoEvento): Color = when (tipo) {
    TipoEvento.SUBIU_NIVEL, TipoEvento.ACERTO -> MaterialTheme.colorScheme.tertiary
    TipoEvento.ERRO -> MaterialTheme.colorScheme.error
    TipoEvento.CAPTURADA, TipoEvento.FICHA_PRONTA -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** O ponto do dia e a linha que desce dele ficam nesta coluna, à esquerda de tudo. */
private val LARGURA_DO_TRILHO = 20.dp

/** Alinha o ponto com a primeira linha do título do dia, e não com o topo do bloco. */
private val TOPO_DO_PONTO = 5.dp
private val TAMANHO_DO_PONTO = 10.dp
private val FOLGA_DO_TRILHO = 4.dp

/** Um dia da linha do tempo, com o que aconteceu nele. */
internal data class DiaDeEventos(
    val dia: Long,
    val titulo: String,
    val hoje: Boolean,
    val quota: String?,
    val eventos: List<Evento>,
)

/**
 * Agrupa por dia e preenche os buracos.
 *
 * Do dia mais recente com evento até hoje, todo dia entra na lista, mesmo os
 * vazios — é o "Dia parado. Nada aqui." do handoff. O limite é o dia mais antigo
 * com evento: inventar meses vazios antes da primeira captura só encheria a
 * rolagem de nada.
 */
internal fun agruparPorDia(
    eventos: List<Evento>,
    hoje: LocalDate,
    quotaDeHoje: String? = null,
): List<DiaDeEventos> {
    if (eventos.isEmpty()) return emptyList()
    val porDia = eventos.groupBy { it.dia }
    val diaDeHoje = hoje.toEpochDay() + DIA_JULIANO_DA_EPOCA_UI
    val maisAntigo = porDia.keys.min()
    val maisRecente = maxOf(porDia.keys.max(), diaDeHoje)

    return (maisRecente downTo maisAntigo).map { dia ->
        DiaDeEventos(
            dia = dia,
            titulo = tituloDoDia(dia, diaDeHoje),
            hoje = dia == diaDeHoje,
            // A quota é uma conta sobre a fila de agora, e a fila de terça já
            // passou — só o dia de hoje pode dizer quanto falta.
            quota = quotaDeHoje.takeIf { dia == diaDeHoje },
            eventos = porDia[dia].orEmpty(),
        )
    }
}

private const val DIA_JULIANO_DA_EPOCA_UI = 2_440_588L

private fun tituloDoDia(dia: Long, hoje: Long): String {
    val data = LocalDate.ofEpochDay(dia - DIA_JULIANO_DA_EPOCA_UI)
    val nome = "${data.dayOfMonth} de ${nomeDoMes(data).lowercase()}"
    return when (dia) {
        hoje -> "$nome · hoje"
        hoje - 1 -> "$nome · ontem"
        else -> nome
    }
}

/**
 * O que a linha diz à direita: "capturada", "2ª revisão certa", "virou dominada".
 *
 * O ordinal vem do número da revisão guardado no evento — sem ele a linha diria
 * só "revisão certa" e a linha do tempo perderia justamente a noção de avanço
 * que ela existe para mostrar.
 */
internal fun descricaoDoEvento(evento: Evento): String = when (evento.tipo) {
    TipoEvento.CAPTURADA -> "capturada"
    TipoEvento.FICHA_PRONTA -> "ficha pronta"
    TipoEvento.ACERTO -> ordinalDaRevisao(evento.detalhe, certa = true)
    TipoEvento.ERRO -> ordinalDaRevisao(evento.detalhe, certa = false)
    TipoEvento.SUBIU_NIVEL -> "virou ${rotuloDoNivel(nivelDe(evento.detalhe))}"
}

private fun ordinalDaRevisao(detalhe: String?, certa: Boolean): String {
    val desfecho = if (certa) "certa" else "errada"
    val numero = detalhe?.toIntOrNull() ?: return "revisão $desfecho"
    return "${numero}ª revisão $desfecho"
}

private fun nivelDe(detalhe: String?): NivelMemoria =
    NivelMemoria.entries.firstOrNull { it.name == detalhe } ?: NivelMemoria.APRENDENDO
