package com.jean.vocabs.ui

import com.jean.vocabs.contracts.ErrorCode
import com.jean.vocabs.contracts.Language

/**
 * Scaffolding. Everything in this file is replaced by string resources and the
 * whole file is deleted — the language names when the resource catalog lands,
 * the error text when the UI strings are extracted.
 *
 * It exists so the app keeps compiling in between: the display name left the
 * shared contract (the server compiles that module and has no Android
 * resources), and the server now sends an error code instead of a sentence.
 * Both need somewhere to resolve to text, and resources are not there yet.
 */

/** Replaces the display name that used to live on `Language` in `:contracts`. */
val Language.displayName: String
    get() = NOMES_TEMPORARIOS[code] ?: code

/** Was the sentence the server used to send, before it sent a code. */
fun temporaryErrorText(code: ErrorCode?): String = when (code) {
    ErrorCode.UNKNOWN_LANGUAGE_PAIR -> "Este par de idiomas não é aceito."
    ErrorCode.MISSING_FIELDS -> "Faltou o trecho ou o alvo."
    ErrorCode.INVALID_TOKEN -> "O app não conseguiu se identificar no servidor."
    ErrorCode.UNREACHABLE -> "Não foi possível falar com o servidor."
    ErrorCode.HTTP_ERROR -> "O servidor respondeu algo inesperado."
    ErrorCode.GENERATION_FAILED, null -> "Não foi possível gerar a ficha."
}

private val NOMES_TEMPORARIOS: Map<String, String> = mapOf(
    "en" to "Inglês",
    "es" to "Espanhol",
    "fr" to "Francês",
    "de" to "Alemão",
    "it" to "Italiano",
    "ja" to "Japonês",
    "ru" to "Russo",
    "zh" to "Mandarim",
    "ko" to "Coreano",
    "nl" to "Holandês",
    "sv" to "Sueco",
    "ar" to "Árabe",
    "pt-BR" to "Português (Brasil)",
    "pt-PT" to "Português (Portugal)",
    "hi" to "Hindi",
    "tr" to "Turco",
    "pl" to "Polonês",
    "el" to "Grego",
    "he" to "Hebraico",
    "nb" to "Norueguês",
    "da" to "Dinamarquês",
    "fi" to "Finlandês",
    "cs" to "Tcheco",
    "hu" to "Húngaro",
    "ro" to "Romeno",
    "uk" to "Ucraniano",
    "th" to "Tailandês",
    "vi" to "Vietnamita",
    "id" to "Indonésio",
    "ms" to "Malaio",
    "fa" to "Persa",
    "sw" to "Suaíli",
    "ca" to "Catalão",
    "is" to "Islandês",
    "bg" to "Búlgaro",
    "hr" to "Croata",
    "sr" to "Sérvio",
    "sk" to "Eslovaco",
    "et" to "Estoniano",
    "lv" to "Letão",
    "lt" to "Lituano",
    "tl" to "Filipino",
    "bn" to "Bengali",
)
