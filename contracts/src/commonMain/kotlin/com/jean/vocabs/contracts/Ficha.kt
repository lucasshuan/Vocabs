package com.jean.vocabs.contracts

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Contrato entre o app e o servidor. Estes tipos são compilados nos dois lados:
 * mudar um campo aqui quebra o build do app E do servidor, em vez de virar
 * bug em runtime.
 */

/** O que o app manda: o modelo unificado de captura (seção 5 do documento). */
@Serializable
data class GerarFichaRequest(
    val trecho: String,
    val alvo: String,
    /** Classificado localmente: um token é palavra; vários, expressão. */
    val tipo: TipoAlvo,
)

/** O que o servidor devolve: a ficha da Fase 1. */
@Serializable
data class FichaResponse(
    val tipo: TipoAlvo,
    val traducao: String,
    val definicoes: List<String>,
    val exemplo: String,
    val ipa: String,
    /** Termos próximos para a seção "Puxa outras palavras". */
    val relacionadas: List<String> = emptyList(),
)

/**
 * Palavra vs expressão é uma decisão de captura, não da IA: um token selecionado
 * vira PALAVRA e dois ou mais tokens contíguos viram EXPRESSAO.
 */
@Serializable
enum class TipoAlvo {
    @SerialName("PALAVRA")
    PALAVRA,

    @SerialName("EXPRESSAO")
    EXPRESSAO,
}

/** Erro devolvido pelo servidor, para o app ter o que mostrar. */
@Serializable
data class ErroResponse(
    val mensagem: String,
)
