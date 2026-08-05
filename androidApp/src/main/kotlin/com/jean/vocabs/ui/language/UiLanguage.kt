package com.jean.vocabs.ui.language

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * The language the interface is written in — a different setting from the
 * language cards are written in, which lives in [com.jean.vocabs.shared.Preferences].
 *
 * On API 33+ the system is the store. `LocaleManager` holds the choice, and the
 * same choice is settable from Settings > Apps > Vocabu > Language, so mirroring
 * it into our own preferences would go stale the first time someone changes it
 * there.
 *
 * Below 33 there is no system store, so the choice lives in SharedPreferences and
 * [wrap] re-applies it to the activity's base context. AppCompat backports the
 * same behaviour, but only inside `AppCompatActivity`: adopting it would mean
 * re-parenting a framework theme with hand-tuned splash attributes onto an
 * AppCompat ancestor and handing AppCompat ownership of night mode, which
 * MainActivity reads synchronously to avoid a first-frame flash.
 */
object UiLanguage {

    /**
     * The tags with a `values-xx/` folder behind them. Kept in step by hand with
     * `res/xml/locales_config.xml` — nothing checks the two against each other.
     */
    val SUPPORTED = listOf("en", "pt-BR")

    /** The stored tag, or empty for "follow the device". */
    fun tagOf(context: Context): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)
                ?.applicationLocales
                ?.takeIf { !it.isEmpty }
                ?.get(0)
                ?.toLanguageTag()
                .orEmpty()
        } else {
            storedTag(context)
        }

    /**
     * An empty [tag] hands the choice back to the device.
     *
     * On 33+ the framework recreates the activity itself. Below it, nothing is
     * watching, so the recreate is ours — and it has to happen after the write,
     * because `attachBaseContext` reads the file on the way back up.
     */
    fun set(activity: Activity, tag: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.getSystemService(LocaleManager::class.java)
                ?.applicationLocales = LocaleList.forLanguageTags(tag)
        } else {
            preferences(activity).edit().putString(KEY, tag).apply()
            activity.recreate()
        }
    }

    /**
     * Applies the stored tag to an activity's base context. API < 33 only — above
     * that the framework has already applied it, and wrapping again would pin the
     * locale to whatever was last written here.
     *
     * Also sets the process default, because card generation runs on
     * `AppContainer.scope` with no Activity in scope to read a configuration from.
     */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = storedTag(base)
        if (tag.isEmpty()) return base

        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return base.createConfigurationContext(configuration)
    }

    private fun storedTag(context: Context): String =
        preferences(context).getString(KEY, null).orEmpty()

    /**
     * The same file the rest of the app's preferences live in, opened directly
     * rather than through `Preferences`: [wrap] runs inside `attachBaseContext`,
     * before there is an application context to hand a container.
     */
    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences("vocabu_prefs", Context.MODE_PRIVATE)

    private const val KEY = "ui_language"
}
