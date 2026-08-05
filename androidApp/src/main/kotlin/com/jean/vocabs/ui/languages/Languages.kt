package com.jean.vocabs.ui.languages

import androidx.annotation.DrawableRes
import com.jean.vocabs.R
import com.jean.vocabs.contracts.Language
import com.jean.vocabs.contracts.Languages
import com.jean.vocabs.ui.displayName

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
fun flagOf(language: Language): Int = when (language.country) {
    "bd" -> R.drawable.flag_bd
    "bg" -> R.drawable.flag_bg
    "br" -> R.drawable.flag_br
    "cn" -> R.drawable.flag_cn
    "cz" -> R.drawable.flag_cz
    "de" -> R.drawable.flag_de
    "dk" -> R.drawable.flag_dk
    "ee" -> R.drawable.flag_ee
    "es" -> R.drawable.flag_es
    "es-ct" -> R.drawable.flag_es_ct
    "fi" -> R.drawable.flag_fi
    "fr" -> R.drawable.flag_fr
    "gr" -> R.drawable.flag_gr
    "hr" -> R.drawable.flag_hr
    "hu" -> R.drawable.flag_hu
    "id" -> R.drawable.flag_id
    "il" -> R.drawable.flag_il
    "in" -> R.drawable.flag_in
    "ir" -> R.drawable.flag_ir
    "is" -> R.drawable.flag_is
    "it" -> R.drawable.flag_it
    "jp" -> R.drawable.flag_jp
    "ke" -> R.drawable.flag_ke
    "kr" -> R.drawable.flag_kr
    "lt" -> R.drawable.flag_lt
    "lv" -> R.drawable.flag_lv
    "my" -> R.drawable.flag_my
    "nl" -> R.drawable.flag_nl
    "no" -> R.drawable.flag_no
    "ph" -> R.drawable.flag_ph
    "pl" -> R.drawable.flag_pl
    "pt" -> R.drawable.flag_pt
    "ro" -> R.drawable.flag_ro
    "rs" -> R.drawable.flag_rs
    "ru" -> R.drawable.flag_ru
    "sa" -> R.drawable.flag_sa
    "se" -> R.drawable.flag_se
    "sk" -> R.drawable.flag_sk
    "th" -> R.drawable.flag_th
    "tr" -> R.drawable.flag_tr
    "ua" -> R.drawable.flag_ua
    "us" -> R.drawable.flag_us
    "vn" -> R.drawable.flag_vn
    else -> R.drawable.flag_us
}

/**
 * O idioma de um código, com o inglês como último recurso.
 *
 * A interface precisa de **alguma** coisa para desenhar; devolver nulo aqui
 * espalharia um `?:` por cada linha de cada tela. Quem precisa saber que o
 * código é desconhecido — o servidor, na hora de gerar — usa [Languages.de], que
 * devolve nulo e recusa.
 */
fun languageOf(codigo: String?): Language = Languages.de(codigo) ?: Languages.INGLES

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
