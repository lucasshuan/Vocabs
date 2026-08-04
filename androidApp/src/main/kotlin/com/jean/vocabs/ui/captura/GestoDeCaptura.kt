package com.jean.vocabs.ui.captura

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.jean.vocabs.shared.domain.FormatoCaptura
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Onde o dedo está, do ponto de vista do gesto de captura.
 *
 * As três posições são exaustivas e é isso que faz o gesto ser reversível: não
 * existe um estado "quase escolheu" nem um alvo que continua marcado depois que
 * o dedo saiu dele. Soltar sempre executa exatamente o que a tela está mostrando
 * no instante em que o dedo sobe.
 */
sealed interface AlvoDoGesto {

    /** Em cima do `+`, de onde o gesto saiu — soltar aqui desfaz. */
    data object Origem : AlvoDoGesto

    /** Fora do leque: nem origem nem alvo. Soltar aqui não faz nada. */
    data object Fora : AlvoDoGesto

    data class Modo(val formato: FormatoCaptura) : AlvoDoGesto
}

/**
 * A posição dos três alvos em volta do `+`, em dp e a partir do centro dele.
 *
 * O áudio fica **no eixo**, e não numa das laterais, por ser o alvo mais provável
 * de longe: é o modo que existe para o momento em que a frase está passando. Os
 * outros dois ficam a ±100 dp, dentro do arco que o polegar cobre sem o aparelho
 * sair da mão.
 */
val DESLOCAMENTO_DO_AUDIO = DpOffset(0.dp, (-166).dp)
val DESLOCAMENTO_DO_TEXTO = DpOffset((-98).dp, (-118).dp)
val DESLOCAMENTO_DA_FOTO = DpOffset(98.dp, (-118).dp)

/** O diâmetro de cada alvo. O do meio é maior porque é o que se acerta sem olhar. */
val DIAMETRO_DO_ALVO_CENTRAL = 78.dp
val DIAMETRO_DO_ALVO_LATERAL = 62.dp

/**
 * O raio em volta do `+` onde o gesto ainda não escolheu nada.
 *
 * É a zona de cancelamento e a de "só toquei": abaixo dela nenhum alvo está
 * marcado, em cima ou fora dela a escolha já vale.
 */
val RAIO_DE_ORIGEM = 56.dp

/** Além disto o dedo saiu do leque — arrastar para o outro canto da tela não escolhe nada. */
val ALCANCE_DO_LEQUE = 320.dp

/** O leque só abre para cima: fora deste arco, nenhum alvo. */
private const val ABERTURA_EM_GRAUS = 62f

/**
 * A fronteira entre o setor do áudio e os laterais, na metade do caminho entre
 * os ângulos em que os três alvos são desenhados (0° e ±40°).
 */
private const val FRONTEIRA_EM_GRAUS = 20f

/** Depois disto o toque virou pressão e o leque abre mesmo sem o dedo ter andado. */
const val ABERTURA_DO_LEQUE_MS = 180L

/**
 * Que alvo um deslocamento a partir do `+` escolhe.
 *
 * **A escolha é por ângulo, não por acerto do círculo desenhado.** Um menu radial
 * em que é preciso alcançar fisicamente um disco a 160 dp de distância obriga a
 * mão a se reposicionar no meio do gesto, e é assim que um atalho de um gesto só
 * vira dois. Aqui basta apontar: passou do raio de origem, o setor em que o dedo
 * está já marca o alvo, e o disco correspondente cresce para confirmar. Quem
 * quiser percorrer os 160 dp inteiros também acerta — o alvo desenhado está
 * dentro do próprio setor.
 *
 * Todas as medidas chegam em pixels porque quem chama está dentro de um
 * `PointerInputScope`, onde as posições são pixels e a densidade está à mão.
 */
fun alvoPara(
    desloc: Offset,
    raioDeOrigemPx: Float,
    alcancePx: Float,
): AlvoDoGesto {
    val distancia = desloc.getDistance()
    if (distancia < raioDeOrigemPx) return AlvoDoGesto.Origem
    if (distancia > alcancePx) return AlvoDoGesto.Fora

    // 0° é para cima; negativo é para a esquerda. O eixo Y da tela cresce para
    // baixo, daí o sinal invertido.
    val graus = Math.toDegrees(atan2(desloc.x.toDouble(), -desloc.y.toDouble())).toFloat()
    if (abs(graus) > ABERTURA_EM_GRAUS) return AlvoDoGesto.Fora

    return AlvoDoGesto.Modo(
        when {
            graus < -FRONTEIRA_EM_GRAUS -> FormatoCaptura.TEXTO
            graus > FRONTEIRA_EM_GRAUS -> FormatoCaptura.FOTO
            else -> FormatoCaptura.AUDIO
        },
    )
}

/**
 * Durante a gravação o leque some e só restam duas respostas.
 *
 * Enquanto grava, a tela mostra um alvo de áudio e um `+` virado em `×`, e mais
 * nada — trocar de modo no meio de uma gravação não é uma coisa que alguém
 * queira fazer, e oferecer isso só criaria a chance de perder o áudio por um
 * desvio de dedo. A pergunta vira binária: voltou para a origem, descarta;
 * qualquer outro lugar, guarda.
 */
fun voltouParaOrigem(desloc: Offset, raioDeOrigemPx: Float): Boolean =
    desloc.getDistance() < raioDeOrigemPx

/** O deslocamento de cada modo, para quem desenha os alvos. */
fun deslocamentoDe(formato: FormatoCaptura): DpOffset = when (formato) {
    FormatoCaptura.TEXTO -> DESLOCAMENTO_DO_TEXTO
    FormatoCaptura.AUDIO -> DESLOCAMENTO_DO_AUDIO
    FormatoCaptura.FOTO -> DESLOCAMENTO_DA_FOTO
}
