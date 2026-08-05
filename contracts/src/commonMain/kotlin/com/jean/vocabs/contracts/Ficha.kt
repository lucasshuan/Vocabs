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
data class GenerateCardRequest(
    val snippet: String,
    val target: String,
    /** Classificado localmente: um token é palavra; vários, expressão. */
    val type: TargetType,
    /** Código de [Languages]: em que língua a ficha é escrita. */
    val nativeLanguage: String,
    /** Código de [Languages]: que língua a ficha ensina. */
    val targetLanguage: String,
)

/** O que o servidor devolve: a ficha da Fase 1. */
@Serializable
data class CardResponse(
    val type: TargetType,
    val translation: String,
    val definitions: List<String>,
    val example: String,
    /**
     * Como o termo se pronuncia, na notação que o idioma alvo pede.
     *
     * Chamava-se `ipa` enquanto só havia inglês. IPA não é o que quem aprende
     * mandarim quer ler — ali a resposta é pinyin, e em japonês é o kana com o
     * romaji ao lado. O campo carrega a notação de cada idioma, e qual é ela
     * está em `TargetLanguage`, no servidor.
     */
    val pronunciation: String,
    /** Termos próximos para a seção "Puxa outras palavras". */
    val related: List<String> = emptyList(),
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

/**
 * O erro do servidor, como código em vez de frase.
 *
 * A mensagem antiga vinha pronta em português e era gravada no banco, então a
 * língua da falha era a do servidor e não a de quem lê. O código escolhe um
 * recurso de texto no app; [detail] carrega o que não dá para traduzir — a
 * frase crua do provedor de IA, útil só para diagnóstico.
 */
@Serializable
data class ErrorResponse(
    val code: String,
    val detail: String? = null,
)

/**
 * Os códigos que o app sabe traduzir.
 *
 * Viaja como `String` no fio, e não como enum: kotlinx.serialization recusa um
 * valor de enum que não conhece, então um servidor novo com um código a mais
 * derrubaria a decodificação num app antigo. [of] é a mesma tolerância que
 * `EntryStatus.de` e companhia já usam.
 */
enum class ErrorCode {
    UNKNOWN_LANGUAGE_PAIR,
    MISSING_FIELDS,
    INVALID_TOKEN,
    GENERATION_FAILED,

    /** Só do lado do app: o servidor não respondeu. */
    UNREACHABLE,

    /** Só do lado do app: respondeu, mas não em um formato que dê para ler. */
    HTTP_ERROR,
    ;

    companion object {
        fun of(valor: String?): ErrorCode =
            entries.firstOrNull { it.name == valor } ?: GENERATION_FAILED
    }
}
