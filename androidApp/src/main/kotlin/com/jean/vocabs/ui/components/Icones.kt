package com.jean.vocabs.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Ícones desenhados por path data (viewport 24x24), no traço do Material.
 * Evita puxar a dependência material-icons inteira por meia dúzia de glifos.
 */
private fun icone(nome: String, dados: String): ImageVector =
    ImageVector.Builder(
        name = nome,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(dados),
        fill = SolidColor(Color.Black),
    ).build()

object Icones {

    val Lupa: ImageVector by lazy {
        icone("lupa", "M9.5,3a6.5,6.5 0,1 0,3.98,11.64L19.85,21 21,19.85l-6.36,-6.37A6.5,6.5 0,0 0,9.5,3zM5,9.5a4.5,4.5 0,1 1,9,0 4.5,4.5 0,0 1,-9,0z")
    }

    val MaisVertical: ImageVector by lazy {
        icone("mais_vertical", "M12,8c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2zM12,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2zM12,16c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2z")
    }

    val Voltar: ImageVector by lazy {
        icone("voltar", "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z")
    }

    val Mais: ImageVector by lazy {
        icone("mais", "M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z")
    }

    val Som: ImageVector by lazy {
        icone(
            "som",
            "M3,9v6h4l5,5V4L7,9H3zM16.5,12c0,-1.77 -1.02,-3.29 -2.5,-4.03v8.05c1.48,-0.73 2.5,-2.25 2.5,-4.02zM14,3.23v2.06c2.89,0.86 5,3.54 5,6.71s-2.11,5.85 -5,6.71v2.06c4.01,-0.91 7,-4.49 7,-8.77s-2.99,-7.86 -7,-8.77z",
        )
    }

    val Repetir: ImageVector by lazy {
        icone(
            "repetir",
            "M17.65,6.35C16.2,4.9 14.21,4 12,4c-4.42,0 -7.99,3.58 -7.99,8s3.57,8 7.99,8c3.73,0 6.84,-2.55 7.73,-6h-2.08c-0.82,2.33 -3.04,4 -5.65,4 -3.31,0 -6,-2.69 -6,-6s2.69,-6 6,-6c1.66,0 3.14,0.69 4.22,1.78L13,11h7V4l-2.35,2.35z",
        )
    }

    val Compartilhar: ImageVector by lazy {
        icone(
            "compartilhar",
            "M18,16.08c-0.76,0 -1.44,0.3 -1.96,0.77L8.91,12.7c0.05,-0.23 0.09,-0.46 0.09,-0.7s-0.04,-0.47 -0.09,-0.7l7.05,-4.11c0.54,0.5 1.25,0.81 2.04,0.81 1.66,0 3,-1.34 3,-3s-1.34,-3 -3,-3 -3,1.34 -3,3c0,0.24 0.04,0.47 0.09,0.7L8.04,9.81C7.5,9.31 6.79,9 6,9c-1.66,0 -3,1.34 -3,3s1.34,3 3,3c0.79,0 1.5,-0.31 2.04,-0.81l7.12,4.16c-0.05,0.21 -0.08,0.43 -0.08,0.65 0,1.61 1.31,2.92 2.92,2.92 1.61,0 2.92,-1.31 2.92,-2.92s-1.31,-2.92 -2.92,-2.92z",
        )
    }

    val Cartas: ImageVector by lazy {
        icone(
            "cartas",
            "M3,3v8h8V3H3zM9,9H5V5h4V9zM3,13v8h8v-8H3zM9,19H5v-4h4V19zM13,3v8h8V3H13zM19,9h-4V5h4V9zM13,13v8h8v-8H13zM19,19h-4v-4h4V19z",
        )
    }

    val CapturarCirculo: ImageVector by lazy {
        icone(
            "capturar",
            "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20c-4.41,0 -8,-3.59 -8,-8s3.59,-8 8,-8 8,3.59 8,8 -3.59,8 -8,8zM13,7h-2v4H7v2h4v4h2v-4h4v-2h-4V7z",
        )
    }

    val Camera: ImageVector by lazy {
        icone(
            "camera",
            "M9,2L7.17,4H4C2.9,4 2,4.9 2,6v12c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V6c0,-1.1 -0.9,-2 -2,-2h-3.17L15,2H9zM12,17c-2.76,0 -5,-2.24 -5,-5s2.24,-5 5,-5 5,2.24 5,5 -2.24,5 -5,5z",
        )
    }

    val Microfone: ImageVector by lazy {
        icone(
            "microfone",
            "M12,14c1.66,0 3,-1.34 3,-3V5c0,-1.66 -1.34,-3 -3,-3S9,3.34 9,5v6c0,1.66 1.34,3 3,3zM17,11c0,2.76 -2.24,5 -5,5s-5,-2.24 -5,-5H5c0,3.53 2.61,6.43 6,6.92V21h2v-3.08c3.39,-0.49 6,-3.39 6,-6.92h-2z",
        )
    }

    val Parar: ImageVector by lazy {
        icone("parar", "M6,6h12v12H6z")
    }

    val Tocar: ImageVector by lazy {
        icone("tocar", "M8,5v14l11,-7z")
    }

    val Inbox: ImageVector by lazy {
        icone(
            "inbox",
            "M19,3H4.99c-1.11,0 -1.98,0.9 -1.98,2L3,19c0,1.1 0.88,2 1.99,2H19c1.1,0 2,-0.9 2,-2V5c0,-1.1 -0.9,-2 -2,-2zM19,15h-4c0,1.66 -1.35,3 -3,3s-3,-1.34 -3,-3H4.99V5H19v10z",
        )
    }

    val Lixeira: ImageVector by lazy {
        icone(
            "lixeira",
            "M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12zM19,4h-3.5l-1,-1h-5l-1,1H5v2h14V4z",
        )
    }

    val Check: ImageVector by lazy {
        icone("check", "M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z")
    }

    val Pessoa: ImageVector by lazy {
        icone(
            "pessoa",
            "M12,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4 -4,1.79 -4,4 1.79,4 4,4zM12,14c-2.67,0 -8,1.34 -8,4v2h16v-2c0,-2.66 -5.33,-4 -8,-4z",
        )
    }

    /** Palavras puxando palavras — a rede de associações da Fase 3. */
    val Rede: ImageVector by lazy {
        icone(
            "rede",
            "M17,16l-4,-4V8.82C14.16,8.4 15,7.3 15,6c0,-1.66 -1.34,-3 -3,-3S9,4.34 9,6c0,1.3 0.84,2.4 2,2.82V12l-4,4H3v5h5v-3.05l4,-4.2 4,4.2V21h5v-5h-4z",
        )
    }

    val Grafico: ImageVector by lazy {
        icone("grafico", "M5,9.2h3V19H5zM10.6,5h2.8v14h-2.8zm5.6,8H19v6h-2.8z")
    }

    val Casa: ImageVector by lazy {
        icone("casa", "M10,20v-6h4v6h5v-8h3L12,3 2,12h3v8z")
    }

    /** Seta para a direita: "isto abre outra tela". */
    val Avancar: ImageVector by lazy {
        icone("avancar", "M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z")
    }

    /** Seta para baixo: "isto abre uma lista de opções". */
    val Expandir: ImageVector by lazy {
        icone("expandir", "M7.41,8.59L12,13.17l4.59,-4.58L18,10l-6,6 -6,-6z")
    }

    val Lapis: ImageVector by lazy {
        icone(
            "lapis",
            "M3,17.25V21h3.75L17.81,9.94l-3.75,-3.75L3,17.25zM20.71,7.04c0.39,-0.39 0.39,-1.02 0,-1.41l-2.34,-2.34c-0.39,-0.39 -1.02,-0.39 -1.41,0l-1.83,1.83 3.75,3.75 1.83,-1.83z",
        )
    }

    /**
     * Relógio para a aba de Pendentes.
     *
     * A bandeja de "inbox" dizia de onde as capturas vêm; o relógio diz o que
     * elas estão fazendo — esperando você. É o significado que importa na aba.
     */
    val Relogio: ImageVector by lazy {
        icone(
            "relogio",
            "M11.99,2C6.47,2 2,6.48 2,12s4.47,10 9.99,10C17.52,22 22,17.52 22,12S17.52,2 11.99,2zM12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8zM12.5,7H11v6l5.25,3.15 0.75,-1.23 -4.5,-2.67z",
        )
    }

    val Fechar: ImageVector by lazy {
        icone(
            "fechar",
            "M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z",
        )
    }
}
