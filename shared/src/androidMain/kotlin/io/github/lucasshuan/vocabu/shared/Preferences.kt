package io.github.lucasshuan.vocabu.shared

import android.content.Context
import android.content.SharedPreferences
import io.github.lucasshuan.vocabu.contracts.Languages
import io.github.lucasshuan.vocabu.shared.domain.LanguagePair
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * `SharedPreferences`, not DataStore: five scalars read in first composition,
 * and a suspending theme read would flash light before dark.
 *
 * Nothing here is the truth about cards — the pair each was born in is on the
 * capture, in the database.
 */
class Preferences(context: Context) {

    /**
     * A storage identity. Renaming it migrates nothing: it opens an empty file
     * beside the old one, and the install comes back with no native language and
     * no enrolled courses while the word database sits there intact. Change it
     * only with a migration that reads the old name first.
     */
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("vocabu_prefs", Context.MODE_PRIVATE)

    // ---- languages ----------------------------------------------------------

    var native: String
        get() = prefs.getString(NATIVE, null) ?: Languages.DEFAULT_NATIVE
        set(value) = prefs.edit().putString(NATIVE, value).apply()

    var target: String
        get() = prefs.getString(TARGET, null) ?: Languages.DEFAULT_TARGET
        set(value) = prefs.edit().putString(TARGET, value).apply()

    /**
     * In strip order. A separate list, not "languages that have cards": a new
     * course has no words, and would vanish from the strip on creation.
     */
    var courses: List<String>
        get() = prefs.getString(COURSES, null)
            ?.split(SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?: listOf(Languages.DEFAULT_TARGET)
        set(value) = prefs.edit()
            .putString(COURSES, value.distinct().joinToString(SEPARATOR))
            .apply()

    val languagePair: LanguagePair get() = LanguagePair(native = native, target = target)

    fun enroll(code: String) {
        courses = courses + code
        target = code
    }

    /** Enrols first, so the open course can never point nowhere. */
    fun openCourse(code: String) {
        if (code !in courses) courses = courses + code
        target = code
    }

    /**
     * Off the strip only; the cards stay. Never empties the list — with no
     * course Home has no page and the `+` no destination.
     */
    fun unenroll(code: String) {
        val rest = courses - code
        if (rest.isEmpty()) return
        courses = rest
        if (target == code) target = rest.first()
    }

    /**
     * A preference, not screen state: reopening the groups on every return to
     * the tab would undo the gesture. Stores the closed ones, so a new language
     * appears expanded without being registered anywhere.
     */
    var collapsedGroups: Set<String>
        get() = prefs.getStringSet(COLLAPSED, emptySet()).orEmpty()
        set(value) = prefs.edit().putStringSet(COLLAPSED, value).apply()

    fun toggleGroup(code: String) {
        collapsedGroups = collapsedGroups.let { if (code in it) it - code else it + code }
    }

    // ---- theme --------------------------------------------------------------

    var theme: ThemePreference
        get() = ThemePreference.of(prefs.getString(THEME, null))
        set(value) = prefs.edit().putString(THEME, value.name).apply()

    // ---- observation --------------------------------------------------------

    /**
     * On SharedPreferences' own listener, so the strip and the word list rebuild
     * on the frame the course changes, not the next time the screen is recreated.
     */
    private fun <T> observe(vararg keys: String, read: () -> T): Flow<T> = callbackFlow {
        trySend(read())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key in keys) trySend(read())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
        // The current value, not the series: three switches with the screen busy
        // should land on the third, not replay all three.
        .conflate()
        .distinctUntilChanged()

    fun observeLanguagePair(): Flow<LanguagePair> = observe(NATIVE, TARGET) { languagePair }

    fun observeCourses(): Flow<List<String>> = observe(COURSES) { courses }

    fun observeTheme(): Flow<ThemePreference> = observe(THEME) { theme }

    fun observeCollapsedGroups(): Flow<Set<String>> = observe(COLLAPSED) { collapsedGroups }

    fun observeNativeLanguage(): Flow<String> = observeLanguagePair().map { it.native }

    private companion object {
        const val NATIVE = "native_language"
        const val TARGET = "target_language"
        const val COURSES = "courses"
        const val THEME = "theme"
        const val COLLAPSED = "collapsed_groups"

        /** No language code in the catalog contains a comma. */
        const val SEPARATOR = ","
    }
}

enum class ThemePreference {
    LIGHT,
    DARK,
    SYSTEM;

    companion object {
        fun of(value: String?): ThemePreference = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}
