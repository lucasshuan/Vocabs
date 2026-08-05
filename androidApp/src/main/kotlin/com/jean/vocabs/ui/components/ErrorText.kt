package com.jean.vocabs.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jean.vocabs.R
import com.jean.vocabs.contracts.ErrorCode

/**
 * The server sends a code, not a sentence, so the sentence is chosen here.
 *
 * That split is what lets a failure be readable in the interface language
 * rather than in whatever language the server happens to speak. Any free text
 * the provider returned travels separately and is never translated.
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
