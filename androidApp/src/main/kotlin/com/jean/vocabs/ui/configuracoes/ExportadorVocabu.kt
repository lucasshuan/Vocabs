package com.jean.vocabs.ui.configuracoes

import android.content.Context
import com.jean.vocabs.shared.domain.DadosExportacao
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object ExportadorVocabu {
    suspend fun criar(context: Context, dados: DadosExportacao): File = withContext(Dispatchers.IO) {
        val pasta = File(context.cacheDir, "exports").apply { mkdirs() }
        val destino = File(pasta, "Vocabu-${System.currentTimeMillis()}.zip")
        ZipOutputStream(FileOutputStream(destino)).use { zip ->
            zip.putNextEntry(ZipEntry("Vocabu.json"))
            zip.write(json(dados).toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            dados.capturas.distinctBy { it.midiaCaminho }.forEach { captura ->
                val caminho = captura.midiaCaminho ?: return@forEach
                val arquivo = File(caminho).takeIf(File::isFile) ?: return@forEach
                zip.putNextEntry(ZipEntry("media/${captura.id}-${arquivo.name}"))
                FileInputStream(arquivo).use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        destino
    }

    private fun json(dados: DadosExportacao) = JSONObject().apply {
        // 2: `ipa` virou `pronuncia` e o par de idiomas deixou de ser um valor
        // fixo do arquivo para ser uma propriedade de cada captura.
        put("schemaVersion", 2)
        put("app", "Vocabu")
        put("exportadoEm", System.currentTimeMillis())
        put("usoIa", JSONObject().put("mes", dados.usoIa.mes).put("geracoes", dados.usoIa.usadas))
        put("capturas", JSONArray().apply {
            dados.capturas.forEach { captura -> put(JSONObject().apply {
                put("id", captura.id)
                put("trecho", captura.trecho)
                put("origem", captura.origem)
                put("criadoEm", captura.criadoEm)
                put("status", captura.status.name)
                put("formato", captura.formato.name)
                put("midia", captura.midiaCaminho?.let { "media/${captura.id}-${File(it).name}" })
                put("duracaoMs", captura.duracaoMs)
                put("erroTranscricao", captura.erroTranscricao)
                put("idiomaNativo", captura.par.nativo)
                put("idiomaAlvo", captura.par.alvo)
            }) }
        })
        put("entradas", JSONArray().apply {
            dados.entradas.forEach { entrada -> put(JSONObject().apply {
                put("id", entrada.id)
                put("capturaId", entrada.capturaId)
                put("alvo", entrada.alvo)
                put("inicio", entrada.inicio)
                put("fim", entrada.fim)
                put("tipo", entrada.tipo.name)
                put("status", entrada.status.name)
                put("traducao", entrada.ficha?.traducao)
                put("definicoes", JSONArray(entrada.ficha?.definicoes.orEmpty()))
                put("exemplo", entrada.ficha?.exemplo)
                put("pronuncia", entrada.ficha?.pronuncia)
                put("relacionadas", JSONArray(entrada.ficha?.relacionadas.orEmpty()))
                put("erro", entrada.erro)
                put("retencao", entrada.retencao?.let { retencao -> JSONObject()
                    .put("pontos", retencao.pontos)
                    .put("taxaDecaimento", retencao.taxa)
                    .put("ultimaInteracao", retencao.ultimaInteracao)
                    .put("revisoes", retencao.revisoes)
                    .put("acertos", retencao.acertos)
                    .put("erros", retencao.erros)
                })
            }) }
        })
        put("atividade", JSONArray().apply {
            dados.atividade.forEach { put(JSONObject().put("dia", it.dia).put("revisoes", it.revisoes)) }
        })
    }
}
