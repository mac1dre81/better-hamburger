package com.dredio.textraocr

import android.view.textservice.SentenceSuggestionsInfo

internal data class SpellCorrection(
    val original: String,
    val replacement: String
)

internal fun applySpellCorrections(
    originalText: String,
    corrections: List<SpellCorrection>
): String {
    var correctedText = originalText

    corrections.forEach { correction ->
        correctedText = correctedText.replaceFirst(
            Regex("\\b${Regex.escape(correction.original)}\\b"),
            correction.replacement
        )
    }

    return correctedText
}

internal fun extractSpellCorrections(
    originalWords: List<String>,
    results: Array<SentenceSuggestionsInfo>?
): List<SpellCorrection> {
    if (results.isNullOrEmpty()) {
        return emptyList()
    }

    return buildList {
        results.forEach { sentenceSuggestions ->
            val suggestionsCount = sentenceSuggestions.suggestionsCount
            for (wordIndex in 0 until suggestionsCount) {
                val suggestionsInfo = sentenceSuggestions.getSuggestionsInfoAt(wordIndex)
                if (suggestionsInfo.suggestionsCount <= 0) continue

                val original = originalWords.getOrNull(wordIndex) ?: continue
                val candidate = suggestionsInfo.getSuggestionAt(0)
                if (candidate.isNullOrBlank()) continue
                if (candidate.equals(original, ignoreCase = true)) continue

                add(SpellCorrection(original = original, replacement = candidate))
            }
        }
    }
}