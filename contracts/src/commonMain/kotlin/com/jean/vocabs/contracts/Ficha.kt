package com.jean.vocabs.contracts

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Contrato entre o app e o servidor. Estes tipos são compilados nos dois lados:
 * mudar um campo aqui quebra o build do app E do servidor, em vez de virar
 * bug em runtime.
 */

/**
 * O que o app manda: o modelo unificado de captura (seção 5 do documento).
 *
 * O par de idiomas viaja junto, e não como configuração do servidor, porque ele
 * é uma propriedade da **entrada** — cada uma nasceu num par e é regerada nele.
 * Um servidor que soubesse o par sozinho reescreveria uma ficha de alemão em
 * inglês no dia em que a pessoa trocasse de curso.
 */
@Serializable
data class GerarFichaRequest(
    val trecho: String,
    val alvo: String,
    /** Classificado localmente: um token é palavra; vários, expressão. */
    val tipo: TargetType,
    /** Código de [Idiomas]: em que língua a ficha é escrita. */
    val idiomaNativo: String,
    /** Código de [Idiomas]: que língua a ficha ensina. */
    val idiomaAlvo: String,
)

/** O que o servidor devolve: a ficha da Fase 1. */
@Serializable
data class FichaResponse(
    val tipo: TargetType,
    val traducao: String,
    val definicoes: List<String>,
    val exemplo: String,
    /**
     * Como o termo se pronuncia, na notação que o idioma alvo pede.
     *
     * Chamava-se `ipa` enquanto só havia inglês. IPA não é o que quem aprende
     * mandarim quer ler — ali a resposta é pinyin, e em japonês é o kana com o
     * romaji ao lado. O campo carrega a notação de cada idioma, e qual é ela
     * está em `IdiomaAlvo`, no servidor.
     */
    val pronuncia: String,
    /** Termos próximos para a seção "Puxa outras palavras". */
    val relacionadas: List<String> = emptyList(),
)

/**
 * Palavra vs expressão é uma decisão de captura, não da IA: um token selecionado
 * vira WORD e dois ou mais tokens contíguos viram PHRASE.
 */
@Serializable
enum class TargetType {
    @SerialName("WORD")
    WORD,

    @SerialName("PHRASE")
    PHRASE,
}

/** Erro devolvido pelo servidor, para o app ter o que mostrar. */
@Serializable
data class ErroResponse(
    val mensagem: String,
)
