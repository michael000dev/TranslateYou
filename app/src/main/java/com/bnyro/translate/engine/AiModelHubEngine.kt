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

import android.os.Build
import android.util.Log
import com.ai_model_hub.sdk.ModelAllowlist
import com.ai_model_hub.sdk.functional.TranslateAvailableLanguage
import net.youapps.translation_engines.ApiKeyState
import net.youapps.translation_engines.EngineSettingsProvider
import net.youapps.translation_engines.Language
import net.youapps.translation_engines.Translation
import net.youapps.translation_engines.TranslationEngine

private const val TAG = "AiModelHubEngine"

class AiModelHubEngine(
    settingsProvider: EngineSettingsProvider
) : TranslationEngine(settingsProvider) {

    override val name = "AiModelHub"
    override val defaultUrl = ""
    override val urlModifiable = false
    override val apiKeyState = ApiKeyState.DISABLED

    override val autoLanguageCode = ""
    override val supportedModels = ModelAllowlist.models.map { it.name }

    override fun createOrRecreate(): TranslationEngine = apply {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@apply
    }

    override suspend fun getLanguages(): List<Language> = SUPPORTED_LANGUAGES

    override suspend fun translate(query: String, source: String, target: String): Translation {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            throw RuntimeException("AiModelHub requires Android 12 (API 31) or higher.")
        }
        val modelName = getSelectedModel() ?: error("No model selected for AiModelHub engine")

        val sourceName = LANGUAGE_NAMES[source] ?: source
        val targetName = LANGUAGE_NAMES[target] ?: target

        Log.i(TAG, "Translating with model '$modelName' from '$sourceName' to '$targetName'")
        val result = com.ai_model_hub.sdk.functional.translate(
            modelName = modelName,
            text = query,
            targetLanguage = targetName,
            sourceLanguage = sourceName
        )

        return Translation(translatedText = result.trim())
    }

    companion object {
        private val SUPPORTED_LANGUAGES = listOf(
            Language("zh", TranslateAvailableLanguage.CHINESE),
            Language("en", TranslateAvailableLanguage.ENGLISH),
            Language("es", TranslateAvailableLanguage.SPANISH),
            Language("fr", TranslateAvailableLanguage.FRENCH),
            Language("de", TranslateAvailableLanguage.GERMAN),
            Language("ja", TranslateAvailableLanguage.JAPANESE),
            Language("ko", TranslateAvailableLanguage.KOREAN),
            Language("pt", TranslateAvailableLanguage.PORTUGUESE),
            Language("ru", TranslateAvailableLanguage.RUSSIAN),
            Language("ar", TranslateAvailableLanguage.ARABIC),
            Language("it", TranslateAvailableLanguage.ITALIAN),
            Language("nl", TranslateAvailableLanguage.DUTCH),
            Language("pl", TranslateAvailableLanguage.POLISH),
            Language("sv", TranslateAvailableLanguage.SWEDISH),
            Language("da", TranslateAvailableLanguage.DANISH),
            Language("fi", TranslateAvailableLanguage.FINNISH),
            Language("nb", TranslateAvailableLanguage.NORWEGIAN),
            Language("tr", TranslateAvailableLanguage.TURKISH),
            Language("cs", TranslateAvailableLanguage.CZECH),
            Language("hu", TranslateAvailableLanguage.HUNGARIAN),
            Language("ro", TranslateAvailableLanguage.ROMANIAN),
            Language("uk", TranslateAvailableLanguage.UKRAINIAN),
            Language("id", TranslateAvailableLanguage.INDONESIAN),
            Language("vi", TranslateAvailableLanguage.VIETNAMESE),
            Language("th", TranslateAvailableLanguage.THAI),
            Language("hi", TranslateAvailableLanguage.HINDI),
        )

        /** Maps ISO 639-1 codes to full English language names used in the translation prompt. */
        private val LANGUAGE_NAMES = SUPPORTED_LANGUAGES.associate { it.code to it.name }
    }
}
