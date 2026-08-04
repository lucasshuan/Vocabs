package com.jean.vocabs.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jean.vocabs.shared.domain.FormatoCaptura
import com.jean.vocabs.ui.theme.LocalTemaEscuro
import com.jean.vocabs.ui.theme.TagararaColors

/**
 * Uma cor por tipo de captura, e a mesma tripla em todos os lugares.
 *
 * Texto é ameixa, áudio é menta, foto é o vermelho do papagaio. A associação se
 * forma na folha do `+`, onde os três aparecem lado a lado, e se cobra em
 * Pendentes, onde o disco à esquerda de cada linha é a única coisa que diz de
 * onde aquela captura veio. Ela só funciona se as duas telas usarem a mesma
 * fonte — daí este arquivo existir em vez de um `when` por tela.
 *
 * O vermelho **não** é `error`: uma foto na fila não é uma foto com problema.
 */
data class CoresDeCategoria(val cor: Color, val fundo: Color)

@Composable
fun coresDoFormato(formato: FormatoCaptura): CoresDeCategoria {
    val cores = MaterialTheme.colorScheme
    val escuro = LocalTemaEscuro.current
    return when (formato) {
        FormatoCaptura.TEXTO -> CoresDeCategoria(cores.primary, cores.secondaryContainer)
        FormatoCaptura.AUDIO -> CoresDeCategoria(cores.tertiary, cores.tertiaryContainer)
        FormatoCaptura.FOTO -> if (escuro) {
            CoresDeCategoria(TagararaColors.PapagaioEscuro, TagararaColors.PapagaioContainerEscuro)
        } else {
            CoresDeCategoria(TagararaColors.Papagaio, TagararaColors.PapagaioContainer)
        }
    }
}

/**
 * O vermelho de descartar — o mesmo do papagaio, e é de propósito.
 *
 * A regra do app é que este vermelho é categoria e nunca erro nem ação, para que
 * uma foto na fila não pareça uma foto com problema. O gesto de captura abre uma
 * exceção estreita e a paga: o `+` só fica vermelho **durante a gravação**, e
 * nesse estado o alvo da foto já recolheu — os dois sentidos nunca dividem a
 * tela. Puxar o `error` do tema para cá criaria um segundo vermelho quase igual
 * ao primeiro, que é o jeito garantido de tornar os dois ilegíveis.
 */
@Composable
fun corDeDescarte(): Color =
    if (LocalTemaEscuro.current) TagararaColors.PapagaioEscuro else TagararaColors.Papagaio

fun iconeDoFormato(formato: FormatoCaptura): ImageVector = when (formato) {
    FormatoCaptura.TEXTO -> Icones.Lapis
    FormatoCaptura.AUDIO -> Icones.Microfone
    FormatoCaptura.FOTO -> Icones.Camera
}

/**
 * O nome de cada formato. Não há enum próprio para as abas: elas **são** os
 * formatos, e um segundo enum com os mesmos três nomes só criaria a chance de um
 * ganhar um caso a mais que o outro não tem.
 */
fun rotuloDoFormato(formato: FormatoCaptura): String = when (formato) {
    FormatoCaptura.TEXTO -> "Texto"
    FormatoCaptura.AUDIO -> "Áudio"
    FormatoCaptura.FOTO -> "Foto"
}

/** O disco da categoria, já com as cores certas — o começo de toda linha de Pendentes. */
@Composable
fun DiscoDeCategoria(formato: FormatoCaptura, tamanho: Dp = 38.dp) {
    val paleta = coresDoFormato(formato)
    DiscoDeIcone(
        icone = iconeDoFormato(formato),
        descricao = rotuloDoFormato(formato),
        cor = paleta.cor,
        fundo = paleta.fundo,
        tamanho = tamanho,
    )
}
