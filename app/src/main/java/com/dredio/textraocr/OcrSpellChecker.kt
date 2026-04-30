package com.dredio.textraocr

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SpellCheckerSession.SpellCheckerSessionListener
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import java.util.Locale

class OcrSpellChecker(
    context: Context,
    private val onResult: (String) -> Unit
) : SpellCheckerSessionListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val textServicesManager =
        context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as? TextServicesManager
    private val session: SpellCheckerSession? = textServicesManager?.newSpellCheckerSession(
        null,
        Locale.getDefault(),
        this,
        true
    )

    private var originalText: String = ""
    private var originalWords: List<String> = emptyList()

    fun close() {
        session?.close()
    }

    fun suggestCorrections(text: String) {
        originalText = text
        originalWords = text
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(50)

        if (originalWords.isEmpty() || session == null) {
            postResult(text)
            return
        }

        session.getSuggestions(originalWords.map { TextInfo(it) }.toTypedArray(), 5)
    }

    override fun onGetSuggestions(results: Array<SuggestionsInfo>?) {
        if (results.isNullOrEmpty()) {
            postResult(originalText)
            return
        }

        var correctedText = originalText
        results.forEachIndexed { index, suggestionsInfo ->
            val original = originalWords.getOrNull(index) ?: return@forEachIndexed
            if (suggestionsInfo.suggestionsCount <= 0) return@forEachIndexed

            val candidate = suggestionsInfo.getSuggestionAt(0)
            if (candidate.isNullOrBlank()) return@forEachIndexed
            if (candidate.equals(original, ignoreCase = true)) return@forEachIndexed

            correctedText = correctedText.replaceFirst(
                Regex("\\b${Regex.escape(original)}\\b"),
                candidate
            )
        }

        postResult(correctedText)
    }

    override fun onGetSentenceSuggestions(results: Array<SentenceSuggestionsInfo>?) {
        // Not used; word-level suggestions are sufficient for basic OCR correction.
    }

    private fun postResult(result: String) {
        mainHandler.post {
            onResult(result)
        }
    }
}
