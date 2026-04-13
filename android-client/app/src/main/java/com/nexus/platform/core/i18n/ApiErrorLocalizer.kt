package com.nexus.platform.core.i18n

import android.content.Context
import com.nexus.platform.R

object ApiErrorLocalizer {
    private val cjkRegex = Regex("[\\p{IsHan}]")
    private val latinRegex = Regex("[A-Za-z]")

    fun localize(context: Context, rawMessage: String?, fallbackRes: Int): String {
        if (rawMessage.isNullOrBlank()) {
            return context.getString(fallbackRes)
        }
        val message = rawMessage.trim()
        val mappedRes = mapKnownMessage(message)
        if (mappedRes != null) {
            return context.getString(mappedRes)
        }

        val localeLanguage = context.resources.configuration.locales[0].language
        val isChineseLocale = localeLanguage.startsWith("zh", ignoreCase = true)
        val hasCjk = cjkRegex.containsMatchIn(message)
        val hasLatin = latinRegex.containsMatchIn(message)
        val isCrossLanguageMismatch = (isChineseLocale && hasLatin && !hasCjk) || (!isChineseLocale && hasCjk)
        if (isCrossLanguageMismatch) {
            return context.getString(fallbackRes)
        }
        return message
    }

    private fun mapKnownMessage(message: String): Int? {
        val text = message.lowercase()
        return when {
            text.contains("email is required") -> R.string.login_error_email_required
            text.contains("account is required") -> R.string.login_error_email_required
            text.contains("invalid email format") || text == "invalid email" -> R.string.auth_error_invalid_email
            text.contains("email already registered") -> R.string.register_error_email_registered
            text.contains("email is not registered") -> R.string.register_error_email_not_registered
            text.contains("verification code is invalid or expired") -> R.string.auth_error_code_invalid_or_expired
            text.contains("invalid parameters") -> R.string.common_error_invalid_parameters
            text.contains("user not found") -> R.string.common_error_user_not_found
            text.contains("incorrect password") -> R.string.login_error_failed
            text.contains("email does not match current account") -> R.string.common_error_email_mismatch
            text.contains("password must be at least 8 chars") -> R.string.change_password_error_too_short
            text.contains("unable to resolve host")
                    || text.contains("failed to connect")
                    || text.contains("timeout")
                    || text.contains("timed out")
                    || text.contains("connection reset")
                    || text.contains("network is unreachable") -> R.string.common_error_network
            text.contains("internal server error")
                    || text.contains("http 500")
                    || text.contains("http 502")
                    || text.contains("http 503")
                    || text.contains("http 504") -> R.string.common_error_server
            else -> null
        }
    }
}
