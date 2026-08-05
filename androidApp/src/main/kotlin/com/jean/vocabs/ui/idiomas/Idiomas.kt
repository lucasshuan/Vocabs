package com.jean.vocabs.ui.idiomas

import com.jean.vocabs.ui.displayName
import androidx.annotation.DrawableRes
import com.jean.vocabs.R
import com.jean.vocabs.contracts.Language
import com.jean.vocabs.contracts.Languages

/**
 * O desenho de cada bandeira.
 *
 * As bandeiras são a coleção **circle-flags** (MIT), convertida de SVG para
 * VectorDrawable sem redesenhar nada: são desenhos feitos à mão, com as
 * proporções e os brasões certos. A versão anterior tinha duas delas
 * aproximadas em `Canvas`, e essa conta não fecha para 43 idiomas — a bandeira
 * do Brasil não é um losango com um círculo, e a da Coreia do Sul não é
 * desenhável de cabeça.
 *
 * Emoji também não serve: o indicador regional depende da fonte do sistema e
 * vira retângulo ou duas letras em vários aparelhos.
 *
 * O mapa é explícito de propósito. `getIdentifier()` resolveria o nome em
 * runtime numa linha, e é exatamente o tipo de referência que o R8 não enxerga:
 * as 43 bandeiras seriam removidas do APK e só a tela mostraria o estrago.
 */
@DrawableRes
fun bandeiraDe(idioma: Language): Int = when (idioma.country) {
    "bd" -> R.drawable.bandeira_bd
    "bg" -> R.drawable.bandeira_bg
    "br" -> R.drawable.bandeira_br
    "cn" -> R.drawable.bandeira_cn
    "cz" -> R.drawable.bandeira_cz
    "de" -> R.drawable.bandeira_de
    "dk" -> R.drawable.bandeira_dk
    "ee" -> R.drawable.bandeira_ee
    "es" -> R.drawable.bandeira_es
    "es-ct" -> R.drawable.bandeira_es_ct
    "fi" -> R.drawable.bandeira_fi
    "fr" -> R.drawable.bandeira_fr
    "gr" -> R.drawable.bandeira_gr
    "hr" -> R.drawable.bandeira_hr
    "hu" -> R.drawable.bandeira_hu
    "id" -> R.drawable.bandeira_id
    "il" -> R.drawable.bandeira_il
    "in" -> R.drawable.bandeira_in
    "ir" -> R.drawable.bandeira_ir
    "is" -> R.drawable.bandeira_is
    "it" -> R.drawable.bandeira_it
    "jp" -> R.drawable.bandeira_jp
    "ke" -> R.drawable.bandeira_ke
    "kr" -> R.drawable.bandeira_kr
    "lt" -> R.drawable.bandeira_lt
    "lv" -> R.drawable.bandeira_lv
    "my" -> R.drawable.bandeira_my
    "nl" -> R.drawable.bandeira_nl
    "no" -> R.drawable.bandeira_no
    "ph" -> R.drawable.bandeira_ph
    "pl" -> R.drawable.bandeira_pl
    "pt" -> R.drawable.bandeira_pt
    "ro" -> R.drawable.bandeira_ro
    "rs" -> R.drawable.bandeira_rs
    "ru" -> R.drawable.bandeira_ru
    "sa" -> R.drawable.bandeira_sa
    "se" -> R.drawable.bandeira_se
    "sk" -> R.drawable.bandeira_sk
    "th" -> R.drawable.bandeira_th
    "tr" -> R.drawable.bandeira_tr
    "ua" -> R.drawable.bandeira_ua
    "us" -> R.drawable.bandeira_us
    "vn" -> R.drawable.bandeira_vn
    else -> R.drawable.bandeira_us
}

/**
 * O idioma de um código, com o inglês como último recurso.
 *
 * A interface precisa de **alguma** coisa para desenhar; devolver nulo aqui
 * espalharia um `?:` por cada linha de cada tela. Quem precisa saber que o
 * código é desconhecido — o servidor, na hora de gerar — usa [Languages.de], que
 * devolve nulo e recusa.
 */
fun idiomaDe(codigo: String?): Language = Languages.de(codigo) ?: Languages.INGLES

/** Filtro da busca da tela "Novo idioma": ignora acento e caixa. */
fun List<Language>.buscar(termo: String): List<Language> {
    val procurado = termo.trim().semAcento()
    if (procurado.isEmpty()) return this
    return filter { it.displayName.semAcento().contains(procurado) || it.code.semAcento().contains(procurado) }
}

/**
 * Sem acento e em caixa baixa. Quem digita "japones" na pressa quer achar
 * "Japonês", e um filtro que exige o circunflexo devolve lista vazia.
 */
private fun String.semAcento(): String = lowercase()
    .replace(ACENTUADAS) { encontro -> SEM_ACENTO[ACENTUADAS_TEXTO.indexOf(encontro.value)].toString() }

private const val ACENTUADAS_TEXTO = "áàâãäéèêëíìîïóòôõöúùûüçñ"
private const val SEM_ACENTO = "aaaaaeeeeiiiiooooouuuucn"
private val ACENTUADAS = Regex("[$ACENTUADAS_TEXTO]")
