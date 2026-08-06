package io.github.lucasshuan.vocabu.ui.languages

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.lucasshuan.vocabu.R
import io.github.lucasshuan.vocabu.contracts.Language
import io.github.lucasshuan.vocabu.contracts.Languages
import java.text.Normalizer
import java.util.Locale

/**
 * The circle-flags collection (MIT), SVG converted to VectorDrawable without
 * redrawing. `Canvas` does not scale to 43 flags — Brazil's is not a diamond
 * with a circle — and emoji regional indicators depend on the system font,
 * landing as a rectangle or two letters on many devices.
 *
 * Explicit, not `getIdentifier()`: a name resolved at runtime is a reference R8
 * cannot see, and all 43 flags would be stripped from the APK.
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
 * Falls back rather than returning null: the interface needs something to draw,
 * and null would spread a `?:` across every row. Whoever has to know a code is
 * unknown — the server, at generation — uses [Languages.of].
 */
fun languageOf(code: String?): Language = Languages.of(code) ?: Languages.ENGLISH

/**
 * A `when`, not a `<string-array>`: an array is index-keyed, and drift against
 * [Languages.CATALOG] would be silent. Regions become underscores — a resource
 * name cannot hold a hyphen.
 */
@StringRes
fun nameResOf(code: String): Int = when (code) {
    "en" -> R.string.language_en
    "es" -> R.string.language_es
    "fr" -> R.string.language_fr
    "de" -> R.string.language_de
    "it" -> R.string.language_it
    "ja" -> R.string.language_ja
    "ru" -> R.string.language_ru
    "zh" -> R.string.language_zh
    "ko" -> R.string.language_ko
    "nl" -> R.string.language_nl
    "sv" -> R.string.language_sv
    "ar" -> R.string.language_ar
    "pt-BR" -> R.string.language_pt_br
    "pt-PT" -> R.string.language_pt_pt
    "hi" -> R.string.language_hi
    "tr" -> R.string.language_tr
    "pl" -> R.string.language_pl
    "el" -> R.string.language_el
    "he" -> R.string.language_he
    "nb" -> R.string.language_nb
    "da" -> R.string.language_da
    "fi" -> R.string.language_fi
    "cs" -> R.string.language_cs
    "hu" -> R.string.language_hu
    "ro" -> R.string.language_ro
    "uk" -> R.string.language_uk
    "th" -> R.string.language_th
    "vi" -> R.string.language_vi
    "id" -> R.string.language_id
    "ms" -> R.string.language_ms
    "fa" -> R.string.language_fa
    "sw" -> R.string.language_sw
    "ca" -> R.string.language_ca
    "is" -> R.string.language_is
    "bg" -> R.string.language_bg
    "hr" -> R.string.language_hr
    "sr" -> R.string.language_sr
    "sk" -> R.string.language_sk
    "et" -> R.string.language_et
    "lv" -> R.string.language_lv
    "lt" -> R.string.language_lt
    "tl" -> R.string.language_tl
    "bn" -> R.string.language_bn
    else -> R.string.language_en
}

val Language.displayName: String
    @Composable get() = stringResource(nameResOf(code))

/** For search and `semantics`, the two places outside composition. */
fun Context.nameOf(language: Language): String = getString(nameResOf(language.code))

/**
 * Matches display name, English name and code, so someone on a Portuguese
 * interface typing "japanese" still finds it.
 */
fun List<Language>.search(term: String, displayName: (Language) -> String): List<Language> {
    val wanted = term.fold()
    if (wanted.isEmpty()) return this
    return filter { language ->
        displayName(language).fold().contains(wanted) ||
            language.englishName.fold().contains(wanted) ||
            language.code.fold().contains(wanted)
    }
}

/**
 * Accent-free on both sides: "japones" typed in a hurry has to find "Japonês".
 *
 * `Locale.ROOT`, not the default — under a Turkish locale that folds "I" to a
 * dotless "ı" and stops matching.
 */
private fun String.fold(): String = Normalizer.normalize(trim(), Normalizer.Form.NFD)
    .replace(COMBINING_MARKS, "")
    .lowercase(Locale.ROOT)

private val COMBINING_MARKS = Regex("\\p{Mn}+")
