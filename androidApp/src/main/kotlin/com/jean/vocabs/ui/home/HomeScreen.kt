package com.jean.vocabs.ui.home

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.R
import com.jean.vocabs.shared.domain.Entry
import com.jean.vocabs.ui.components.AppIcons
import com.jean.vocabs.ui.components.DashedBox
import com.jean.vocabs.ui.components.IconDisc
import com.jean.vocabs.ui.components.LanguageStrip
import com.jean.vocabs.ui.components.PageDots
import com.jean.vocabs.ui.components.PrimaryButton
import com.jean.vocabs.ui.components.ProgressRing
import com.jean.vocabs.ui.components.ScreenCard
import com.jean.vocabs.ui.components.SectionLabel
import com.jean.vocabs.ui.components.animatedCount
import com.jean.vocabs.ui.components.smoothEntrance
import com.jean.vocabs.ui.components.timeUntil
import com.jean.vocabs.ui.displayName
import com.jean.vocabs.ui.languages.languageOf

/**
 * Tela 01/02 do handoff — a Início, uma página por curso.
 *
 * É a **única** aba recortada por idioma, e a troca é um deslize. As outras três
 * mostram sempre tudo: um filtro que continuasse ligado ao mudar de aba faria
 * palavras sumirem sem que ninguém tivesse pedido.
 *
 * Deslizar não é só navegar — é trocar o curso aberto. Por isso o botão de
 * revisar e a folha do `+` seguem a página visível sem que nenhum dos dois
 * precise saber que existe um carrossel.
 */
@Composable
fun HomeScreen(
    onCapture: () -> Unit,
    onReview: () -> Unit,
    onOpenProfile: () -> Unit,
    onAddLanguage: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val pages = state.pages
    val pager = rememberPagerState(pageCount = { pages.size })

    // Duas direções, e a ordem entre elas importa. O pager nasce na página 0, que
    // quase nunca é o curso aberto — deixá-lo mandar antes de estar posicionado
    // faria abrir o app no inglês trocar o curso para o inglês, em silêncio.
    // Por isso ele só passa a mandar depois do primeiro posicionamento.
    var positioned by remember { mutableStateOf(false) }

    LaunchedEffect(pages.size, state.activeTarget) {
        if (pages.isEmpty()) return@LaunchedEffect
        if (!positioned) {
            pager.scrollToPage(state.activeIndex)
            positioned = true
        } else if (state.activeIndex != pager.currentPage && !pager.isScrollInProgress) {
            pager.animateScrollToPage(state.activeIndex)
        }
    }

    LaunchedEffect(positioned) {
        if (!positioned) return@LaunchedEffect
        snapshotFlow { pager.settledPage }.collect { index ->
            pages.getOrNull(index)?.let { vm.openCourse(it.languagePair.target) }
        }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 14.dp),
        ) {
            Image(painterResource(R.drawable.logo_vocabu), stringResource(R.string.logo_description), Modifier.size(34.dp))
            Text("Vocabu", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 9.dp))
            Spacer(Modifier.weight(1f))
            Surface(onClick = onOpenProfile, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = AppIcons.Person,
                        contentDescription = "Você",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        if (state.hasCarousel) {
            LanguageStrip(
                courses = state.courses,
                activeTarget = state.activeTarget,
                onChoose = vm::openCourse,
                onAdd = onAddLanguage,
                modifier = Modifier.padding(bottom = 14.dp),
            )
        }

        HorizontalPager(
            state = pager,
            beyondViewportPageCount = 1,
            modifier = Modifier.weight(1f),
        ) { index ->
            pages.getOrNull(index)?.let { page ->
                CoursePage(
                    page = page,
                    onReview = onReview,
                    onCapture = onCapture,
                )
            }
        }

        // Fora do pager, de propósito: aqui os pontos não pertencem a nenhuma
        // página nem a nenhum cartão, e ficam no mesmo lugar não importa o que
        // muda acima.
        PageDots(
            total = pages.size,
            current = pager.currentPage,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 10.dp),
        )
        Spacer(Modifier.navigationBarsPadding().height(BAR_SPACING))
    }
}

@Composable
private fun CoursePage(
    page: HomePage,
    onReview: () -> Unit,
    onCapture: () -> Unit,
) {
    val language = languageOf(page.languagePair.target)
    val summary = page.summary

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        ScreenCard(
            shape = MaterialTheme.shapes.extraLarge,
            filling = PaddingValues(18.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CourseRing(page)
                Column(Modifier.weight(1f).padding(start = 15.dp)) {
                    Text("Seu ${language.displayName.lowercase()}", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = courseDetail(summary.total, summary.mastered, summary.inQueue),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            if (summary.inQueue > 0) {
                PrimaryButton(
                    text = "Revisar ${summary.inQueue} ${if (summary.inQueue == 1) "word" else "words"}",
                    onClick = onReview,
                    modifier = Modifier.padding(top = 15.dp),
                )
            } else {
                UpNextRow(page, Modifier.padding(top = 15.dp))
            }
        }

        // As capturas do dia entram escalonadas, de cima para baixo. É a lista
        // que cresce enquanto a pessoa usa o app, e é o único lugar do Início
        // onde ela vê o próprio dia se acumulando — chegar montada faz três
        // capturas parecerem um histórico velho em vez do que aconteceu hoje.
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            SectionLabel("Capturadas hoje em ${language.displayName.lowercase()}")
            if (page.capturedToday.isEmpty()) {
                CaptureInvite(language.displayName.lowercase(), onCapture)
            } else {
                page.capturedToday.forEachIndexed { index, entry ->
                    CapturedRow(entry, Modifier.smoothEntrance(index))
                }
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

/**
 * O anel do curso: a força média por dentro, ou o tique quando não há fila.
 *
 * Curso em dia repete no anel o mesmo tique que a bandeira mostra na faixa — é a
 * confirmação de que o selo lá em cima não estava falando de outra coisa.
 */
@Composable
private fun CourseRing(page: HomePage) {
    val colors = MaterialTheme.colorScheme
    val upToDate = page.summary.inQueue == 0 && page.summary.total > 0
    ProgressRing(
        fraction = if (upToDate) 1f else page.averageStrength / 100f,
        size = 70.dp,
        thickness = 8.dp,
    ) {
        if (upToDate) {
            Icon(AppIcons.Check, null, tint = colors.tertiary, modifier = Modifier.size(26.dp))
        } else {
            // A porcentagem sobe no mesmo tempo em que o arco corre: os dois são
            // a mesma medida, e vê-los chegarem juntos é o que impede o anel de
            // parecer decoração ao redor de um número.
            Text("${animatedCount(page.averageStrength, "averageStrength")}%", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

/** "Próximas 5 em 19h · nada a fazer hoje" — o cartão de um curso sem fila. */
@Composable
private fun UpNextRow(page: HomePage, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val next = page.summary.nextInMillis
    ScreenCard(
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceVariant,
        filling = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconDisc(
                icon = if (next == null) AppIcons.Plus else AppIcons.Clock,
                contentDescription = null,
                color = colors.onSurfaceVariant,
                background = colors.surface,
                size = 34.dp,
            )
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = when {
                        next == null -> "Nada agendado ainda"
                        page.nextIn24h > 1 -> "Próximas ${page.nextIn24h} ${timeUntil(next)}"
                        else -> "Próxima ${timeUntil(next)}"
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = if (next == null) "capture algo para começar" else "nada a fazer hoje",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
    }
}

@Composable
private fun CapturedRow(entry: Entry, modifier: Modifier = Modifier) {
    ScreenCard(
        shape = MaterialTheme.shapes.small,
        filling = PaddingValues(horizontal = 15.dp, vertical = 11.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(entry.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                text = entry.card?.translation.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Dia sem captura vira convite, e não tela vazia — o custo de capturar é o ponto do app. */
@Composable
private fun CaptureInvite(language: String, onClick: () -> Unit) {
    DashedBox(
        modifier = Modifier.fillMaxWidth(),
        filling = PaddingValues(18.dp),
        onClick = onClick,
    ) {
        Text(
            text = "Nada ainda. Ouviu alguma coisa hoje que valia guardar?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PrimaryButton(
            text = "Capturar em $language",
            onClick = onClick,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

private fun courseDetail(total: Int, mastered: Int, inQueue: Int): String {
    val stock = if (total == 0) "nenhuma ficha ainda" else "$total ${if (total == 1) "card" else "cards"} · $mastered ${if (mastered == 1) "dominada" else "mastered"}"
    val queue = when {
        total == 0 -> "capture a primeira"
        inQueue == 0 -> "nada esfriou hoje"
        inQueue == 1 -> "1 esfriou hoje"
        else -> "$inQueue esfriaram hoje"
    }
    return "$stock\n$queue"
}

/** A barra de baixo mais o vão do botão de captura, que passa dela para cima. */
private val BAR_SPACING = 92.dp
