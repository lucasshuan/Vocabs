package com.jean.vocabs.server

import java.io.File

/**
 * Configuração em duas camadas: variável de ambiente primeiro, arquivo `.env`
 * local depois.
 *
 * O arquivo existe porque redigitar dois segredos a cada terminal novo é fricção
 * que se paga todo dia — e fricção diária é exatamente o que este projeto existe
 * para eliminar. A variável de ambiente mantém precedência para que CI e produção
 * não dependam de arquivo nenhum.
 *
 * O `.env` está no .gitignore. Nunca commite um.
 */
object Config {

    private val doArquivo: Map<String, String> by lazy { lerArquivoLocal() }

    // O takeIf nos dois lados importa: `APP_TOKEN=` no arquivo (ou exportada
    // vazia) precisa contar como ausente, senão o servidor sobe com token vazio
    // e aceita qualquer requisição sem header.
    operator fun get(chave: String): String? =
        System.getenv(chave)?.takeIf { it.isNotBlank() }
            ?: doArquivo[chave]?.takeIf { it.isNotBlank() }

    fun obrigatorio(chave: String): String = get(chave) ?: error(
        "$chave não definida. Use uma das duas opções:\n" +
            "  1. \$env:$chave = \"valor\"   (só nesta sessão do terminal)\n" +
            "  2. adicione a linha  $chave=valor  no arquivo .env na raiz do projeto",
    )

    /**
     * Procura o `.env` subindo a partir do diretório de trabalho.
     *
     * A subida não é preciosismo: o Gradle roda `:server:run` com o working dir
     * em `server/`, não na raiz do repositório, então procurar só em `.` acharia
     * nada e o arquivo pareceria ignorado.
     */
    private fun lerArquivoLocal(): Map<String, String> {
        val arquivo = generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, ".env") }
            .firstOrNull { it.isFile }
            ?: return emptyMap()

        return arquivo.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains('=') }
            .associate { linha ->
                val chave = linha.substringBefore('=').trim()
                val valor = linha.substringAfter('=').trim().removeSurrounding("\"").removeSurrounding("'")
                chave to valor
            }
    }
}
