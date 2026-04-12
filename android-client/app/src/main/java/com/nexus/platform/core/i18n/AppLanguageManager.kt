package com.nexus.platform.core.i18n

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.annotation.StringRes
import com.nexus.platform.R
import java.util.Locale

enum class AppLanguage(val tag: String, @StringRes val labelRes: Int) {
    TraditionalChinese("zh-TW", R.string.language_traditional_chinese),
    SimplifiedChinese("zh-CN", R.string.language_simplified_chinese),
    English("en", R.string.language_english);

    companion object {
        fun fromTag(tag: String): AppLanguage {
            return entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: SimplifiedChinese
        }

        fun fromLocale(locale: Locale): AppLanguage {
            val language = locale.language.lowercase(Locale.ROOT)
            if (language == "zh") {
                val script = locale.script.lowercase(Locale.ROOT)
                val country = locale.country.uppercase(Locale.ROOT)
                return if (script == "hant" || country in setOf("TW", "HK", "MO")) {
                    TraditionalChinese
                } else {
                    SimplifiedChinese
                }
            }

            return English
        }
    }
}

object AppLanguageManager {
    private const val PREF_NAME = "app_language_pref"
    private const val KEY_LANGUAGE = "language_tag"
    private const val KEY_LANGUAGE_EXPLICITLY_SELECTED = "language_explicitly_selected"
    private const val LEGACY_AUTO_DEFAULT_LANGUAGE_TAG = "zh-TW"

    fun ensureInitialized(context: Context): Context {
        migrateLegacyDefault(context)
        return wrap(context)
    }

    fun currentLanguage(context: Context): AppLanguage {
        return currentLanguageOrNull(context) ?: systemLanguage(context)
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.tag)
            .putBoolean(KEY_LANGUAGE_EXPLICITLY_SELECTED, true)
            .apply()
    }

    fun wrap(context: Context): Context {
        migrateLegacyDefault(context)
        val language = currentLanguageOrNull(context) ?: systemLanguage(context)
        return wrapWithLanguage(context, language)
    }

    private fun wrapWithLanguage(context: Context, language: AppLanguage): Context {
        val locale = Locale.forLanguageTag(language.tag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        val wrapped = context.createConfigurationContext(config)
        @Suppress("DEPRECATION")
        wrapped.resources.updateConfiguration(config, wrapped.resources.displayMetrics)
        return wrapped
    }

    private fun currentLanguageOrNull(context: Context): AppLanguage? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedTag = prefs.getString(KEY_LANGUAGE, null)
        return savedTag?.takeIf { it.isNotBlank() }?.let(AppLanguage::fromTag)
    }

    private fun systemLanguage(context: Context): AppLanguage {
        val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
        return AppLanguage.fromLocale(locale)
    }

    private fun migrateLegacyDefault(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val explicitlySelected = prefs.getBoolean(KEY_LANGUAGE_EXPLICITLY_SELECTED, false)
        val savedTag = prefs.getString(KEY_LANGUAGE, null)
        if (!explicitlySelected && savedTag == LEGACY_AUTO_DEFAULT_LANGUAGE_TAG) {
            prefs.edit().remove(KEY_LANGUAGE).apply()
        }
    }
}
