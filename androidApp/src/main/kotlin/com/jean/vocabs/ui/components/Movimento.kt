package com.jean.vocabs.ui.components

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * O vocabulário de movimento do app.
 *
 * Existe pelo mesmo motivo que `Base.kt` existe para as formas: o mesmo gesto
 * aparecia com duração diferente em cada tela — o chip trocava de cor em 180 ms,
 * a barra de memória em 500, o anel do Início não animava nada — e a soma disso é
 * uma interface que parece feita por três pessoas. Aqui há **três** durações e
 * duas molas, e toda animação do app sai de uma delas.
 *
 * A regra que governa os números: nada do que a pessoa **espera** passa de
 * [PADRAO]. Só o que ela já pode ler enquanto acontece — o anel se preenchendo, o
 * número subindo — chega a [AMPLO], e mesmo esses são interrompíveis, porque
 * `animate*AsState` persegue o alvo novo de onde estiver em vez de terminar o
 * trajeto antigo. Nenhuma tela espera uma animação acabar para responder a um
 * toque.
 */
object Movimento {
    /** Reação imediata: cor de um chip, opacidade de um rótulo que troca. */
    const val RAPIDO = 150

    /** O padrão: entrar, sair, girar, trocar de estado. */
    const val PADRAO = 240

    /**
     * Só para o que se lê enquanto corre: o arco do anel, a barra da quota, a
     * contagem de um número grande. Nunca para o que bloqueia um toque.
     */
    const val AMPLO = 620

    /**
     * O intervalo entre um item e o seguinte numa entrada escalonada.
     *
     * Curto de propósito. O escalonamento serve para dar direção à leitura, não
     * para encenar a chegada da tela: com 34 ms e o teto de [ITENS_ESCALONADOS],
     * o último item sai do lugar 170 ms depois do primeiro — abaixo do que se
     * percebe como demora, acima do que se percebe como nada.
     */
    const val PASSO_ESCALONADO = 34L

    /**
     * A partir daqui todo item entra junto.
     *
     * Sem este teto, o décimo cartão de uma lista esperaria meio segundo para
     * aparecer, e o vigésimo mais de um — o escalonamento viraria justamente o
     * gargalo que ele deveria evitar.
     */
    const val ITENS_ESCALONADOS = 5

    /** A mola de toda mudança de forma: sem oscilação visível, com peso. */
    fun <T> mola() = spring<T>(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)

    /** A mola do que comemora — o tique de "Guardado", o selo que aparece. */
    fun <T> molaElastica() = spring<T>(dampingRatio = 0.52f, stiffness = Spring.StiffnessMediumLow)

    /**
     * A mola de quem sai do lugar sob o dedo — os alvos do leque de captura.
     *
     * Mais dura que [mola] porque a distância é grande e o gesto está em curso:
     * um alvo que leva 300 ms para chegar ao lugar é um alvo que ainda está
     * viajando quando o dedo já decidiu para onde vai.
     */
    fun <T> molaDeGesto() = spring<T>(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium)
}

/**
 * Se o aparelho está com as animações desligadas.
 *
 * "Reduzir movimento" no Android é a escala de duração dos animadores em zero,
 * nas opções de acessibilidade e nas de desenvolvedor. Quem liga isso não quer o
 * halo do botão de captura respirando no canto da tela para sempre — e a regra
 * do handoff é que, nesse caso, o botão simplesmente fica parado.
 *
 * Lido uma vez por composição e não observado: a chave vive em Configurações do
 * sistema, e voltar para o app recompõe tudo de qualquer jeito.
 */
@Composable
fun movimentoReduzido(): Boolean {
    val contexto = LocalContext.current
    return remember(contexto) {
        Settings.Global.getFloat(
            contexto.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}

/**
 * Encolhe o que está sendo tocado.
 *
 * O ripple do Material diz *onde* o dedo tocou; ele não diz que o alvo é um
 * objeto. Um cartão que cede 3% sob o dedo e volta ao soltar dá aos cartões deste
 * app a única coisa que faltava para parecerem coisas em vez de retângulos
 * pintados — e custa nada, porque `graphicsLayer` aplica a escala na fase de
 * desenho, sem remedir nem reposicionar ninguém.
 *
 * Recebe a mesma [fonte] passada ao `Surface`/`clickable`: quem desenha o ripple
 * e quem encolhe precisam reagir ao mesmo toque, senão o cartão volta ao tamanho
 * antes de o ripple sumir.
 */
@Composable
fun Modifier.encolheAoTocar(
    fonte: MutableInteractionSource,
    minimo: Float = 0.97f,
): Modifier {
    val pressionado by fonte.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (pressionado) minimo else 1f,
        animationSpec = Movimento.mola(),
        label = "escalaDeToque",
    )
    return graphicsLayer {
        scaleX = escala
        scaleY = escala
    }
}

/** A fonte de interação que [encolheAoTocar] e o ripple do `Surface` compartilham. */
@Composable
fun lembrarToque(): MutableInteractionSource = remember { MutableInteractionSource() }

/**
 * A entrada de um elemento na tela: sobe um pouco e aparece.
 *
 * [indice] escalona a chegada dentro de uma mesma lista — o primeiro parte na
 * hora, o segundo 34 ms depois, e a partir do sexto todos partem juntos.
 *
 * **Não use em `LazyColumn`/`LazyRow`.** Lá o item é composto de novo toda vez
 * que entra no viewport, e a animação recomeçaria a cada rolagem: a lista
 * pareceria estar carregando o tempo todo. Nessas listas o certo é
 * `Modifier.animateItem()`, que anima só o que de fato entrou, saiu ou mudou de
 * lugar — e é o que Vocabulários e Pendentes já usam.
 */
@Composable
fun Modifier.entradaSuave(
    indice: Int = 0,
    deslocamento: Dp = 12.dp,
): Modifier {
    var chegou by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val espera = indice.coerceIn(0, Movimento.ITENS_ESCALONADOS) * Movimento.PASSO_ESCALONADO
        if (espera > 0) delay(espera)
        chegou = true
    }
    val avanco by animateFloatAsState(
        targetValue = if (chegou) 1f else 0f,
        animationSpec = tween(Movimento.PADRAO, easing = FastOutSlowInEasing),
        label = "entradaSuave",
    )
    val alturaEmPx = with(LocalDensity.current) { deslocamento.toPx() }
    return graphicsLayer {
        alpha = avanco
        translationY = (1f - avanco) * alturaEmPx
    }
}

/**
 * Uma fração que parte do zero na primeira vez e persegue o alvo depois.
 *
 * É o que transforma o anel do Início e as barras de progresso de um desenho
 * estático num gesto: a tela abre e o arco se preenche até onde a memória chegou.
 * Passado o primeiro quadro ela vira simplesmente uma fração animada — uma
 * revisão respondida empurra a barra do valor atual para o novo, sem voltar ao
 * zero para recomeçar.
 *
 * Devolve o `State` e não o `Float` de propósito: quem desenha num `Canvas` ou
 * num `drawBehind` lê `.value` **dentro** da lambda de desenho, e aí só a fase de
 * desenho é invalidada a cada quadro. Devolver o número puro obrigaria o
 * composable inteiro a recompor 60 vezes por segundo para animar um arco.
 */
@Composable
fun fracaoAnimada(target: Float, rotulo: String = "fracao"): State<Float> {
    var partiu by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { partiu = true }
    return animateFloatAsState(
        targetValue = if (partiu) target.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(Movimento.AMPLO, easing = FastOutSlowInEasing),
        label = rotulo,
    )
}

/**
 * O mesmo para um número que se lê: ele conta do zero até o valor.
 *
 * Vale para o que é conquista acumulada — quantas palavras são suas, quantos
 * acertos a sessão teve, a força média do curso. **Não** vale para o que é fila
 * ou dívida: ver "12 na fila" contar de zero a doze premiaria o atraso.
 */
@Composable
fun contagemAnimada(target: Int, rotulo: String = "contagem"): Int {
    var partiu by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { partiu = true }
    val value by animateIntAsState(
        targetValue = if (partiu) target else 0,
        animationSpec = tween(Movimento.AMPLO, easing = FastOutSlowInEasing),
        label = rotulo,
    )
    return value
}

/**
 * O respiro de quem está esperando por algo de fora — a ficha sendo montada, a
 * transcrição correndo.
 *
 * Sempre condicionado ao estado que o justifica: quando [ativo] é falso o
 * modifier não instala transição nenhuma. Uma animação infinita que continuasse
 * depois de o trabalho terminar manteria o Compose recompondo a tela para
 * sempre, que é o jeito mais fácil de gastar bateria parado.
 */
@Composable
fun Modifier.respirando(ativo: Boolean, minimo: Float = 0.45f): Modifier {
    if (!ativo) return this
    val transicao = rememberInfiniteTransition(label = "respiro")
    val opacidade by transicao.animateFloat(
        initialValue = 1f,
        targetValue = minimo,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "opacidadeDoRespiro",
    )
    return graphicsLayer { alpha = opacidade }
}
