package com.jean.vocabs.shared

import android.content.Context
import android.content.SharedPreferences
import com.jean.vocabs.contracts.Languages
import com.jean.vocabs.shared.domain.LanguagePair
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
class Preferences(context: Context) {

    /**
     * The file name on disk. Renaming it does not migrate anything — it creates
     * an empty file beside the old one, so an existing install comes back with
     * no native language, no enrolled courses and the default theme, while the
     * word database sits there intact and no screen knows what language to show
     * it in. Changing it again means shipping a migration that reads the old
     * file first.
     */
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("vocabu_prefs", Context.MODE_PRIVATE)

    // ---- idiomas ------------------------------------------------------------

    var native: String
        get() = prefs.getString(NATIVO, null) ?: Languages.NATIVO_PADRAO
        set(value) = prefs.edit().putString(NATIVO, value).apply()

    var target: String
        get() = prefs.getString(ALVO, null) ?: Languages.ALVO_PADRAO
        set(value) = prefs.edit().putString(ALVO, value).apply()

    /**
     * Os cursos matriculados, na ordem em que a faixa os mostra.
     *
     * Uma lista à parte, e não "os idiomas que já têm ficha": um curso recém
     * criado não tem palavra nenhuma, e sumir da faixa no instante seguinte ao
     * de ser criado é o oposto do que a tela promete.
     */
    var courses: List<String>
        get() = prefs.getString(CURSOS, null)
            ?.split(SEPARADOR)
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?: listOf(Languages.ALVO_PADRAO)
        set(value) = prefs.edit()
            .putString(CURSOS, value.distinct().joinToString(SEPARADOR))
            .apply()

    val languagePair: LanguagePair get() = LanguagePair(native = native, target = target)

    /** Matricula num language novo e já o deixa aberto — é o que o botão da tela 5c faz. */
    fun enroll(codigo: String) {
        courses = courses + codigo
        target = codigo
    }

    /** Troca o course aberto. Matricula por segurança: escolher o que não existe seria um beco. */
    fun openCourse(codigo: String) {
        if (codigo !in courses) courses = courses + codigo
        target = codigo
    }

    /**
     * Tira um idioma da faixa. As fichas dele continuam no banco.
     *
     * Nunca esvazia a lista: sem curso nenhum a Início não teria página, o `+`
     * não teria destino e a única saída seria enroll de novo às cegas. Sair
     * do curso aberto abre o primeiro que sobrou, para que a tela não fique
     * apontando para um idioma que não está mais ali.
     */
    fun unenroll(codigo: String) {
        val restantes = courses - codigo
        if (restantes.isEmpty()) return
        courses = restantes
        if (target == codigo) target = restantes.first()
    }

    /**
     * Que grupos de Vocabulários estão fechados.
     *
     * É preferência e não estado de tela: quem estuda três idiomas e só quer ver
     * um deles fecha os outros dois uma vez, e reabri-los a cada volta para a aba
     * anularia o gesto. Guarda os fechados (e não os abertos) porque o padrão é
     * tudo aberto — um idioma novo aparece expandido sem precisar ser inscrito.
     */
    var collapsedGroups: Set<String>
        get() = prefs.getStringSet(RECOLHIDOS, emptySet()).orEmpty()
        set(value) = prefs.edit().putStringSet(RECOLHIDOS, value).apply()

    fun toggleGroup(codigo: String) {
        collapsedGroups = collapsedGroups.let { if (codigo in it) it - codigo else it + codigo }
    }

    // ---- tema ---------------------------------------------------------------

    var theme: ThemePreference
        get() = ThemePreference.de(prefs.getString(TEMA, null))
        set(value) = prefs.edit().putString(TEMA, value.name).apply()

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
        val ouvinte = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key in chaves) trySend(ler())
        }
        prefs.registerOnSharedPreferenceChangeListener(ouvinte)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(ouvinte) }
    }
        // Conflate porque o que interessa é o valor atual, não a série de
        // valores: quem trocar de curso três vezes seguidas com a tela ocupada
        // quer ver o terceiro, e não as três telas em sequência.
        .conflate()
        .distinctUntilChanged()

    fun observeLanguagePair(): Flow<LanguagePair> = observar(NATIVO, ALVO) { languagePair }

    fun observeCourses(): Flow<List<String>> = observar(CURSOS) { courses }

    fun observeTheme(): Flow<ThemePreference> = observar(TEMA) { theme }

    fun observeCollapsedGroups(): Flow<Set<String>> = observar(RECOLHIDOS) { collapsedGroups }

    /** O native sozinho, para a row "Meu language" da tela Configurações. */
    fun observeNativeLanguage(): Flow<String> = observeLanguagePair().map { it.native }

    private companion object {
        const val NATIVO = "native_language"
        const val ALVO = "target_language"
        const val CURSOS = "courses"
        const val TEMA = "theme"
        const val RECOLHIDOS = "collapsed_groups"

        /** Vírgula não aparece em código de language nenhum do catálogo. */
        const val SEPARADOR = ","
    }
}

/** Claro, escuro ou o que o aparelho mandar — o segmentado da tela Configurações. */
enum class ThemePreference {
    LIGHT,
    DARK,
    SYSTEM;

    companion object {
        fun de(value: String?): ThemePreference = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}
