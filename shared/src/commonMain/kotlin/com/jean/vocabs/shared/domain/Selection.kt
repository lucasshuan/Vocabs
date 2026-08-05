package com.jean.vocabs.shared.domain

import com.jean.vocabs.contracts.TargetType

data class SnippetToken(
    val text: String,
    val start: Int,
    val end: Int,
)

private val validToken = Regex("[\\p{L}\\p{N}]+(?:['’\\-][\\p{L}\\p{N}]+)*")
private val repeatedSpaces = Regex("\\s+")

/** External punctuation is dropped; internal apostrophes and hyphens stay. */
fun tokenizeSnippet(snippet: String): List<SnippetToken> = validToken.findAll(snippet).map { found ->
    SnippetToken(
        text = found.value,
        start = found.range.first,
        end = found.range.last + 1,
    )
}.toList()

/**
 * Builds a continuous selection between two tokens. Bounds are [start, end), the
 * same shape the database and String.substring use.
 */
fun selectTokens(snippet: String, first: Int, last: Int = first): SelectedTarget? {
    val tokens = tokenizeSnippet(snippet)
    if (first !in tokens.indices || last !in tokens.indices) return null
    val startIndex = minOf(first, last)
    val endIndex = maxOf(first, last)
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

/** Only case and repeated spaces are ignored; accents and punctuation still count. */
fun normalizeAnswer(value: String): String = value.trim().lowercase().replace(repeatedSpaces, " ")

fun isAnswerCorrect(answer: String, expected: String): Boolean =
    normalizeAnswer(answer) == normalizeAnswer(expected)

fun clozeSnippet(entry: Entry, marker: String = "________"): String {
    val snippet = entry.snippet.orEmpty()
    val start = entry.start ?: return snippet.replace(entry.target.orEmpty(), marker)
    val end = entry.end ?: return snippet.replace(entry.target.orEmpty(), marker)
    if (start !in 0..snippet.length || end !in start..snippet.length) return snippet
    return snippet.replaceRange(start, end, marker)
}
