package com.dredio.textraocr

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrSpellCorrectionTest {

    @Test
    fun applySpellCorrectionsReplacesOnlyWholeWords() {
        val corrected = applySpellCorrections(
            originalText = "Ths text has a smple word and thisSubstring should stay.",
            corrections = listOf(
                SpellCorrection(original = "Ths", replacement = "This"),
                SpellCorrection(original = "smple", replacement = "simple")
            )
        )

        assertEquals(
            "This text has a simple word and thisSubstring should stay.",
            corrected
        )
    }

    @Test
    fun applySpellCorrectionsPreservesUnmatchedText() {
        val corrected = applySpellCorrections(
            originalText = "Nothing changes here.",
            corrections = emptyList()
        )

        assertEquals("Nothing changes here.", corrected)
    }
}