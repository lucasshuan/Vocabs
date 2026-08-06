package io.github.lucasshuan.vocabu.ui.language

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * The interface's language — not the cards', which lives in
 * [io.github.lucasshuan.vocabu.shared.Preferences].
 *
 * On API 33+ `LocaleManager` is the store, and it is settable from system
 * Settings too, so mirroring it here would go stale the first time it changes
 * there. Below 33 it lives in SharedPreferences and [wrap] re-applies it.
 *
 * AppCompat backports this, but only inside `AppCompatActivity`: adopting it
 * means re-parenting a framework theme with hand-tuned splash attributes and
 * handing AppCompat night mode, which MainActivity reads synchronously to avoid
 * a first-frame flash.
 */
object UiLanguage {

    /**
     * The tags with a `values-xx/` folder. Kept in step with
     * `res/xml/locales_config.xml` by hand — nothing checks the two.
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
     * Below 33 the recreate is ours, and has to follow the write:
     * `attachBaseContext` reads the file on the way back up.
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
     * API < 33 only: above that the framework has applied it already, and
     * wrapping again pins the locale to whatever was last written here.
     *
     * Also sets the process default — card generation runs on
     * `AppContainer.scope`, with no Activity to read a configuration from.
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
     * The preferences file, opened directly: [wrap] runs inside
     * `attachBaseContext`, before there is an application context to hand around.
     */
    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences("vocabu_prefs", Context.MODE_PRIVATE)

    private const val KEY = "ui_language"
}
