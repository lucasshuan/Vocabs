package com.jean.vocabs.ui.progress

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.domain.CourseSummary
import com.jean.vocabs.shared.domain.DailyQuota
import com.jean.vocabs.shared.domain.Steps
import com.jean.vocabs.ui.components.AppIcons
import com.jean.vocabs.ui.components.BandBars
import com.jean.vocabs.ui.components.CircularFlag
import com.jean.vocabs.ui.components.DashedBox
import com.jean.vocabs.ui.components.InnerHeader
import com.jean.vocabs.ui.components.Motion
import com.jean.vocabs.ui.components.ProgressRing
import com.jean.vocabs.ui.components.ScreenCard
import com.jean.vocabs.ui.components.SecondaryAction
import com.jean.vocabs.ui.components.WeekDay
import com.jean.vocabs.ui.components.WeekStrip
import com.jean.vocabs.ui.components.animatedCount
import com.jean.vocabs.ui.components.animatedFraction
import com.jean.vocabs.ui.components.cardOutline
import com.jean.vocabs.ui.components.dashedOutline
import com.jean.vocabs.ui.components.shrinkOnTouch
import com.jean.vocabs.ui.components.rememberHaptics
import com.jean.vocabs.ui.displayName
import com.jean.vocabs.ui.languages.languageOf
import kotlinx.coroutines.launch

/**
 * Tela 08 do handoff — "Seu progresso", de um curso.
 *
 * Dois blocos, sempre os mesmos: a semana com a quota do dia e o estoque de
 * palavras. Cada um é a porta de uma tela mais funda, e os dois chevrons são as
 * únicas saídas. A tela não tem porcentagem de acerto, palavras por dia nem
 * melhor sequência — os dias marcados na semana são o único registro de
 * frequência que ela guarda.
 *
 * Sem palavra nenhuma no idioma a estrutura não muda: os mesmos dois cartões
 * ficam tracejados, com os rótulos no lugar e nenhum número inventado. Um
 * esqueleto mostra onde as coisas vão ficar; um "0 de 10" diria que já se
 * falhou em alguma coisa.
 *
 * A pastilha da bandeira é o único indicador de curso e também o botão de troca:
 * um toque abre a gaveta, escolher fecha e recarrega os dois cartões. Trocar
 * aqui **não** troca o curso aberto do app — quem só quis olhar o francês não
 * deve encontrar o `+` e a revisão mudados de idioma depois.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    target: String?,
    onBack: () -> Unit,
    onOpenDayByDay: (String) -> Unit,
    onOpenWhatsLeft: (String) -> Unit,
    onAddLanguage: () -> Unit,
    vm: ProgressViewModel = viewModel(),
) {
    LaunchedEffect(target) { vm.open(target) }
    val state by vm.state.collectAsStateWithLifecycle()
    val courses by vm.courses.collectAsStateWithLifecycle()
    val canRemove by vm.canRemove.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var confirmRemoval by remember { mutableStateOf(false) }
    var drawerOpen by remember { mutableStateOf(false) }
    val drawerState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    /** O curso olhado agora — o da rota, o escolhido na gaveta, ou o aberto. */
    val viewed = state.languagePair.target
    val empty = state.total == 0

    fun closeDrawer() {
        scope.launch { drawerState.hide() }.invokeOnCompletion { drawerOpen = false }
    }

    if (confirmRemoval) {
        AlertDialog(
            onDismissRequest = { confirmRemoval = false },
            title = { Text("Sair do ${languageOf(viewed).displayName.lowercase()}?") },
            text = { Text("As fichas continuam guardadas — o idioma volta com tudo se você matricular de novo.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemoval = false
                    vm.removeCourse(viewed)
                    onBack()
                }) { Text("Remover", color = colors.error) }
            },
            dismissButton = { TextButton(onClick = { confirmRemoval = false }) { Text("Manter") } },
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(13.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        InnerHeader("Seu progresso", onBack, Modifier.padding(top = 8.dp)) {
            CoursePill(target = viewed, opened = drawerOpen, onClick = { drawerOpen = true })
        }

        WeekCard(state = state, empty = empty, onOpen = { onOpenDayByDay(viewed) })

        StockCard(state = state, empty = empty, onOpen = { onOpenWhatsLeft(viewed) })

        if (canRemove) {
            SecondaryAction(
                text = "Remover o ${languageOf(viewed).displayName.lowercase()} da faixa",
                onClick = { confirmRemoval = true },
            )
        }

        Spacer(Modifier.navigationBarsPadding().height(110.dp))
    }

    if (drawerOpen) {
        ModalBottomSheet(onDismissRequest = { drawerOpen = false }, sheetState = drawerState) {
            CourseDrawer(
                courses = courses,
                viewed = viewed,
                onChoose = {
                    vm.open(it)
                    closeDrawer()
                },
                onAdd = {
                    closeDrawer()
                    onAddLanguage()
                },
            )
        }
    }
}

/**
 * O primeiro bloco: a semana e a quota de hoje, no mesmo cartão.
 *
 * Os dois estão juntos porque respondem a mesma pergunta em dois prazos —
 * "andei esta semana?" e "andei hoje?". Separá-los faria a quota parecer uma
 * meta à parte, e ela é só o dia de hoje da faixa logo acima.
 */
@Composable
private fun WeekCard(state: ProgressState, empty: Boolean, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val content: @Composable ColumnScope.() -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${state.month} · esta semana",
                style = MaterialTheme.typography.labelMedium,
                color = if (empty) colors.outline else colors.onSurfaceVariant,
            )
            if (!empty) {
                Icon(
                    imageVector = AppIcons.Forward,
                    contentDescription = null,
                    tint = colors.outline,
                    modifier = Modifier.size(16.dp).padding(start = 2.dp),
                )
            }
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
            dashed = empty,
            modifier = Modifier.padding(top = 13.dp),
        )

        Box(Modifier.fillMaxWidth().padding(vertical = 13.dp).height(1.dp).background(colors.outlineVariant))

        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Quota de hoje no ${languageOf(state.languagePair.target).displayName.lowercase()}",
                style = MaterialTheme.typography.titleSmall,
                color = if (empty) colors.onSurfaceVariant else colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = quotaText(state.quota),
                style = MaterialTheme.typography.bodySmall,
                color = if (empty) colors.outline else colors.onSurfaceVariant,
            )
        }

        // Sem barra no vazio: uma trilha cinza de ponta a ponta é uma promessa de
        // que existe alguma coisa para preencher hoje, e não existe ainda.
        if (!empty) {
            val advance by animatedFraction(state.quota.fraction, "fracaoDaQuota")
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(8.dp)
                    .background(colors.outlineVariant, RoundedCornerShape(4.dp)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(advance)
                        .height(8.dp)
                        .background(colors.tertiary, RoundedCornerShape(4.dp)),
                )
            }
        }
    }

    if (empty) {
        DashedBox(
            modifier = Modifier.fillMaxWidth(),
            radius = 22.dp,
            filling = PaddingValues(16.dp),
            content = content,
        )
    } else {
        ScreenCard(
            onClick = onOpen,
            filling = PaddingValues(16.dp),
            modifier = Modifier.fillMaxWidth(),
            content = content,
        )
    }
}

/**
 * O segundo bloco: quanto do estoque já é seu.
 *
 * Vazio, ele não mostra "0 de 0" nem um anel zerado — mostra o lugar do anel e
 * diz quando ele começa a existir. A promessa é datada: quatro revisões certas,
 * que é literalmente a escada de [Degraus] do primeiro degrau ao último.
 */
@Composable
private fun StockCard(state: ProgressState, empty: Boolean, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    if (empty) {
        DashedBox(modifier = Modifier.fillMaxWidth(), radius = 22.dp, filling = PaddingValues(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.size(78.dp).dashedOutline(colors.outline, radius = 39.dp, thickness = 2.dp))
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(
                        text = "Palavras que já são suas",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurfaceVariant,
                    )
                    Text(
                        text = whenItAppearsText(),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.outline,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
            StockLegend(
                labels = listOf("dominadas", "familiares", "aprendendo"),
                color = colors.outline,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
        return
    }

    ScreenCard(onClick = onOpen, filling = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            ProgressRing(
                fraction = state.mastered.toFloat() / state.total,
                size = 78.dp,
                thickness = 9.dp,
            ) {
                // Conta do zero junto com o arco: é a contagem de conquista
                // acumulada que `contagemAnimada` existe para servir.
                Text(
                    text = "${animatedCount(state.mastered, "mastered")}",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "de ${state.total}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(stockTitle(state.mastered), style = MaterialTheme.typography.titleLarge)
                Text(
                    text = closeToLevelingText(state.closeToLeveling.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
            Icon(AppIcons.Forward, null, tint = colors.outline, modifier = Modifier.size(20.dp))
        }

        BandBars(
            strips = listOf(
                state.mastered to colors.tertiary,
                state.familiar to colors.tertiary.copy(alpha = 0.55f),
                state.learning to colors.outlineVariant,
            ),
            modifier = Modifier.padding(top = 14.dp),
        )

        StockLegend(
            labels = listOf(
                "${state.mastered} dominadas",
                "${state.familiar} familiares",
                "${state.learning} aprendendo",
            ),
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

/**
 * Os três nomes das faixas, espalhados na largura do cartão.
 *
 * Sem quadradinho de cor: a barra logo acima já está na mesma ordem, e um
 * marcador por rótulo repetiria o que ela diz melhor. No estado vazio os mesmos
 * três nomes ficam sozinhos, sem número — é o rótulo do lugar, não um placar.
 */
@Composable
private fun StockLegend(labels: List<String>, color: Color, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = modifier.fillMaxWidth()) {
        labels.forEach { label ->
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = color)
        }
    }
}

/**
 * A bandeira do curso no cabeçalho — e o botão que troca de curso.
 *
 * Sem ela seriam telas iguais e nenhum jeito de dizer qual é qual; sem o chevron
 * ela seria só um rótulo, e trocar de idioma exigiria voltar até a tela Você.
 * O chevron aponta para cima enquanto a gaveta está aberta, que é o que promete
 * que outro toque a fecha.
 */
@Composable
private fun CoursePill(target: String, opened: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val language = languageOf(target)
    val toque = rememberHaptics()
    val spin by animateFloatAsState(
        targetValue = if (opened) 180f else 0f,
        animationSpec = tween(Motion.DEFAULT),
        label = "giroDaPilula",
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (opened) colors.secondaryContainer else colors.surface,
        border = if (opened) BorderStroke(1.5.dp, colors.primary) else cardOutline(),
        interactionSource = toque,
        modifier = Modifier.shrinkOnTouch(toque, minimum = 0.94f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.padding(start = 5.dp, end = 9.dp, top = 5.dp, bottom = 5.dp),
        ) {
            CircularFlag(language, size = 20.dp)
            Text(
                text = language.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = if (opened) colors.primary else colors.onSurfaceVariant,
            )
            Icon(
                imageVector = AppIcons.Expand,
                contentDescription = "Trocar idioma",
                tint = colors.primary,
                modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = spin },
            )
        }
    }
}

/**
 * A gaveta da bandeira: os cursos matriculados, com o que cada um já rendeu.
 *
 * Cada linha traz o próprio "9 de 24" porque a pergunta que leva alguém a abrir
 * a gaveta é justamente comparar — e obrigar a entrar em cada idioma para
 * descobrir onde está o progresso seria pedir três viagens para uma resposta.
 */
@Composable
private fun CourseDrawer(
    courses: List<CourseSummary>,
    viewed: String,
    onChoose: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = "Ver progresso em",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 5.dp),
        )

        courses.forEach { course ->
            DrawerRow(
                course = course,
                chosen = course.languagePair.target == viewed,
                onClick = { onChoose(course.languagePair.target) },
            )
        }

        DashedBox(
            modifier = Modifier.fillMaxWidth(),
            onClick = onAdd,
            filling = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(32.dp).background(colors.surfaceVariant, CircleShape),
                ) {
                    Icon(AppIcons.Plus, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                }
                Text("Adicionar idioma", style = MaterialTheme.typography.titleSmall, color = colors.primary)
            }
        }

        Spacer(Modifier.navigationBarsPadding().height(14.dp))
    }
}

@Composable
private fun DrawerRow(course: CourseSummary, chosen: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val language = languageOf(course.languagePair.target)
    val toque = rememberHaptics()

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (chosen) colors.secondaryContainer else colors.surface,
        border = if (chosen) BorderStroke(1.5.dp, colors.primary) else cardOutline(),
        interactionSource = toque,
        modifier = Modifier.fillMaxWidth().shrinkOnTouch(toque),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            CircularFlag(language, size = 32.dp)
            Column(Modifier.weight(1f)) {
                Text(language.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = courseSummaryText(course),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (chosen) colors.primary else colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .then(
                        if (chosen) Modifier.background(colors.primary, CircleShape)
                        else Modifier.border(2.dp, colors.outline, CircleShape),
                    ),
            ) {
                if (chosen) {
                    Icon(AppIcons.Check, null, tint = colors.onPrimary, modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

internal fun streakLabel(days: Int): String =
    if (days == 1) "1 dia seguido" else "$days dias seguidos"

/**
 * O número da quota, à direita do rótulo: "6 de 10".
 *
 * Travessão quando o dia não pediu nada — seja porque não há palavra nenhuma,
 * seja porque nenhuma venceu hoje. "0 de 0" seria um placar de uma partida que
 * não houve.
 */
internal fun quotaText(quota: DailyQuota): String =
    if (quota.total == 0) "—" else "${quota.done} de ${quota.total}"

/** A linha do anel vazio: quando é que ele passa a existir. */
internal fun whenItAppearsText(): String =
    "aparece depois de ${SPELLED_NUMBERS[Steps.TOTAL - 1].lowercase()} revisões"

/** "9 de 24 já são suas" — ou a falta delas, sem número inventado. */
internal fun courseSummaryText(course: CourseSummary): String =
    if (course.total == 0) "nenhuma palavra ainda" else "${course.mastered} de ${course.total} já são suas"

private val SPELLED_NUMBERS = listOf(
    "Nenhuma", "Uma", "Duas", "Três", "Quatro", "Cinco", "Seis", "Sete", "Oito", "Nove", "Dez",
)

/**
 * "Nove palavras já são suas".
 *
 * Por extenso até dez porque é assim que se lê em voz alta uma conquista; a
 * partir daí o algarismo volta, que é como se lê um número grande.
 */
internal fun stockTitle(mastered: Int): String = when {
    mastered == 0 -> "Nenhuma palavra é sua ainda"
    mastered == 1 -> "Uma palavra já é sua"
    mastered <= 10 -> "${SPELLED_NUMBERS[mastered]} palavras já são suas"
    else -> "$mastered palavras já são suas"
}

internal fun closeToLevelingText(count: Int): String = when (count) {
    0 -> "Nenhuma está perto de virar."
    1 -> "1 está perto de virar."
    else -> "$count estão perto de virar."
}
