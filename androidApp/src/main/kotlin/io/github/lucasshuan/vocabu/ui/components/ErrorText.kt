package io.github.lucasshuan.vocabu.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.lucasshuan.vocabu.R
import io.github.lucasshuan.vocabu.contracts.ErrorCode

/**
 * The server sends a code so the sentence can be chosen here, in the interface
 * language. The provider's free text travels apart and is never translated.
 */
@StringRes
fun errorTextRes(code: ErrorCode?): Int = when (code) {
    ErrorCode.UNKNOWN_LANGUAGE_PAIR -> R.string.error_unknown_language_pair
    ErrorCode.MISSING_FIELDS -> R.string.error_missing_fields
    ErrorCode.INVALID_TOKEN -> R.string.error_invalid_token
    ErrorCode.UNREACHABLE -> R.string.error_unreachable
    ErrorCode.HTTP_ERROR -> R.string.error_http
    ErrorCode.GENERATION_FAILED, null -> R.string.error_generation_failed
}

@Composable
fun errorText(code: ErrorCode?): String = stringResource(errorTextRes(code))
