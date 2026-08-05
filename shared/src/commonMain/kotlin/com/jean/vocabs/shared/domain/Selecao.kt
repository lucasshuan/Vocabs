package com.jean.vocabs.shared.domain

import com.jean.vocabs.contracts.TargetType

data class TokenDoTrecho(
    val texto: String,
    val inicio: Int,
    val fim: Int,
)

private val tokenValido = Regex("[\\p{L}\\p{N}]+(?:['’\\-][\\p{L}\\p{N}]+)*")
private val espacosRepetidos = Regex("\\s+")

/** Pontuação externa fica fora; apóstrofos e hífens internos permanecem. */
fun tokenizarTrecho(trecho: String): List<TokenDoTrecho> = tokenValido.findAll(trecho).map { achado ->
    TokenDoTrecho(
        texto = achado.value,
        inicio = achado.range.first,
        fim = achado.range.last + 1,
    )
}.toList()

/**
 * Cria uma seleção contínua entre dois tokens. Os limites são [início, fim), o
 * mesmo formato usado pelo banco e por String.substring.
 */
fun selecionarTokens(trecho: String, primeiro: Int, ultimo: Int = primeiro): AlvoSelecionado? {
    val tokens = tokenizarTrecho(trecho)
    if (primeiro !in tokens.indices || ultimo !in tokens.indices) return null
    val inicioIndice = minOf(primeiro, ultimo)
    val fimIndice = maxOf(primeiro, ultimo)
    val inicio = tokens[inicioIndice].inicio
    val fim = tokens[fimIndice].fim
    return AlvoSelecionado(
        texto = trecho.substring(inicio, fim),
        inicio = inicio,
        fim = fim,
        tipo = if (inicioIndice == fimIndice) TargetType.WORD else TargetType.PHRASE,
    )
}

fun AlvoSelecionado.eValidoEm(trecho: String): Boolean =
    inicio >= 0 && fim in (inicio + 1)..trecho.length && trecho.substring(inicio, fim) == texto

/** Só caixa e espaços repetidos são ignorados; acentos e pontuação continuam valendo. */
fun normalizarResposta(valor: String): String = valor.trim().lowercase().replace(espacosRepetidos, " ")

fun respostaCorreta(resposta: String, esperado: String): Boolean =
    normalizarResposta(resposta) == normalizarResposta(esperado)

fun trechoCloze(entrada: Entrada, marcador: String = "________"): String {
    val trecho = entrada.trecho.orEmpty()
    val inicio = entrada.inicio ?: return trecho.replace(entrada.alvo.orEmpty(), marcador)
    val fim = entrada.fim ?: return trecho.replace(entrada.alvo.orEmpty(), marcador)
    if (inicio !in 0..trecho.length || fim !in inicio..trecho.length) return trecho
    return trecho.replaceRange(inicio, fim, marcador)
}
