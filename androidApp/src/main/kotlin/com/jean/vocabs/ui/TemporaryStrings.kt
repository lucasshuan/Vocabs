package com.jean.vocabs.ui

import com.jean.vocabs.contracts.ErrorCode

/**
 * Scaffolding. The language names left for the resource catalog; this last piece
 * goes when the UI strings are extracted, and the file with it.
 *
 * It exists because the server sends an error code instead of a sentence, and the
 * code has to resolve to text somewhere.
 */

/** Was the sentence the server used to send, before it sent a code. */
fun temporaryErrorText(code: ErrorCode?): String = when (code) {
    ErrorCode.UNKNOWN_LANGUAGE_PAIR -> "Este par de idiomas não é aceito."
    ErrorCode.MISSING_FIELDS -> "Faltou o trecho ou o alvo."
    ErrorCode.INVALID_TOKEN -> "O app não conseguiu se identificar no servidor."
    ErrorCode.UNREACHABLE -> "Não foi possível falar com o servidor."
    ErrorCode.HTTP_ERROR -> "O servidor respondeu algo inesperado."
    ErrorCode.GENERATION_FAILED, null -> "Não foi possível gerar a ficha."
}
