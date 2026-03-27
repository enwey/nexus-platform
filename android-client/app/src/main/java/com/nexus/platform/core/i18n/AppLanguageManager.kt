package com.nexus.platform.core.i18n

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import com.nexus.platform.R
import java.util.Locale

enum class AppLanguage(val tag: String, @StringRes val labelRes: Int) {
    TraditionalChinese("zh-TW", R.string.language_traditional_chinese),
    SimplifiedChinese("zh-CN", R.string.language_simplified_chinese),
    English("en", R.string.language_english);

    companion object {
        fun fromTag(tag: String): AppLanguage {
            return entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: TraditionalChinese
        }
    }
}

object AppLanguageManager {
    private const val PREF_NAME = "app_language_pref"
    private const val KEY_LANGUAGE = "language_tag"
    private val defaultLanguage = AppLanguage.TraditionalChinese

    fun ensureInitialized(context: Context): Context {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedTag = prefs.getString(KEY_LANGUAGE, null)
        val language = if (savedTag.isNullOrBlank()) {
            prefs.edit().putString(KEY_LANGUAGE, defaultLanguage.tag).apply()
            defaultLanguage
        } else {
            AppLanguage.fromTag(savedTag)
        }
        return wrapWithLanguage(context, language)
    }

    fun currentLanguage(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedTag = prefs.getString(KEY_LANGUAGE, defaultLanguage.tag).orEmpty()
        return AppLanguage.fromTag(savedTag)
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.tag)
            .apply()
    }

    fun wrap(context: Context): Context {
        val language = currentLanguage(context)
        return wrapWithLanguage(context, language)
    }

    private fun wrapWithLanguage(context: Context, language: AppLanguage): Context {
        val locale = Locale.forLanguageTag(language.tag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
