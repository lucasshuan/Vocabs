package com.jean.vocabs.ui.review

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.domain.clozeSnippet
import com.jean.vocabs.ui.components.AppIcons
import com.jean.vocabs.ui.components.CircularButton
import com.jean.vocabs.ui.components.EmptyState
import com.jean.vocabs.ui.components.MetricCard
import com.jean.vocabs.ui.components.Motion
import com.jean.vocabs.ui.components.PrimaryButton
import com.jean.vocabs.ui.components.ScreenCard
import com.jean.vocabs.ui.components.SecondaryAction
import com.jean.vocabs.ui.components.SectionLabel
import com.jean.vocabs.ui.components.animatedCount
import com.jean.vocabs.ui.components.animatedFraction
import com.jean.vocabs.ui.components.smoothEntrance

private const val GAP = "________"

@Composable
fun ReviewScreen(onBack: () -> Unit, vm: ReviewViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 8.dp),
        ) {
            CircularButton(AppIcons.Close, "Fechar revisão", onBack)
            val card = state as? ReviewState.CardSurface
            ProgressBar(
                fraction = card?.let { it.position.toFloat() / it.total.coerceAtLeast(1) } ?: 0f,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            )
            Text(
                text = card?.let { "${it.position}/${it.total}" }.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when (val current = state) {
            ReviewState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            ReviewState.Empty -> Empty(onBack)
            is ReviewState.CardSurface -> CardSurface(current, vm)
            is ReviewState.Summary -> Summary(current, vm, onBack)
        }
    }
}

/**
 * A barra do topo, que avança um cartão de cada vez.
 *
 * É a única coisa na tela que mostra a sessão inteira, e ela só cumpre esse papel
 * se o avanço for visto: saltando de um quadro para o outro junto com o cartão
 * novo, ela some no meio da troca e a sessão vira uma sequência de perguntas sem
 * fim à vista.
 */
@Composable
private fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val advance by animatedFraction(fraction, "avancoDaSessao")
    Box(
        modifier = modifier
            .height(6.dp)
            .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(advance)
                .height(6.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)),
        )
    }
}

@Composable
private fun CardSurface(state: ReviewState.CardSurface, vm: ReviewViewModel) {
    val keyboard = LocalSoftwareKeyboardController.current
    // O veredito que a caixa mostra é uma cópia, e não uma leitura do estado.
    // "Continuar" limpa o feedback e troca o cartão no mesmo instante, enquanto a
    // caixa ainda está encolhendo: lendo o estado ao vivo, esses 150 ms de saída
    // exibiriam a resposta do cartão **seguinte** — a revisão entregaria de
    // graça a palavra que está prestes a perguntar.
    var lastVerdict by remember { mutableStateOf<Pair<ReviewFeedback, String>?>(null) }
    LaunchedEffect(state.feedback, state.entry.id) {
        state.feedback?.let { lastVerdict = it to state.entry.target.orEmpty() }
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // O trecho é a única coisa que muda de um cartão para o seguinte, e por
        // isso é ele — e não a tela — que se move: o antigo sai pela esquerda, o
        // novo entra pela direita, e o campo de resposta logo abaixo fica onde
        // está, com o cursor intacto. Trocar a coluna inteira derrubaria o foco
        // do teclado a cada palavra respondida.
        AnimatedContent(
            targetState = state.entry,
            transitionSpec = {
                (slideInHorizontally(tween(Motion.DEFAULT)) { width -> width / 4 } + fadeIn(tween(Motion.DEFAULT)))
                    .togetherWith(slideOutHorizontally(tween(Motion.FAST)) { width -> -width / 4 } + fadeOut(tween(Motion.FAST)))
            },
            contentKey = { it.id },
            label = "cartaoDaRevisao",
        ) { entry ->
            ScreenCard(
                shape = MaterialTheme.shapes.extraLarge,
                filling = PaddingValues(24.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                SectionLabel("COMPLETE O SEU TRECHO")
                Text(
                    text = annotatedCloze(clozeSnippet(entry, GAP)),
                    style = MaterialTheme.typography.headlineSmall.copy(lineHeight = MaterialTheme.typography.headlineLarge.lineHeight),
                    modifier = Modifier.padding(vertical = 22.dp),
                )
                Text(
                    entry.card?.translation.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        OutlinedTextField(
            value = state.answer,
            onValueChange = vm::editAnswer,
            enabled = state.feedback == null,
            placeholder = { Text("Escreva o termo original") },
            singleLine = false,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); vm.confirm() }),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )

        // O veredito se abre empurrando o botão para baixo, em vez de aparecer
        // pronto no lugar dele. É o único instante da revisão em que a pessoa
        // descobriu alguma coisa, e 240 ms de abertura é o que separa "a tela
        // respondeu" de "a tela já estava assim".
        AnimatedVisibility(
            visible = state.feedback != null,
            enter = expandVertically(tween(Motion.DEFAULT)) + fadeIn(tween(Motion.DEFAULT)),
            exit = shrinkVertically(tween(Motion.FAST)) + fadeOut(tween(Motion.FAST)),
        ) {
            // Ao vivo enquanto o feedback existe, pela cópia enquanto ele sai —
            // assim a caixa nunca fica um quadro sem conteúdo na entrada nem
            // mostra o cartão errado na saída.
            val verdict = state.feedback?.let { it to state.entry.target.orEmpty() } ?: lastVerdict
            if (verdict != null) {
                val (feedback, answer) = verdict
                val correct = feedback == ReviewFeedback.CORRETA
                Surface(
                    color = if (correct) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer,
                    contentColor = if (correct) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            when {
                                correct -> "Boa — você lembrou"
                                feedback == ReviewFeedback.NAO_LEMBRO -> "Tudo bem. A resposta é:"
                                else -> "Quase. A resposta é:"
                            },
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(answer, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }

        if (state.feedback == null) {
            PrimaryButton("Verificar", { keyboard?.hide(); vm.confirm() }, enabled = state.answer.isNotBlank())
            SecondaryAction("Não lembro", { keyboard?.hide(); vm.dontRemember() })
        } else {
            PrimaryButton("Continuar", vm::advance)
        }
        Spacer(Modifier.navigationBarsPadding().height(20.dp))
    }
}

/** A lacuna vira um traço de ameixa: no handoff ela é um sublinhado, não underscores. */
@Composable
private fun annotatedCloze(snippet: String) = buildAnnotatedString {
    val cut = snippet.indexOf(GAP)
    if (cut < 0) {
        append(snippet)
        return@buildAnnotatedString
    }
    append(snippet.substring(0, cut))
    pushStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline))
    append(GAP)
    pop()
    append(snippet.substring(cut + GAP.length))
}

@Composable
private fun Empty(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            icon = AppIcons.Check,
            title = "Memória em dia",
            detail = "Volte quando alguma ficha pedir reforço.",
            action = { PrimaryButton("Voltar", onBack, Modifier.padding(horizontal = 40.dp)) },
        )
    }
}

@Composable
private fun Summary(state: ReviewState.Summary, vm: ReviewViewModel, onBack: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        Text(
            text = "Sessão concluída",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.smoothEntrance().padding(top = 48.dp),
        )
        // Acertos e sequência contam do zero; "para reforçar" não. Os dois
        // primeiros são o que a sessão rendeu, e vê-los subir é a recompensa da
        // tela inteira — o terceiro é o que ficou faltando, e uma contagem
        // crescente ali transformaria o erro em placar.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
            MetricCard("${animatedCount(state.hits, "hits")}", "acertos", Modifier.weight(1f), highlight = true)
            MetricCard("${state.misses}", "para reforçar", Modifier.weight(1f))
            MetricCard(
                value = "${animatedCount(state.dayStreak, "dayStreak")}",
                label = "dias seguidos",
                modifier = Modifier.weight(1f),
                highlight = true,
            )
        }
        if (state.wrong.isNotEmpty()) {
            Text(
                "Voltam para a fila: ${state.wrong.distinct().joinToString()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.smoothEntrance(index = 2).padding(top = 20.dp),
            )
        }
        PrimaryButton("Concluir", onBack, Modifier.smoothEntrance(index = 3).padding(top = 28.dp))
        if (state.rest > 0) SecondaryAction("Mais uma rodada (${state.rest})", vm::newRound)
        Spacer(Modifier.navigationBarsPadding().height(20.dp))
    }
}
