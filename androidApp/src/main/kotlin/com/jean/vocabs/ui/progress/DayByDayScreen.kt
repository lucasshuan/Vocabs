package com.jean.vocabs.ui.progress

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
import com.jean.vocabs.shared.domain.Event
import com.jean.vocabs.shared.domain.EventType
import com.jean.vocabs.shared.domain.MemoryLevel
import com.jean.vocabs.ui.components.AppIcons
import com.jean.vocabs.ui.components.EmptyState
import com.jean.vocabs.ui.components.InnerHeader
import com.jean.vocabs.ui.components.ScreenCard
import com.jean.vocabs.ui.components.WeekDay
import com.jean.vocabs.ui.components.WeekStrip
import com.jean.vocabs.ui.components.levelLabel
import java.time.LocalDate

/**
 * Tela 4b do handoff — "Dia a dia".
 *
 * A mesma faixa da semana do Progresso, e abaixo dela o que de fato aconteceu em
 * cada dia. Dia sem nada aparece assim mesmo, escrito: pular os dias vazios
 * deixaria a lista compacta e mentirosa, com terça encostada em sexta.
 */
@Composable
fun DayByDayScreen(
    target: String?,
    onBack: () -> Unit,
    onOpenCard: (Long) -> Unit,
    vm: ProgressViewModel = viewModel(),
) {
    LaunchedEffect(target) { vm.open(target) }
    val state by vm.state.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    val days = remember(state.events, state.quota) {
        groupByDay(
            events = state.events,
            today = today,
            todayQuota = "quota ${state.quota.done}/${state.quota.total}",
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        InnerHeader("Dia a dia", onBack, Modifier.padding(top = 8.dp))

        ScreenCard(filling = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${state.month} · esta semana",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = streakLabel(state.dayStreak),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            WeekStrip(
                days = state.semana.mapIndexed { index, day ->
                    WeekDay(
                        abbreviation = WEEKDAY_LABELS[index],
                        number = day.data.dayOfMonth,
                        reviews = day.reviews,
                        today = day.today,
                        future = day.future,
                    )
                },
                modifier = Modifier.padding(top = 13.dp),
            )
        }

        if (days.isEmpty()) {
            EmptyState(
                icon = AppIcons.Clock,
                title = "Nada aconteceu ainda",
                detail = "Capture uma palavra e ela aparece aqui.",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(days, key = { it.day }) { group ->
                    DayGroup(
                        group = group,
                        last = group.day == days.last().day,
                        onOpenCard = onOpenCard,
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
private fun DayGroup(group: EventDay, last: Boolean, onOpenCard: (Long) -> Unit) {
    val colors = MaterialTheme.colorScheme
    // Hoje é ameixa mesmo estando vazio: é o dia em que ainda dá para fazer
    // alguma coisa, e não mais um buraco no histórico.
    val point = when {
        group.today -> colors.primary
        group.events.isEmpty() -> colors.outline
        else -> colors.tertiary
    }
    val rail = colors.outlineVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // O último dia não tem para onde continuar: a linha pararia no ar,
                // sugerindo um passado que a tela não tem como mostrar.
                if (last) return@drawBehind
                val meio = TRACK_WIDTH.toPx() / 2f
                val beginning = (DOT_TOP + DOT_SIZE + TRACK_GAP).toPx()
                drawLine(
                    color = rail,
                    start = Offset(meio, beginning),
                    end = Offset(meio, size.height),
                    strokeWidth = 2.dp.toPx(),
                )
            },
    ) {
        Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.width(TRACK_WIDTH)) {
            Box(
                Modifier
                    .padding(top = DOT_TOP)
                    .size(DOT_SIZE)
                    .background(point, CircleShape),
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f).padding(start = 11.dp, bottom = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                Text(group.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                group.quota?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = colors.primary)
                }
            }

            if (group.events.isEmpty()) {
                IdleDay()
            } else {
                group.events.forEach { event ->
                    EventCard(event, onClick = { onOpenCard(event.entryId) })
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
private fun IdleDay() {
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
private fun EventCard(event: Event, onClick: () -> Unit) {
    ScreenCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        filling = PaddingValues(horizontal = 15.dp, vertical = 13.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(event.target, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                text = eventDescription(event),
                style = MaterialTheme.typography.bodySmall,
                color = eventColor(event.type),
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
private fun eventColor(type: EventType): Color = when (type) {
    EventType.LEVELED_UP, EventType.CORRECT -> MaterialTheme.colorScheme.tertiary
    EventType.INCORRECT -> MaterialTheme.colorScheme.error
    EventType.CAPTURED, EventType.CARD_READY -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** O ponto do dia e a linha que desce dele ficam nesta coluna, à esquerda de tudo. */
private val TRACK_WIDTH = 20.dp

/** Alinha o ponto com a primeira linha do título do dia, e não com o topo do bloco. */
private val DOT_TOP = 5.dp
private val DOT_SIZE = 10.dp
private val TRACK_GAP = 4.dp

/** Um dia da linha do tempo, com o que aconteceu nele. */
internal data class EventDay(
    val day: Long,
    val title: String,
    val today: Boolean,
    val quota: String?,
    val events: List<Event>,
)

/**
 * Agrupa por dia e preenche os buracos.
 *
 * Do dia mais recente com evento até hoje, todo dia entra na lista, mesmo os
 * vazios — é o "Dia parado. Nada aqui." do handoff. O limite é o dia mais antigo
 * com evento: inventar meses vazios antes da primeira captura só encheria a
 * rolagem de nada.
 */
internal fun groupByDay(
    events: List<Event>,
    today: LocalDate,
    todayQuota: String? = null,
): List<EventDay> {
    if (events.isEmpty()) return emptyList()
    val byDay = events.groupBy { it.day }
    val todayDay = today.toEpochDay() + JULIAN_DAY_OF_EPOCH_UI
    val earliest = byDay.keys.min()
    val latest = maxOf(byDay.keys.max(), todayDay)

    return (latest downTo earliest).map { day ->
        EventDay(
            day = day,
            title = dayTitle(day, todayDay),
            today = day == todayDay,
            // A quota é uma conta sobre a fila de agora, e a fila de terça já
            // passou — só o dia de hoje pode dizer quanto falta.
            quota = todayQuota.takeIf { day == todayDay },
            events = byDay[day].orEmpty(),
        )
    }
}

private const val JULIAN_DAY_OF_EPOCH_UI = 2_440_588L

private fun dayTitle(day: Long, today: Long): String {
    val data = LocalDate.ofEpochDay(day - JULIAN_DAY_OF_EPOCH_UI)
    val name = "${data.dayOfMonth} de ${monthName(data).lowercase()}"
    return when (day) {
        today -> "$name · hoje"
        today - 1 -> "$name · ontem"
        else -> name
    }
}

/**
 * O que a linha diz à direita: "capturada", "2ª revisão certa", "virou dominada".
 *
 * O ordinal vem do número da revisão guardado no evento — sem ele a linha diria
 * só "revisão certa" e a linha do tempo perderia justamente a noção de avanço
 * que ela existe para mostrar.
 */
internal fun eventDescription(event: Event): String = when (event.type) {
    EventType.CAPTURED -> "capturada"
    EventType.CARD_READY -> "ficha pronta"
    EventType.CORRECT -> reviewOrdinal(event.detail, right = true)
    EventType.INCORRECT -> reviewOrdinal(event.detail, right = false)
    EventType.LEVELED_UP -> "virou ${levelLabel(levelOf(event.detail))}"
}

private fun reviewOrdinal(detail: String?, right: Boolean): String {
    val outcome = if (right) "certa" else "errada"
    val number = detail?.toIntOrNull() ?: return "revisão $outcome"
    return "${number}ª revisão $outcome"
}

private fun levelOf(detail: String?): MemoryLevel =
    MemoryLevel.entries.firstOrNull { it.name == detail } ?: MemoryLevel.LEARNING
