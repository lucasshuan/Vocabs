package com.jean.vocabs.ui.progresso

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.jean.vocabs.ui.components.RotuloDeSecao
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
    aoVoltar: () -> Unit,
    aoAbrirFicha: (Long) -> Unit,
    vm: ProgressoViewModel = viewModel(),
) {
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
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(dias, key = { it.dia }) { grupo ->
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                            RotuloDeSecao(grupo.titulo, Modifier.weight(1f))
                            grupo.quota?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        if (grupo.eventos.isEmpty()) {
                            Text(
                                text = "Dia parado. Nada aqui.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        } else {
                            grupo.eventos.forEach { evento ->
                                LinhaDoEvento(evento, aoClicar = { aoAbrirFicha(evento.entradaId) })
                            }
                        }
                    }
                }
                item { Spacer(Modifier.navigationBarsPadding().height(110.dp)) }
            }
        }
    }
}

@Composable
private fun LinhaDoEvento(evento: Evento, aoClicar: () -> Unit) {
    Surface(onClick = aoClicar, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        ) {
            Surface(shape = CircleShape, color = corDoEvento(evento.tipo), modifier = Modifier.size(8.dp)) {}
            Text(evento.alvo, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                text = descricaoDoEvento(evento),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun corDoEvento(tipo: TipoEvento): Color = when (tipo) {
    TipoEvento.SUBIU_NIVEL -> MaterialTheme.colorScheme.tertiary
    TipoEvento.ACERTO -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)
    TipoEvento.ERRO -> MaterialTheme.colorScheme.error
    TipoEvento.CAPTURADA, TipoEvento.FICHA_PRONTA -> MaterialTheme.colorScheme.primary
}

/** Um dia da linha do tempo, com o que aconteceu nele. */
internal data class DiaDeEventos(
    val dia: Long,
    val titulo: String,
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
