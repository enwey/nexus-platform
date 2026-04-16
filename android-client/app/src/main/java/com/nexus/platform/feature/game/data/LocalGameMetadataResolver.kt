package com.nexus.platform.feature.game.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nexus.platform.core.i18n.AppLanguageManager
import com.nexus.platform.domain.model.GameItem
import java.io.File

class LocalGameMetadataResolver(
    private val context: Context,
    private val gson: Gson = Gson()
) {
    private val gameManager = GameManager(context)

    fun merge(game: GameItem): GameItem {
        val languageTag = AppLanguageManager.currentLanguage(context).tag
        val remoteName = resolveLocalizedValue(languageTag, game.localizedNames, game.name)
        val remoteDescription = resolveLocalizedValue(languageTag, game.localizedDescriptions, game.description)

        val manifest = readManifest(game.id) ?: return game.copy(
            name = remoteName,
            description = remoteDescription
        )

        val iconUri = manifest.iconPath
            ?.takeIf { it.isNotBlank() }
            ?.let { File(gameManager.getGameDir(game.id), it) }
            ?.takeIf { it.exists() && it.isFile }
            ?.toURI()
            ?.toString()
            .orEmpty()

        return game.copy(
            name = resolveLocalizedValue(languageTag, manifest.localizedNames, manifest.name.ifBlank { remoteName }),
            description = resolveLocalizedValue(languageTag, manifest.localizedDescriptions, manifest.description.ifBlank { remoteDescription }),
            iconUrl = iconUri.ifBlank { game.iconUrl }
        )
    }

    private fun readManifest(gameId: String): ParsedManifest? {
        val manifestFile = File(gameManager.getGameDir(gameId), "manifest.json")
        if (!manifestFile.exists()) return null

        val root = runCatching {
            gson.fromJson(manifestFile.readText(Charsets.UTF_8), JsonObject::class.java)
        }.getOrNull() ?: return null

        val metadata = root.getAsJsonObject("metadata")
        val locales = metadata?.getAsJsonObject("locales") ?: root.getAsJsonObject("locales")
        return ParsedManifest(
            name = root.string("name"),
            description = root.string("description"),
            iconPath = metadata?.string("icon")?.ifBlank { root.string("icon") },
            localizedNames = locales.extractLocalizedField("name"),
            localizedDescriptions = locales.extractLocalizedField("description")
        )
    }

    private fun resolveLocalizedValue(
        languageTag: String,
        values: Map<String, String>,
        fallback: String
    ): String {
        if (values.isEmpty()) return fallback
        val normalized = languageTag.lowercase()
        val languageOnly = normalized.substringBefore('-')
        return values.entries.firstOrNull { it.key.lowercase() == normalized }?.value
            ?: values.entries.firstOrNull { it.key.lowercase() == languageOnly }?.value
            ?: values.entries.firstOrNull { it.key.lowercase().startsWith("$languageOnly-") }?.value
            ?: values["default"]
            ?: fallback
    }

    private fun JsonObject?.extractLocalizedField(fieldName: String): Map<String, String> {
        if (this == null) return emptyMap()
        return entrySet().mapNotNull { (tag, value) ->
            val text = value?.asJsonObject?.string(fieldName).orEmpty()
            if (text.isBlank()) null else tag to text
        }.toMap()
    }

    private fun JsonObject.string(key: String): String {
        return get(key)?.takeIf { !it.isJsonNull }?.asString.orEmpty()
    }

    private data class ParsedManifest(
        val name: String,
        val description: String,
        val iconPath: String?,
        val localizedNames: Map<String, String>,
        val localizedDescriptions: Map<String, String>
    )
}
