package com.jean.vocabs.ui.captura

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.jean.vocabs.shared.domain.FormatoCaptura

/**
 * Onde o dedo está, do ponto de vista do gesto de captura.
 *
 * As três posições são exaustivas e é isso que faz o gesto ser reversível: não
 * existe um estado "quase escolheu" nem um alvo que continua marcado depois que
 * o dedo saiu dele. Soltar sempre executa exatamente o que a tela está mostrando
 * no instante em que o dedo sobe — e **só** soltar executa: entrar no alvo pinta,
 * não dispara.
 */
sealed interface AlvoDoGesto {

    /** Em cima do `+`, de onde o gesto saiu. */
    data object Origem : AlvoDoGesto

    /** Nem origem nem alvo. Soltar aqui não faz nada. */
    data object Fora : AlvoDoGesto

    data class Modo(val formato: FormatoCaptura) : AlvoDoGesto
}

/**
 * A posição dos três alvos em volta do `+`, em dp e a partir do centro dele.
 *
 * Um arco de verdade: os três estão à **mesma distância** do `+` — 152 dp —, o
 * áudio no eixo por ser o alvo mais provável e os outros dois a ±54°. Nenhum dos
 * três custa mais dedo que o outro, e é literalmente um arco: o polegar varre a
 * mesma curva sem esticar nem recolher.
 *
 * A abertura é larga de propósito. Com os laterais mais altos e mais para dentro
 * os três discos se acotovelavam, e a terra de ninguém entre um e outro — o que
 * garante que atravessar o leque não marque nada por acidente — ficava estreita
 * demais para ser sentida. Descê-los e jogá-los para as bordas separa os vizinhos
 * em ~137 dp de centro a centro, quase 50 dp de folga entre as áreas de toque.
 */
val DESLOCAMENTO_DO_AUDIO = DpOffset(0.dp, (-152).dp)
val DESLOCAMENTO_DO_TEXTO = DpOffset((-122).dp, (-90).dp)
val DESLOCAMENTO_DA_FOTO = DpOffset(122.dp, (-90).dp)

/**
 * Os três alvos nascem iguais.
 *
 * Nenhum é maior nem preenchido antes de ser alcançado: o áudio deixou de ser o
 * disco grande e verde porque um alvo já pintado antes do dedo chegar promete que
 * alguma coisa está em curso — e nada está. [DIAMETRO_DO_ALVO_MARCADO] é o
 * tamanho do que está sob o dedo, e é o único sinal de escolha que existe.
 */
val DIAMETRO_DO_ALVO = 68.dp
val DIAMETRO_DO_ALVO_MARCADO = 76.dp

/**
 * O quanto o dedo pode passar perto de um alvo e ainda contar como estando nele.
 *
 * 44 dp de raio dão uma área de toque de 88 dp — bem acima do mínimo de 48 dp do
 * Material e maior que o próprio disco de 68 dp, para o alvo ser mais fácil de
 * acertar do que de ver. Entre dois alvos vizinhos sobram ~27 dp de terra de
 * ninguém, e é ali que o gesto não escolhe nada: um alvo que se marcasse por
 * proximidade relativa marcaria alguma coisa em toda a metade de cima da tela.
 */
val RAIO_DO_ALVO = 44.dp

/**
 * O raio em volta do `+` onde o gesto ainda não escolheu nada.
 *
 * É a zona de "só toquei": soltar aqui abre o texto, que é o que quem encostou no
 * botão devagar estava pedindo.
 */
val RAIO_DE_ORIGEM = 56.dp

/** Depois disto o toque virou pressão e o leque abre mesmo sem o dedo ter andado. */
const val ABERTURA_DO_LEQUE_MS = 180L

/**
 * O realce do alvo persegue o dedo, e por isso é mais curto que qualquer duração
 * do vocabulário de movimento do app.
 *
 * `Movimento.RAPIDO` (150 ms) é a reação de um chip que troca de cor depois de um
 * toque que já terminou. Aqui o dedo ainda está andando: 90 ms é o teto para o
 * realce chegar antes de a mão duvidar se aquele alvo é mesmo o que está marcado.
 */
const val REALCE_DO_ALVO_MS = 90

/** O deslocamento de cada modo, para quem desenha os alvos. */
fun deslocamentoDe(formato: FormatoCaptura): DpOffset = when (formato) {
    FormatoCaptura.TEXTO -> DESLOCAMENTO_DO_TEXTO
    FormatoCaptura.AUDIO -> DESLOCAMENTO_DO_AUDIO
    FormatoCaptura.FOTO -> DESLOCAMENTO_DA_FOTO
}

/** Os mesmos três deslocamentos em pixels, prontos para comparar com o dedo. */
fun Density.alvosEmPixels(): List<Pair<FormatoCaptura, Offset>> =
    FormatoCaptura.entries.map { formato ->
        val destino = deslocamentoDe(formato)
        formato to Offset(destino.x.toPx(), destino.y.toPx())
    }

/**
 * Que alvo um deslocamento a partir do `+` escolhe.
 *
 * **É preciso alcançar o alvo.** A versão anterior escolhia por ângulo — apontar
 * bastava, e o disco desenhado era só a ilustração de um setor — o que economizava
 * dedo e cobrava o preço em outro lugar: com o alvo do áudio ocupando todo o setor
 * central, um deslize curto para cima já marcava "gravar", e o que era para ser
 * ponteiro virava gatilho. Agora o alvo é uma coisa no lugar dela: o dedo chega
 * nele, ele se pinta, e soltar ali executa. Os três estão a ~150 dp, dentro do
 * arco do polegar, e o raio de 44 dp perdoa a mira.
 *
 * Todas as medidas chegam em pixels porque quem chama está dentro de um
 * `PointerInputScope`, onde as posições são pixels e a densidade está à mão.
 */
fun alvoPara(
    desloc: Offset,
    alvos: List<Pair<FormatoCaptura, Offset>>,
    raioDoAlvoPx: Float,
    raioDeOrigemPx: Float,
): AlvoDoGesto {
    val maisProximo = alvos.minByOrNull { (_, centro) -> (desloc - centro).getDistanceSquared() }
    if (maisProximo != null && (desloc - maisProximo.second).getDistance() <= raioDoAlvoPx) {
        return AlvoDoGesto.Modo(maisProximo.first)
    }
    return if (desloc.getDistance() < raioDeOrigemPx) AlvoDoGesto.Origem else AlvoDoGesto.Fora
}

