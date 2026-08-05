package com.jean.vocabs.ui.revisao

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
import com.jean.vocabs.shared.domain.trechoCloze
import com.jean.vocabs.ui.components.AcaoSecundaria
import com.jean.vocabs.ui.components.BotaoPrincipal
import com.jean.vocabs.ui.components.BotaoCircular
import com.jean.vocabs.ui.components.CartaoDaTela
import com.jean.vocabs.ui.components.CartaoMetrica
import com.jean.vocabs.ui.components.EstadoVazio
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.Movimento
import com.jean.vocabs.ui.components.RotuloDeSecao
import com.jean.vocabs.ui.components.contagemAnimada
import com.jean.vocabs.ui.components.entradaSuave
import com.jean.vocabs.ui.components.fracaoAnimada

private const val LACUNA = "________"

@Composable
fun RevisaoScreen(aoVoltar: () -> Unit, vm: RevisaoViewModel = viewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 8.dp),
        ) {
            BotaoCircular(Icones.Fechar, "Fechar revisão", aoVoltar)
            val cartao = estado as? RevisaoEstado.Cartao
            BarraDeProgresso(
                fracao = cartao?.let { it.posicao.toFloat() / it.total.coerceAtLeast(1) } ?: 0f,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            )
            Text(
                text = cartao?.let { "${it.posicao}/${it.total}" }.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when (val atual = estado) {
            RevisaoEstado.Carregando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            RevisaoEstado.Vazia -> Vazia(aoVoltar)
            is RevisaoEstado.Cartao -> Cartao(atual, vm)
            is RevisaoEstado.Resumo -> Resumo(atual, vm, aoVoltar)
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
private fun BarraDeProgresso(fracao: Float, modifier: Modifier = Modifier) {
    val avanco by fracaoAnimada(fracao, "avancoDaSessao")
    Box(
        modifier = modifier
            .height(6.dp)
            .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(avanco)
                .height(6.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)),
        )
    }
}

@Composable
private fun Cartao(estado: RevisaoEstado.Cartao, vm: RevisaoViewModel) {
    val teclado = LocalSoftwareKeyboardController.current
    // O veredito que a caixa mostra é uma cópia, e não uma leitura do estado.
    // "Continuar" limpa o feedback e troca o cartão no mesmo instante, enquanto a
    // caixa ainda está encolhendo: lendo o estado ao vivo, esses 150 ms de saída
    // exibiriam a resposta do cartão **seguinte** — a revisão entregaria de
    // graça a palavra que está prestes a perguntar.
    var ultimoVeredito by remember { mutableStateOf<Pair<FeedbackRevisao, String>?>(null) }
    LaunchedEffect(estado.feedback, estado.entrada.id) {
        estado.feedback?.let { ultimoVeredito = it to estado.entrada.alvo.orEmpty() }
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
            targetState = estado.entrada,
            transitionSpec = {
                (slideInHorizontally(tween(Movimento.PADRAO)) { largura -> largura / 4 } + fadeIn(tween(Movimento.PADRAO)))
                    .togetherWith(slideOutHorizontally(tween(Movimento.RAPIDO)) { largura -> -largura / 4 } + fadeOut(tween(Movimento.RAPIDO)))
            },
            contentKey = { it.id },
            label = "cartaoDaRevisao",
        ) { entrada ->
            CartaoDaTela(
                forma = MaterialTheme.shapes.extraLarge,
                recheio = PaddingValues(24.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                RotuloDeSecao("COMPLETE O SEU TRECHO")
                Text(
                    text = clozeAnotado(trechoCloze(entrada, LACUNA)),
                    style = MaterialTheme.typography.headlineSmall.copy(lineHeight = MaterialTheme.typography.headlineLarge.lineHeight),
                    modifier = Modifier.padding(vertical = 22.dp),
                )
                Text(
                    entrada.ficha?.translation.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        OutlinedTextField(
            value = estado.resposta,
            onValueChange = vm::editarResposta,
            enabled = estado.feedback == null,
            placeholder = { Text("Escreva o termo original") },
            singleLine = false,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { teclado?.hide(); vm.confirmar() }),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )

        // O veredito se abre empurrando o botão para baixo, em vez de aparecer
        // pronto no lugar dele. É o único instante da revisão em que a pessoa
        // descobriu alguma coisa, e 240 ms de abertura é o que separa "a tela
        // respondeu" de "a tela já estava assim".
        AnimatedVisibility(
            visible = estado.feedback != null,
            enter = expandVertically(tween(Movimento.PADRAO)) + fadeIn(tween(Movimento.PADRAO)),
            exit = shrinkVertically(tween(Movimento.RAPIDO)) + fadeOut(tween(Movimento.RAPIDO)),
        ) {
            // Ao vivo enquanto o feedback existe, pela cópia enquanto ele sai —
            // assim a caixa nunca fica um quadro sem conteúdo na entrada nem
            // mostra o cartão errado na saída.
            val veredito = estado.feedback?.let { it to estado.entrada.alvo.orEmpty() } ?: ultimoVeredito
            if (veredito != null) {
                val (feedback, resposta) = veredito
                val acertou = feedback == FeedbackRevisao.CORRETA
                Surface(
                    color = if (acertou) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer,
                    contentColor = if (acertou) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            when {
                                acertou -> "Boa — você lembrou"
                                feedback == FeedbackRevisao.NAO_LEMBRO -> "Tudo bem. A resposta é:"
                                else -> "Quase. A resposta é:"
                            },
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(resposta, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }

        if (estado.feedback == null) {
            BotaoPrincipal("Verificar", { teclado?.hide(); vm.confirmar() }, habilitado = estado.resposta.isNotBlank())
            AcaoSecundaria("Não lembro", { teclado?.hide(); vm.naoLembro() })
        } else {
            BotaoPrincipal("Continuar", vm::avancar)
        }
        Spacer(Modifier.navigationBarsPadding().height(20.dp))
    }
}

/** A lacuna vira um traço de ameixa: no handoff ela é um sublinhado, não underscores. */
@Composable
private fun clozeAnotado(trecho: String) = buildAnnotatedString {
    val corte = trecho.indexOf(LACUNA)
    if (corte < 0) {
        append(trecho)
        return@buildAnnotatedString
    }
    append(trecho.substring(0, corte))
    pushStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline))
    append(LACUNA)
    pop()
    append(trecho.substring(corte + LACUNA.length))
}

@Composable
private fun Vazia(aoVoltar: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EstadoVazio(
            icone = Icones.Check,
            titulo = "Memória em dia",
            detalhe = "Volte quando alguma ficha pedir reforço.",
            acao = { BotaoPrincipal("Voltar", aoVoltar, Modifier.padding(horizontal = 40.dp)) },
        )
    }
}

@Composable
private fun Resumo(estado: RevisaoEstado.Resumo, vm: RevisaoViewModel, aoVoltar: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        Text(
            text = "Sessão concluída",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.entradaSuave().padding(top = 48.dp),
        )
        // Acertos e sequência contam do zero; "para reforçar" não. Os dois
        // primeiros são o que a sessão rendeu, e vê-los subir é a recompensa da
        // tela inteira — o terceiro é o que ficou faltando, e uma contagem
        // crescente ali transformaria o erro em placar.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
            CartaoMetrica("${contagemAnimada(estado.acertos, "acertos")}", "acertos", Modifier.weight(1f), destaque = true)
            CartaoMetrica("${estado.erros}", "para reforçar", Modifier.weight(1f))
            CartaoMetrica(
                valor = "${contagemAnimada(estado.diasSeguidos, "diasSeguidos")}",
                rotulo = "dias seguidos",
                modifier = Modifier.weight(1f),
                destaque = true,
            )
        }
        if (estado.errados.isNotEmpty()) {
            Text(
                "Voltam para a fila: ${estado.errados.distinct().joinToString()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.entradaSuave(indice = 2).padding(top = 20.dp),
            )
        }
        BotaoPrincipal("Concluir", aoVoltar, Modifier.entradaSuave(indice = 3).padding(top = 28.dp))
        if (estado.restantes > 0) AcaoSecundaria("Mais uma rodada (${estado.restantes})", vm::novaRodada)
        Spacer(Modifier.navigationBarsPadding().height(20.dp))
    }
}
