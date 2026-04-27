/*
 * Copyright (c) 2026 You Apps
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.bnyro.translate.engine

import android.content.Context
import android.os.Build
import com.ai_model_hub.sdk.AiHubClient
import com.ai_model_hub.sdk.ConnectionState
import com.ai_model_hub.sdk.ModelAllowlist
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import net.youapps.translation_engines.ApiKeyState
import net.youapps.translation_engines.EngineSettingsProvider
import net.youapps.translation_engines.Language
import net.youapps.translation_engines.Translation
import net.youapps.translation_engines.TranslationEngine

class AiModelHubEngine(
    private val context: Context,
    settingsProvider: EngineSettingsProvider
) : TranslationEngine(settingsProvider) {

    override val name = "AiModelHub"
    override val defaultUrl = ""
    override val urlModifiable = false
    override val apiKeyState = ApiKeyState.DISABLED

    // No auto-detect: the prompt works best when the source language is explicit
    override val autoLanguageCode: String? = null
    override val supportedModels = ModelAllowlist.models.map { it.name }

    private var client: AiHubClient? = null

    override fun createOrRecreate(): TranslationEngine = apply {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@apply
        client?.disconnect()
        client = AiHubClient(context).also { it.connect() }
    }

    override suspend fun getLanguages(): List<Language> = SUPPORTED_LANGUAGES

    override suspend fun translate(query: String, source: String, target: String): Translation {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            throw RuntimeException("AiModelHub requires Android 12 (API 31) or higher.")
        }
        val hub =
            client ?: error("AiModelHubEngine not initialized — call createOrRecreate() first")
        val modelName = getSelectedModel() ?: error("No model selected for AiModelHub engine")

        // Wait up to 30 s for the service to connect
        val connected = withTimeoutOrNull(30_000) {
            hub.connectionState.first { it is ConnectionState.Connected }
        } ?: throw RuntimeException(
            "Could not connect to AiModelHub service. Is the AiModelHub app installed and a model enabled?"
        )
        check(connected is ConnectionState.Connected)

        // Load the model if it is not already loaded
        if (!hub.isModelLoaded(modelName)) {
            hub.loadModel(modelName)
        }

        val sourceName = LANGUAGE_NAMES[source] ?: source
        val targetName = LANGUAGE_NAMES[target] ?: target

        val prompt = buildString {
            append("Translate the following text")
            if (sourceName.isNotBlank()) append(" from $sourceName")
            append(" to $targetName")
            appendLine(". Output only the translation, nothing else.")
            appendLine()
            append(query)
        }

        // Reset the session so previous conversation context does not bleed in
        hub.resetSession(modelName)

        val result = StringBuilder()
        hub.sendMessage(modelName, prompt).collect { token -> result.append(token) }

        return Translation(translatedText = result.toString().trim())
    }

    companion object {
        private val SUPPORTED_LANGUAGES = listOf(
            Language("zh", "Chinese"),
            Language("en", "English"),
            Language("es", "Spanish"),
            Language("fr", "French"),
            Language("de", "German"),
            Language("ja", "Japanese"),
            Language("ko", "Korean"),
            Language("pt", "Portuguese"),
            Language("ru", "Russian"),
            Language("ar", "Arabic"),
            Language("it", "Italian"),
            Language("nl", "Dutch"),
            Language("pl", "Polish"),
            Language("sv", "Swedish"),
            Language("da", "Danish"),
            Language("fi", "Finnish"),
            Language("nb", "Norwegian"),
            Language("tr", "Turkish"),
            Language("cs", "Czech"),
            Language("hu", "Hungarian"),
            Language("ro", "Romanian"),
            Language("uk", "Ukrainian"),
            Language("id", "Indonesian"),
            Language("vi", "Vietnamese"),
            Language("th", "Thai"),
            Language("hi", "Hindi"),
        )

        /** Maps ISO 639-1 codes to full English language names used in the translation prompt. */
        private val LANGUAGE_NAMES = SUPPORTED_LANGUAGES.associate { it.code to it.name }
    }
}
