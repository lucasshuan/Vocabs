package com.jean.vocabs.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jean.vocabs.shared.domain.CaptureFormat
import com.jean.vocabs.ui.theme.LocalTemaEscuro
import com.jean.vocabs.ui.theme.VocabuColors

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
data class CategoryColors(val cor: Color, val fundo: Color)

@Composable
fun formatColors(format: CaptureFormat): CategoryColors {
    val cores = MaterialTheme.colorScheme
    val escuro = LocalTemaEscuro.current
    return when (format) {
        CaptureFormat.TEXT -> CategoryColors(cores.primary, cores.secondaryContainer)
        CaptureFormat.AUDIO -> CategoryColors(cores.tertiary, cores.tertiaryContainer)
        CaptureFormat.PHOTO -> if (escuro) {
            CategoryColors(VocabuColors.PapagaioEscuro, VocabuColors.PapagaioContainerEscuro)
        } else {
            CategoryColors(VocabuColors.Papagaio, VocabuColors.PapagaioContainer)
        }
    }
}

fun formatIcon(format: CaptureFormat): ImageVector = when (format) {
    CaptureFormat.TEXT -> AppIcons.Lapis
    CaptureFormat.AUDIO -> AppIcons.Microfone
    CaptureFormat.PHOTO -> AppIcons.Camera
}

/**
 * O nome de cada formato. Não há enum próprio para as abas: elas **são** os
 * formatos, e um segundo enum com os mesmos três nomes só criaria a chance de um
 * ganhar um caso a mais que o outro não tem.
 */
fun formatLabel(format: CaptureFormat): String = when (format) {
    CaptureFormat.TEXT -> "Texto"
    CaptureFormat.AUDIO -> "Áudio"
    CaptureFormat.PHOTO -> "Foto"
}

/** O disco da categoria, já com as cores certas — o começo de toda linha de Pendentes. */
@Composable
fun CategoryDisc(format: CaptureFormat, tamanho: Dp = 38.dp) {
    val paleta = formatColors(format)
    IconDisc(
        icon = formatIcon(format),
        descricao = formatLabel(format),
        cor = paleta.cor,
        fundo = paleta.fundo,
        tamanho = tamanho,
    )
}
