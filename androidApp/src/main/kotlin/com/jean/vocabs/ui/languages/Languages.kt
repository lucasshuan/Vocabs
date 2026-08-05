package com.jean.vocabs.ui.languages

import androidx.annotation.DrawableRes
import com.jean.vocabs.R
import com.jean.vocabs.contracts.Language
import com.jean.vocabs.contracts.Languages
import com.jean.vocabs.ui.displayName

/**
 * Each flag's drawing.
 *
 * The flags are the **circle-flags** collection (MIT), converted from SVG to
 * VectorDrawable without redrawing anything: hand-made drawings with the right
 * proportions and coats of arms. Approximating them in `Canvas` does not scale to
 * 43 languages — Brazil's flag is not a diamond with a circle.
 *
 * Emoji does not serve either: the regional indicator depends on the system font
 * and becomes a rectangle or two letters on many devices.
 *
 * The map is explicit on purpose. `getIdentifier()` would resolve the name at
 * runtime in one line, and is exactly the kind of reference R8 cannot see: all 43
 * flags would be stripped from the APK and only the screen would show the damage.
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
 * The language for a code, falling back to English.
 *
 * The interface needs **something** to draw; returning null here would spread a
 * `?:` across every row of every screen. Whoever must know the code is unknown —
 * the server, at generation time — uses [Languages.of], which returns null.
 */
fun languageOf(code: String?): Language = Languages.of(code) ?: Languages.ENGLISH

/** The search filter of the "New language" screen: ignores accents and case. */
fun List<Language>.search(term: String): List<Language> {
    val wanted = term.trim().withoutAccents()
    if (wanted.isEmpty()) return this
    return filter { it.displayName.withoutAccents().contains(wanted) || it.code.withoutAccents().contains(wanted) }
}

/**
 * Accent-free and lower case. Someone typing "japones" in a hurry wants to find
 * "Japonês", and a filter demanding the circumflex returns an empty list.
 */
private fun String.withoutAccents(): String = lowercase()
    .replace(ACCENTED) { found -> WITHOUT_ACCENT[ACCENTED_TEXT.indexOf(found.value)].toString() }

private const val ACCENTED_TEXT = "áàâãäéèêëíìîïóòôõöúùûüçñ"
private const val WITHOUT_ACCENT = "aaaaaeeeeiiiiooooouuuucn"
private val ACCENTED = Regex("[$ACCENTED_TEXT]")
