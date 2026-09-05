package com.focusbyrj.app.util

import org.junit.Test
import org.junit.Assert.*

class ArithmeticCrashTest {
    @Test
    fun testGenerate() {
        for (i in 0 until 100) {
            ArithmeticEngine.generateQuestion(ArithmeticDifficulty.EASY)
            ArithmeticEngine.generateQuestion(ArithmeticDifficulty.MEDIUM)
            ArithmeticEngine.generateQuestion(ArithmeticDifficulty.HARD)
        }
    }
}
