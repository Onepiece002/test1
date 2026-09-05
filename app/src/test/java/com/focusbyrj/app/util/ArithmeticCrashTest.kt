package com.focusbyrj.app.util

import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ArithmeticCrashTest {
    @Test
    fun testGenerate() {
        for (i in 0 until 1000) {
            val qEasy = ArithmeticEngine.generateQuestion(ArithmeticDifficulty.EASY)
            assertNotNull(qEasy.title)
            assertNotNull(qEasy.questionText)
            assertTrue(qEasy.options.isNotEmpty())
            assertTrue(qEasy.correctIndex in qEasy.options.indices)

            val qMed = ArithmeticEngine.generateQuestion(ArithmeticDifficulty.MEDIUM)
            assertNotNull(qMed.title)
            assertNotNull(qMed.questionText)
            assertTrue(qMed.options.isNotEmpty())
            assertTrue(qMed.correctIndex in qMed.options.indices)

            val qHard = ArithmeticEngine.generateQuestion(ArithmeticDifficulty.HARD)
            assertNotNull(qHard.title)
            assertNotNull(qHard.questionText)
            assertTrue(qHard.options.isNotEmpty())
            assertTrue(qHard.correctIndex in qHard.options.indices)
        }
    }

    @Test
    fun testLottieAssets() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val assets = listOf("cat_morning.lottie", "cat_evening.lottie", "cat_error.lottie", "cat_angry.lottie", "cat_action.lottie")
        for (asset in assets) {
            val res = com.airbnb.lottie.LottieCompositionFactory.fromAssetSync(context, asset)
            println("Asset: $asset, exception: ${res.exception}")
            assertNull("Failed to load $asset: ${res.exception}", res.exception)
            assertNotNull("Composition was null for $asset", res.value)
        }
    }
}
