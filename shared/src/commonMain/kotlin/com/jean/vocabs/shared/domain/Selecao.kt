package com.jean.vocabs.shared.domain

import com.jean.vocabs.contracts.TargetType

data class SnippetToken(
    val text: String,
    val start: Int,
    val end: Int,
)

private val validToken = Regex("[\\p{L}\\p{N}]+(?:['’\\-][\\p{L}\\p{N}]+)*")
private val repeatedSpaces = Regex("\\s+")

/** Pontuação externa fica fora; apóstrofos e hífens internos permanecem. */
fun tokenizeSnippet(snippet: String): List<SnippetToken> = validToken.findAll(snippet).map { achado ->
    SnippetToken(
        text = achado.value,
        start = achado.range.first,
        end = achado.range.last + 1,
    )
}.toList()

/**
 * Cria uma seleção contínua entre dois tokens. Os limites são [início, fim), o
 * mesmo formato usado pelo banco e por String.substring.
 */
fun selectTokens(snippet: String, primeiro: Int, ultimo: Int = primeiro): SelectedTarget? {
    val tokens = tokenizeSnippet(snippet)
    if (primeiro !in tokens.indices || ultimo !in tokens.indices) return null
    val startIndex = minOf(primeiro, ultimo)
    val endIndex = maxOf(primeiro, ultimo)
    val start = tokens[startIndex].start
    val end = tokens[endIndex].end
    return SelectedTarget(
        text = snippet.substring(start, end),
        start = start,
        end = end,
        type = if (startIndex == endIndex) TargetType.WORD else TargetType.PHRASE,
    )
}

fun SelectedTarget.isValidIn(snippet: String): Boolean =
    start >= 0 && end in (start + 1)..snippet.length && snippet.substring(start, end) == text

/** Só caixa e espaços repetidos são ignorados; acentos e pontuação continuam valendo. */
fun normalizeAnswer(value: String): String = value.trim().lowercase().replace(repeatedSpaces, " ")

fun isAnswerCorrect(answer: String, esperado: String): Boolean =
    normalizeAnswer(answer) == normalizeAnswer(esperado)

fun clozeSnippet(entry: Entry, marcador: String = "________"): String {
    val snippet = entry.snippet.orEmpty()
    val start = entry.start ?: return snippet.replace(entry.target.orEmpty(), marcador)
    val end = entry.end ?: return snippet.replace(entry.target.orEmpty(), marcador)
    if (start !in 0..snippet.length || end !in start..snippet.length) return snippet
    return snippet.replaceRange(start, end, marcador)
}
