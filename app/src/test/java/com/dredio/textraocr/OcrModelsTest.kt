package com.dredio.textraocr

import com.dredio.textraocr.ocr.LineResult
import com.dredio.textraocr.ocr.SerializableRect
import com.dredio.textraocr.ocr.ReviewLevel
import com.dredio.textraocr.ocr.toReviewLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class OcrModelsTest {

    @Test
    fun confidenceMapsToExpectedReviewLevel() {
        assertEquals(ReviewLevel.UNKNOWN, (null as Float?).toReviewLevel())
        assertEquals(ReviewLevel.LOW, 0.10f.toReviewLevel())
        assertEquals(ReviewLevel.MEDIUM, 0.60f.toReviewLevel())
        assertEquals(ReviewLevel.MEDIUM, 0.79f.toReviewLevel())
        assertEquals(ReviewLevel.HIGH, 0.80f.toReviewLevel())
    }

    @Test
    fun lineResultNormalizesConfidenceIntoRange() {
        val low = LineResult(
            id = "line-1",
            text = "Hello",
            confidence = -0.5f,
            boundingBox = SerializableRect(0, 0, 10, 10)
        )
        val high = LineResult(
            id = "line-2",
            text = "World",
            confidence = 1.5f,
            boundingBox = SerializableRect(0, 0, 10, 10)
        )

        assertEquals(0f, low.normalizedConfidence, 0f)
        assertEquals(1f, high.normalizedConfidence, 0f)
    }
}