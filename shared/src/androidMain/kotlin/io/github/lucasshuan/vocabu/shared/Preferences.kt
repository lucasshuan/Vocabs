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
 * What the person chose and the database does not keep: native language, which
 * courses they have, which one is open, and the theme.
 *
 * `SharedPreferences` rather than DataStore: five scalar values read in the first
 * composition of several screens, and DataStore would make every read suspend —
 * including the theme's, which has to be resolved before the first frame so the
 * app does not flash from light to dark.
 *
 * Nothing here is the source of truth about **cards**. Which pair each card was
 * born in lives in the database, on the capture.
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

    // ---- languages ----------------------------------------------------------

    var native: String
        get() = prefs.getString(NATIVE, null) ?: Languages.DEFAULT_NATIVE
        set(value) = prefs.edit().putString(NATIVE, value).apply()

    var target: String
        get() = prefs.getString(TARGET, null) ?: Languages.DEFAULT_TARGET
        set(value) = prefs.edit().putString(TARGET, value).apply()

    /**
     * The enrolled courses, in the order the strip shows them.
     *
     * A separate list rather than "the languages that already have cards": a newly
     * created course has no words, and disappearing from the strip the instant
     * after being created is the opposite of what the screen promises.
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

    /** Enrolls in a new language and opens it — what the New language button does. */
    fun enroll(code: String) {
        courses = courses + code
        target = code
    }

    /** Switches the open course, enrolling first so it can never point nowhere. */
    fun openCourse(code: String) {
        if (code !in courses) courses = courses + code
        target = code
    }

    /**
     * Removes a language from the strip. Its cards stay in the database.
     *
     * Never empties the list: with no course, Home would have no page, the `+` no
     * destination, and the only way out would be enrolling blind. Leaving the open
     * course opens the first one left.
     */
    fun unenroll(code: String) {
        val rest = courses - code
        if (rest.isEmpty()) return
        courses = rest
        if (target == code) target = rest.first()
    }

    /**
     * Which Words groups are closed.
     *
     * A preference, not screen state: someone studying three languages who wants
     * to see one closes the other two once, and reopening them on every return to
     * the tab would undo the gesture. Stores the closed ones because the default
     * is open — a new language appears expanded without being registered.
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
     * One flow per key, fed by SharedPreferences' own listener.
     *
     * This is what makes the language strip and the word list rebuild on the same
     * frame the course changes, rather than the next time the screen is recreated.
     */
    private fun <T> observe(vararg keys: String, read: () -> T): Flow<T> = callbackFlow {
        trySend(read())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key in keys) trySend(read())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
        // Conflated because what matters is the current value, not the series:
        // switching course three times with the screen busy should show the
        // third, not all three in sequence.
        .conflate()
        .distinctUntilChanged()

    fun observeLanguagePair(): Flow<LanguagePair> = observe(NATIVE, TARGET) { languagePair }

    fun observeCourses(): Flow<List<String>> = observe(COURSES) { courses }

    fun observeTheme(): Flow<ThemePreference> = observe(THEME) { theme }

    fun observeCollapsedGroups(): Flow<Set<String>> = observe(COLLAPSED) { collapsedGroups }

    /** The native language alone, for Settings' "My language" row. */
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

/** Light, dark, or whatever the device says — Settings' segmented control. */
enum class ThemePreference {
    LIGHT,
    DARK,
    SYSTEM;

    companion object {
        fun of(value: String?): ThemePreference = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}
