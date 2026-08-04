package com.jean.vocabs.shared

import android.content.Context
import android.content.SharedPreferences
import com.jean.vocabs.contracts.Idiomas
import com.jean.vocabs.shared.domain.ParIdiomas
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * O que a pessoa escolheu e o banco não guarda: idioma nativo, que cursos ela
 * tem, qual está aberto e o tema.
 *
 * `SharedPreferences` e não DataStore: são cinco valores escalares lidos na
 * primeira composição de várias telas, e o DataStore obrigaria toda leitura a
 * ser suspensa — inclusive a do tema, que precisa estar resolvida antes do
 * primeiro frame para a tela não piscar do claro para o escuro.
 *
 * Nada aqui é fonte da verdade sobre **fichas**. Em que par cada ficha nasceu
 * está no banco, na captura. Estas preferências dizem só o que fazer agora.
 */
class Preferencias(context: Context) {

    /**
     * `"tagarara"` é o **nome do arquivo em disco**, e não o nome do app.
     *
     * Ele fica pela mesma razão que `applicationId`, namespace e `vocabs.db`
     * ficam: renomeá-lo não migra nada, cria um arquivo vazio ao lado do antigo.
     * Quem já tem o app instalado abriria a versão nova sem idioma nativo, sem os
     * cursos em que se matriculou, no curso padrão e no tema padrão — com o banco
     * de palavras intacto e nenhuma tela sabendo em que língua exibi-lo.
     *
     * Se um dia precisar mudar, mude junto com uma migração que leia o arquivo
     * antigo e copie os cinco valores. Sozinho, o nome novo é perda de dados.
     */
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("tagarara", Context.MODE_PRIVATE)

    // ---- idiomas ------------------------------------------------------------

    var nativo: String
        get() = prefs.getString(NATIVO, null) ?: Idiomas.NATIVO_PADRAO
        set(valor) = prefs.edit().putString(NATIVO, valor).apply()

    var alvo: String
        get() = prefs.getString(ALVO, null) ?: Idiomas.ALVO_PADRAO
        set(valor) = prefs.edit().putString(ALVO, valor).apply()

    /**
     * Os cursos matriculados, na ordem em que a faixa os mostra.
     *
     * Uma lista à parte, e não "os idiomas que já têm ficha": um curso recém
     * criado não tem palavra nenhuma, e sumir da faixa no instante seguinte ao
     * de ser criado é o oposto do que a tela promete.
     */
    var cursos: List<String>
        get() = prefs.getString(CURSOS, null)
            ?.split(SEPARADOR)
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?: listOf(Idiomas.ALVO_PADRAO)
        set(valor) = prefs.edit()
            .putString(CURSOS, valor.distinct().joinToString(SEPARADOR))
            .apply()

    val par: ParIdiomas get() = ParIdiomas(nativo = nativo, alvo = alvo)

    /** Matricula num idioma novo e já o deixa aberto — é o que o botão da tela 5c faz. */
    fun matricular(codigo: String) {
        cursos = cursos + codigo
        alvo = codigo
    }

    /** Troca o curso aberto. Matricula por segurança: escolher o que não existe seria um beco. */
    fun abrirCurso(codigo: String) {
        if (codigo !in cursos) cursos = cursos + codigo
        alvo = codigo
    }

    /**
     * Tira um idioma da faixa. As fichas dele continuam no banco.
     *
     * Nunca esvazia a lista: sem curso nenhum a Início não teria página, o `+`
     * não teria destino e a única saída seria matricular de novo às cegas. Sair
     * do curso aberto abre o primeiro que sobrou, para que a tela não fique
     * apontando para um idioma que não está mais ali.
     */
    fun desmatricular(codigo: String) {
        val restantes = cursos - codigo
        if (restantes.isEmpty()) return
        cursos = restantes
        if (alvo == codigo) alvo = restantes.first()
    }

    /**
     * Que grupos de Vocabulários estão fechados.
     *
     * É preferência e não estado de tela: quem estuda três idiomas e só quer ver
     * um deles fecha os outros dois uma vez, e reabri-los a cada volta para a aba
     * anularia o gesto. Guarda os fechados (e não os abertos) porque o padrão é
     * tudo aberto — um idioma novo aparece expandido sem precisar ser inscrito.
     */
    var gruposRecolhidos: Set<String>
        get() = prefs.getStringSet(RECOLHIDOS, emptySet()).orEmpty()
        set(valor) = prefs.edit().putStringSet(RECOLHIDOS, valor).apply()

    fun alternarGrupo(codigo: String) {
        gruposRecolhidos = gruposRecolhidos.let { if (codigo in it) it - codigo else it + codigo }
    }

    // ---- tema ---------------------------------------------------------------

    var tema: PreferenciaDeTema
        get() = PreferenciaDeTema.de(prefs.getString(TEMA, null))
        set(valor) = prefs.edit().putString(TEMA, valor.name).apply()

    // ---- observação ---------------------------------------------------------

    /**
     * Um fluxo por chave, alimentado pelo listener do próprio SharedPreferences.
     *
     * É isso que faz a faixa de idiomas e a lista de palavras se refazerem no
     * mesmo frame em que a pessoa troca de curso, em vez de na próxima vez que a
     * tela for recriada.
     */
    private fun <T> observar(vararg chaves: String, ler: () -> T): Flow<T> = callbackFlow {
        trySend(ler())
        val ouvinte = SharedPreferences.OnSharedPreferenceChangeListener { _, chave ->
            if (chave in chaves) trySend(ler())
        }
        prefs.registerOnSharedPreferenceChangeListener(ouvinte)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(ouvinte) }
    }
        // Conflate porque o que interessa é o valor atual, não a série de
        // valores: quem trocar de curso três vezes seguidas com a tela ocupada
        // quer ver o terceiro, e não as três telas em sequência.
        .conflate()
        .distinctUntilChanged()

    fun observarPar(): Flow<ParIdiomas> = observar(NATIVO, ALVO) { par }

    fun observarCursos(): Flow<List<String>> = observar(CURSOS) { cursos }

    fun observarTema(): Flow<PreferenciaDeTema> = observar(TEMA) { tema }

    fun observarGruposRecolhidos(): Flow<Set<String>> = observar(RECOLHIDOS) { gruposRecolhidos }

    /** O nativo sozinho, para a linha "Meu idioma" da tela Configurações. */
    fun observarNativo(): Flow<String> = observarPar().map { it.nativo }

    private companion object {
        const val NATIVO = "idioma_nativo"
        const val ALVO = "idioma_alvo"
        const val CURSOS = "cursos"
        const val TEMA = "tema"
        const val RECOLHIDOS = "grupos_recolhidos"

        /** Vírgula não aparece em código de idioma nenhum do catálogo. */
        const val SEPARADOR = ","
    }
}

/** Claro, escuro ou o que o aparelho mandar — o segmentado da tela Configurações. */
enum class PreferenciaDeTema {
    CLARO,
    ESCURO,
    AUTO;

    companion object {
        fun de(valor: String?): PreferenciaDeTema = entries.firstOrNull { it.name == valor } ?: AUTO
    }
}
