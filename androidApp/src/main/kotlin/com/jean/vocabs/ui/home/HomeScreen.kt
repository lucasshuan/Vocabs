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
import com.jean.vocabs.ui.components.entradaSuave
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
    aoCapturar: () -> Unit,
    aoRevisar: () -> Unit,
    aoAbrirPerfil: () -> Unit,
    aoAdicionarIdioma: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val paginas = estado.paginas
    val pager = rememberPagerState(pageCount = { paginas.size })

    // Duas direções, e a ordem entre elas importa. O pager nasce na página 0, que
    // quase nunca é o curso aberto — deixá-lo mandar antes de estar posicionado
    // faria abrir o app no inglês trocar o curso para o inglês, em silêncio.
    // Por isso ele só passa a mandar depois do primeiro posicionamento.
    var posicionado by remember { mutableStateOf(false) }

    LaunchedEffect(paginas.size, estado.ativo) {
        if (paginas.isEmpty()) return@LaunchedEffect
        if (!posicionado) {
            pager.scrollToPage(estado.indiceAtivo)
            posicionado = true
        } else if (estado.indiceAtivo != pager.currentPage && !pager.isScrollInProgress) {
            pager.animateScrollToPage(estado.indiceAtivo)
        }
    }

    LaunchedEffect(posicionado) {
        if (!posicionado) return@LaunchedEffect
        snapshotFlow { pager.settledPage }.collect { index ->
            paginas.getOrNull(index)?.let { vm.openCourse(it.languagePair.target) }
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
            Surface(onClick = aoAbrirPerfil, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = AppIcons.Pessoa,
                        contentDescription = "Você",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        if (estado.temCarrossel) {
            LanguageStrip(
                courses = estado.courses,
                ativo = estado.ativo,
                aoEscolher = vm::openCourse,
                aoAdicionar = aoAdicionarIdioma,
                modifier = Modifier.padding(bottom = 14.dp),
            )
        }

        HorizontalPager(
            state = pager,
            beyondViewportPageCount = 1,
            modifier = Modifier.weight(1f),
        ) { index ->
            paginas.getOrNull(index)?.let { pagina ->
                CoursePage(
                    pagina = pagina,
                    aoRevisar = aoRevisar,
                    aoCapturar = aoCapturar,
                )
            }
        }

        // Fora do pager, de propósito: aqui os pontos não pertencem a nenhuma
        // página nem a nenhum cartão, e ficam no mesmo lugar não importa o que
        // muda acima.
        PageDots(
            total = paginas.size,
            current = pager.currentPage,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 10.dp),
        )
        Spacer(Modifier.navigationBarsPadding().height(BAR_SPACING))
    }
}

@Composable
private fun CoursePage(
    pagina: HomePage,
    aoRevisar: () -> Unit,
    aoCapturar: () -> Unit,
) {
    val language = languageOf(pagina.languagePair.target)
    val resumo = pagina.resumo

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        ScreenCard(
            forma = MaterialTheme.shapes.extraLarge,
            recheio = PaddingValues(18.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CourseRing(pagina)
                Column(Modifier.weight(1f).padding(start = 15.dp)) {
                    Text("Seu ${language.displayName.lowercase()}", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = courseDetail(resumo.total, resumo.mastered, resumo.inQueue),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            if (resumo.inQueue > 0) {
                PrimaryButton(
                    text = "Revisar ${resumo.inQueue} ${if (resumo.inQueue == 1) "word" else "words"}",
                    aoClicar = aoRevisar,
                    modifier = Modifier.padding(top = 15.dp),
                )
            } else {
                UpNextRow(pagina, Modifier.padding(top = 15.dp))
            }
        }

        // As capturas do dia entram escalonadas, de cima para baixo. É a lista
        // que cresce enquanto a pessoa usa o app, e é o único lugar do Início
        // onde ela vê o próprio dia se acumulando — chegar montada faz três
        // capturas parecerem um histórico velho em vez do que aconteceu hoje.
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            SectionLabel("Capturadas hoje em ${language.displayName.lowercase()}")
            if (pagina.capturadasHoje.isEmpty()) {
                CaptureInvite(language.displayName.lowercase(), aoCapturar)
            } else {
                pagina.capturadasHoje.forEachIndexed { index, entry ->
                    CapturedRow(entry, Modifier.entradaSuave(index))
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
private fun CourseRing(pagina: HomePage) {
    val cores = MaterialTheme.colorScheme
    val emDia = pagina.resumo.inQueue == 0 && pagina.resumo.total > 0
    ProgressRing(
        fraction = if (emDia) 1f else pagina.forcaMedia / 100f,
        tamanho = 70.dp,
        espessura = 8.dp,
    ) {
        if (emDia) {
            Icon(AppIcons.Check, null, tint = cores.tertiary, modifier = Modifier.size(26.dp))
        } else {
            // A porcentagem sobe no mesmo tempo em que o arco corre: os dois são
            // a mesma medida, e vê-los chegarem juntos é o que impede o anel de
            // parecer decoração ao redor de um número.
            Text("${animatedCount(pagina.forcaMedia, "forcaMedia")}%", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

/** "Próximas 5 em 19h · nada a fazer hoje" — o cartão de um curso sem fila. */
@Composable
private fun UpNextRow(pagina: HomePage, modifier: Modifier = Modifier) {
    val cores = MaterialTheme.colorScheme
    val proxima = pagina.resumo.nextInMillis
    ScreenCard(
        forma = MaterialTheme.shapes.medium,
        cor = cores.surfaceVariant,
        recheio = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconDisc(
                icon = if (proxima == null) AppIcons.Mais else AppIcons.Relogio,
                descricao = null,
                cor = cores.onSurfaceVariant,
                fundo = cores.surface,
                tamanho = 34.dp,
            )
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = when {
                        proxima == null -> "Nada agendado ainda"
                        pagina.proximasEm24h > 1 -> "Próximas ${pagina.proximasEm24h} ${timeUntil(proxima)}"
                        else -> "Próxima ${timeUntil(proxima)}"
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = if (proxima == null) "capture algo para começar" else "nada a fazer hoje",
                    style = MaterialTheme.typography.bodySmall,
                    color = cores.onSurfaceVariant,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
    }
}

@Composable
private fun CapturedRow(entry: Entry, modifier: Modifier = Modifier) {
    ScreenCard(
        forma = MaterialTheme.shapes.small,
        recheio = PaddingValues(horizontal = 15.dp, vertical = 11.dp),
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
private fun CaptureInvite(language: String, aoClicar: () -> Unit) {
    DashedBox(
        modifier = Modifier.fillMaxWidth(),
        recheio = PaddingValues(18.dp),
        aoClicar = aoClicar,
    ) {
        Text(
            text = "Nada ainda. Ouviu alguma coisa hoje que valia guardar?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PrimaryButton(
            text = "Capturar em $language",
            aoClicar = aoClicar,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

private fun courseDetail(total: Int, mastered: Int, inQueue: Int): String {
    val estoque = if (total == 0) "nenhuma ficha ainda" else "$total ${if (total == 1) "card" else "cards"} · $mastered ${if (mastered == 1) "dominada" else "mastered"}"
    val fila = when {
        total == 0 -> "capture a primeira"
        inQueue == 0 -> "nada esfriou hoje"
        inQueue == 1 -> "1 esfriou hoje"
        else -> "$inQueue esfriaram hoje"
    }
    return "$estoque\n$fila"
}

/** A barra de baixo mais o vão do botão de captura, que passa dela para cima. */
private val BAR_SPACING = 92.dp
