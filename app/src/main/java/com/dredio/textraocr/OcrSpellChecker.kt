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

        session.getSentenceSuggestions(originalWords.map { TextInfo(it) }.toTypedArray(), 5)
    }

    override fun onGetSuggestions(results: Array<SuggestionsInfo>?) {
        // Word-level suggestions are not used directly when using sentence suggestions.
    }

    override fun onGetSentenceSuggestions(results: Array<SentenceSuggestionsInfo>?) {
        val corrections = extractSpellCorrections(originalWords, results)
        if (corrections.isEmpty()) {
            postResult(originalText)
            return
        }

        postResult(applySpellCorrections(originalText, corrections))
    }

    private fun postResult(result: String) {
        mainHandler.post {
            onResult(result)
        }
    }
}
