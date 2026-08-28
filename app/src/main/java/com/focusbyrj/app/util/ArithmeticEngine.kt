package com.focusbyrj.app.util

import kotlin.math.sqrt
import kotlin.random.Random

enum class ArithmeticDifficulty {
    EASY,
    MEDIUM,
    HARD
}

data class ArithmeticQuestion(
    val title: String,
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

object ArithmeticEngine {

    fun generateQuestion(difficulty: ArithmeticDifficulty): ArithmeticQuestion {
        val topic = if (difficulty == ArithmeticDifficulty.EASY) {
            Random.nextInt(3) // 0: Speed Math, 1: Simplification, 2: Series
        } else {
            Random.nextInt(1, 4) // 1: Simplification, 2: Series, 3: Quadratics
        }

        return when (topic) {
            0 -> generateSpeedMath()
            1 -> generateSimplification(difficulty)
            2 -> generateNumberSeries(difficulty)
            else -> generateQuadratic(difficulty)
        }
    }

    // =========================================================================
    // 1. SPEED MATH (EASY)
    // =========================================================================
    private fun generateSpeedMath(): ArithmeticQuestion {
        val type = Random.nextInt(2)
        if (type == 0) {
            val num = Random.nextInt(15, 60)
            val answer = (num * num).toDouble()
            return buildQuestion(
                title = "Speed Math",
                question = "What is the square of $num? ($num²)",
                answer = answer,
                distractorLogic = { it + (Random.nextInt(-3, 4) * 10 + Random.nextInt(-2, 3)).toDouble() },
                explanation = "💡 Tip: Learn Vedic math base methods for squaring numbers fast. $num × $num = ${formatNumber(answer)}."
            )
        } else {
            val roots = (15..55).toList()
            val root = roots.random()
            val square = root * root
            return buildQuestion(
                title = "Speed Math",
                question = "Find the value of √$square",
                answer = root.toDouble(),
                distractorLogic = { it + (Random.nextInt(-5, 6).takeIf { it != 0 } ?: 1).toDouble() },
                explanation = "💡 Tip: Check the unit digit of $square (${square % 10}). √$square = $root."
            )
        }
    }

    // =========================================================================
    // 2. SIMPLIFICATION (Fixed Integer Division & Exponents)
    // =========================================================================
    private data class MagicFraction(val display: String, val num: Int, val den: Int, val tip: String)

    private val bankFractions = listOf(
        MagicFraction("14.28%", 1, 7, "1/7"),
        MagicFraction("16.66%", 1, 6, "1/6"),
        MagicFraction("33.33%", 1, 3, "1/3"),
        MagicFraction("37.5%", 3, 8, "3/8"),
        MagicFraction("62.5%", 5, 8, "5/8"),
        MagicFraction("83.33%", 5, 6, "5/6"),
        MagicFraction("11.11%", 1, 9, "1/9"),
        MagicFraction("9.09%", 1, 11, "1/11"),
        MagicFraction("12.5%", 1, 8, "1/8"),
        MagicFraction("28.56%", 2, 7, "2/7")
    )

    private fun generateSimplification(difficulty: ArithmeticDifficulty): ArithmeticQuestion {
        val pattern = Random.nextInt(8)
        return when (pattern) {
            0 -> {
                val fraction = bankFractions.random()
                val multiplier = when (difficulty) {
                    ArithmeticDifficulty.EASY -> Random.nextInt(5, 15)
                    ArithmeticDifficulty.MEDIUM -> Random.nextInt(15, 40)
                    ArithmeticDifficulty.HARD -> Random.nextInt(40, 80)
                }
                val base = multiplier * fraction.den * 10
                val part1Answer = (base / fraction.den) * fraction.num
                val addition = Random.nextInt(10, 50) * 5
                val isAddition = Random.nextBoolean()
                val finalAnswer = (if (isAddition) part1Answer + addition else part1Answer - addition).toDouble()
                val opStr = if (isAddition) "+" else "-"
                
                buildQuestion(
                    title = "Simplification",
                    question = "${fraction.display} of $base $opStr $addition = ?",
                    answer = finalAnswer,
                    distractorLogic = { it + (Random.nextInt(-4, 5).takeIf { n -> n != 0 } ?: 2) * 10 },
                    explanation = "💡 Tip: ${fraction.display} = ${fraction.tip}. (${fraction.num}/${fraction.den}) × $base = $part1Answer. $part1Answer $opStr $addition = ${formatNumber(finalAnswer)}."
                )
            }
            1 -> {
                val baseSq = Random.nextInt(12, 35)
                val baseCb = Random.nextInt(5, 15)
                val sq = baseSq * baseSq
                val cb = baseCb * baseCb * baseCb
                val isAddition = Random.nextBoolean()
                val finalAnswer = (if (isAddition) sq + cb else Math.abs(sq - cb)).toDouble()
                val opStr = if (isAddition) "+" else "-"
                val qStr = if (!isAddition && sq < cb) "$baseCb³ - $baseSq² = ?" else "$baseSq² $opStr $baseCb³ = ?"
                
                buildQuestion(
                    title = "Simplification",
                    question = qStr,
                    answer = finalAnswer,
                    distractorLogic = { it + (Random.nextInt(-5, 6).takeIf { n -> n != 0 } ?: 2) * 10 },
                    explanation = "💡 Step 1: $baseSq² = $sq. Step 2: $baseCb³ = $cb. Result = ${formatNumber(finalAnswer)}."
                )
            }
            2 -> {
                val a = Random.nextInt(15, 45)
                val b = Random.nextInt(11, 25)
                val c = Random.nextInt(20, 200)
                val finalAnswer = ((a * b) - c).toDouble()
                buildQuestion(
                    title = "Simplification",
                    question = "$a × $b - $c = ?",
                    answer = finalAnswer,
                    distractorLogic = { it + (Random.nextInt(-4, 5).takeIf { n -> n != 0 } ?: 1) * 10 },
                    explanation = "💡 Split & multiply: $a × $b = ${a*b}. Then ${a*b} - $c = ${formatNumber(finalAnswer)}."
                )
            }
            3 -> {
                // Fixed Integer Division: Ensure percentages multiply cleanly to multiple of 100
                val pctA = Random.nextInt(2, 9) * 10
                val valA = Random.nextInt(2, 12) * 100 // Guarantees %100 == 0
                val pctB = listOf(15, 25, 35, 45, 55, 65, 75).random()
                val valB = Random.nextInt(1, 8) * 20 // 20 * X * pctB guarantees multiple of 100
                val finalAnswer = ((pctA * valA / 100) + (pctB * valB / 100)).toDouble()
                buildQuestion(
                    title = "Simplification",
                    question = "$pctA% of $valA + $pctB% of $valB = ?",
                    answer = finalAnswer,
                    distractorLogic = { it + (Random.nextInt(-3, 4).takeIf { n -> n != 0 } ?: 1) * 5 },
                    explanation = "💡 $pctA% of $valA = ${pctA * valA / 100}. $pctB% of $valB = ${pctB * valB / 100}. Sum = ${formatNumber(finalAnswer)}."
                )
            }
            4 -> {
                val root1 = Random.nextInt(12, 45)
                val root2 = Random.nextInt(10, 35)
                val extra = Random.nextInt(10, 50)
                val sq1 = root1 * root1
                val sq2 = root2 * root2
                val isAddition = Random.nextBoolean()
                val finalAnswer = (if (isAddition) (root1 * root2) + extra else (root1 * root2) - extra).toDouble()
                val opStr = if (isAddition) "+" else "-"
                buildQuestion(
                    title = "Simplification",
                    question = "√$sq1 × √$sq2 $opStr $extra = ?",
                    answer = finalAnswer,
                    distractorLogic = { it + (Random.nextInt(-4, 5).takeIf { n -> n != 0 } ?: 1) * 10 },
                    explanation = "💡 √$sq1 = $root1. √$sq2 = $root2. ($root1 × $root2) $opStr $extra = ${formatNumber(finalAnswer)}."
                )
            }
            5 -> {
                val c = Random.nextInt(4, 16)
                val b = c * Random.nextInt(2, 8) // Guaranteed multiple
                val a = Random.nextInt(15, 55)
                val d = Random.nextInt(20, 150)
                val finalAnswer = (a * (b / c) + d).toDouble()
                buildQuestion(
                    title = "Simplification",
                    question = "$a × $b ÷ $c + $d = ?",
                    answer = finalAnswer,
                    distractorLogic = { it + (Random.nextInt(-5, 6).takeIf { n -> n != 0 } ?: 1) * 10 },
                    explanation = "💡 BODMAS Rule: First divide $b ÷ $c = ${b/c}. Then multiply $a × ${b/c} = ${a * (b/c)}. Add $d = ${formatNumber(finalAnswer)}."
                )
            }
            6 -> {
                // Fixed Exponents logic (Prevent negative/zero powers on division)
                val base = listOf(2, 3, 4, 5).random()
                var p1 = Random.nextInt(3, 7)
                var p2 = Random.nextInt(2, 5)
                val isMul = Random.nextBoolean()
                
                if (!isMul && p1 <= p2) {
                    val temp = p1
                    p1 = p2
                    p2 = temp
                    if (p1 == p2) p1++ 
                }
                
                val finalPower = if (isMul) p1 + p2 else p1 - p2
                val opStr = if (isMul) "×" else "÷"
                buildQuestion(
                    title = "Simplification (Exponents)",
                    question = "$base^$p1 $opStr $base^$p2 = $base^?",
                    answer = finalPower.toDouble(),
                    distractorLogic = { it + (Random.nextInt(-2, 3).takeIf { n -> n != 0 } ?: 1) },
                    explanation = "💡 Law of Exponents: When bases are the same, ${if (isMul) "add" else "subtract"} the powers. $p1 ${if (isMul) "+" else "-"} $p2 = $finalPower."
                )
            }
            else -> {
                val den1 = Random.nextInt(3, 9)
                val num1 = Random.nextInt(1, den1)
                val den2 = Random.nextInt(4, 12)
                val num2 = Random.nextInt(1, den2)
                val lcm = den1 * den2
                val e = lcm * Random.nextInt(2, 6)
                val finalAnswer = ((num1 * num2 * e) / lcm).toDouble()
                buildQuestion(
                    title = "Simplification",
                    question = "($num1/$den1) × ($num2/$den2) × $e = ?",
                    answer = finalAnswer,
                    distractorLogic = { it + (Random.nextInt(-3, 4).takeIf { n -> n != 0 } ?: 1) * 2 },
                    explanation = "💡 Cancel out denominators: $den1 × $den2 = $lcm. $e ÷ $lcm = ${e / lcm}. Multiply numerators: $num1 × $num2 × ${e / lcm} = ${formatNumber(finalAnswer)}."
                )
            }
        }
    }

    // =========================================================================
    // 3. NUMBER SERIES (Fixed Decimal Sequences)
    // =========================================================================
    private fun generateNumberSeries(difficulty: ArithmeticDifficulty): ArithmeticQuestion {
        val length = 5
        val sequence = mutableListOf<Double>()
        var start = Random.nextInt(2, 15).toDouble()
        sequence.add(start)

        var explanationStr = ""
        val patternChoice = Random.nextInt(7)
           
        when (difficulty) {
            ArithmeticDifficulty.EASY -> {
                when (patternChoice) {
                    0 -> {
                        val diffStart = Random.nextInt(1, 8)
                        val diffs = (diffStart until diffStart + length).map { (it * it).toDouble() }
                        for (i in 0 until length - 1) sequence.add(sequence.last() + diffs[i])
                        explanationStr = "Difference is consecutive squares (+${formatNumber(diffs[0])}, +${formatNumber(diffs[1])}, +${formatNumber(diffs[2])})."
                    }
                    1 -> {
                        val mult = Random.nextInt(3, 12).toDouble()
                        for (i in 1 until length) sequence.add(sequence.last() + (mult * i))
                        explanationStr = "Difference is multiples of ${formatNumber(mult)} (+${formatNumber(mult)}, +${formatNumber(mult*2)}, +${formatNumber(mult*3)})."
                    }
                    2 -> {
                        val primes = listOf(2.0, 3.0, 5.0, 7.0, 11.0, 13.0, 17.0, 19.0, 23.0, 29.0, 31.0, 37.0, 41.0, 43.0)
                        val pStart = Random.nextInt(0, primes.size - length)
                        for (i in 0 until length - 1) sequence.add(sequence.last() + primes[pStart + i])
                        explanationStr = "Difference is consecutive prime numbers (+${formatNumber(primes[pStart])}, +${formatNumber(primes[pStart+1])})."
                    }
                    3 -> {
                        val diff = Random.nextInt(12, 35).toDouble()
                        for (i in 1 until length) sequence.add(sequence.last() + diff)
                        explanationStr = "Constant difference of +${formatNumber(diff)}."
                    }
                    4 -> {
                        val isCube = Random.nextBoolean()
                        val startNum = Random.nextInt(5, 15)
                        sequence.clear()
                        for (i in 0 until length) {
                            val n = (startNum + i).toDouble()
                            sequence.add(if (isCube) n*n*n else n*n)
                        }
                        explanationStr = "Terms are consecutive ${if (isCube) "cubes" else "squares"} of $startNum, ${startNum+1}..."
                    }
                    5 -> {
                        val isOdd = Random.nextBoolean()
                        val startAdd = (Random.nextInt(1, 10) * 2 + (if (isOdd) 1 else 0)).toDouble()
                        for (i in 0 until length - 1) sequence.add(sequence.last() + startAdd + (i * 2))
                        explanationStr = "Difference is consecutive ${if (isOdd) "odd" else "even"} numbers (+${formatNumber(startAdd)}, +${formatNumber(startAdd+2)})."
                    }
                    else -> {
                        val mult = Random.nextInt(2, 5).toDouble()
                        sequence.clear()
                        sequence.add(Random.nextInt(2, 6).toDouble())
                        for (i in 1 until length) sequence.add(sequence.last() * mult)
                        explanationStr = "Each term is multiplied by ${formatNumber(mult)}."
                    }
                }
            }
            ArithmeticDifficulty.MEDIUM -> {
                when (patternChoice) {
                    0 -> {
                        val mult = Random.nextInt(2, 4).toDouble()
                        for (i in 1 until length) sequence.add(sequence.last() * mult + i)
                        explanationStr = "(Previous × ${formatNumber(mult)}) + n where n is 1, 2, 3..."
                    }
                    1 -> {
                        val addVal = Random.nextInt(15, 40).toDouble()
                        val subVal = Random.nextInt(5, 20).toDouble()
                        for (i in 1 until length) {
                            if (i % 2 != 0) sequence.add(sequence.last() + addVal)
                            else sequence.add(sequence.last() - subVal)
                        }
                        explanationStr = "Alternate series (+ ${formatNumber(addVal)}, - ${formatNumber(subVal)})."
                    }
                    2 -> {
                        var diff = Random.nextInt(2, 6).toDouble()
                        val mult = Random.nextInt(2, 4).toDouble()
                        for (i in 1 until length) {
                            sequence.add(sequence.last() + diff)
                            diff *= mult
                        }
                        explanationStr = "The difference itself is multiplied by ${formatNumber(mult)}."
                    }
                    3 -> {
                        var diff = Random.nextInt(5, 15).toDouble()
                        val doubleDiff = Random.nextInt(2, 8).toDouble()
                        for (i in 1 until length) {
                            sequence.add(sequence.last() + diff)
                            diff += doubleDiff
                        }
                        explanationStr = "Double difference is constant (+${formatNumber(doubleDiff)})."
                    }
                    4 -> {
                        for (i in 1 until length) sequence.add((sequence.last() * i) + i)
                        explanationStr = "(Previous × 1) + 1, (× 2) + 2, (× 3) + 3..."
                    }
                    5 -> {
                        val sign = if (Random.nextBoolean()) 1 else -1
                        val diffStart = Random.nextInt(2, 6)
                        for (i in 0 until length - 1) {
                            val n = (diffStart + i).toDouble()
                            sequence.add(sequence.last() + (n * n + sign))
                        }
                        explanationStr = "Difference is (n² ${if (sign>0) "+" else "-"} 1)."
                    }
                    else -> {
                        sequence.clear()
                        sequence.add(Random.nextInt(2, 10).toDouble())
                        sequence.add(Random.nextInt(5, 15).toDouble())
                        for (i in 2 until length) sequence.add(sequence[i-1] + sequence[i-2])
                        explanationStr = "Next term is the sum of the previous two terms."
                    }
                }
            }
            ArithmeticDifficulty.HARD -> {
                when (patternChoice) {
                    0 -> {
                        start = listOf(16.0, 24.0, 32.0, 40.0, 48.0).random()
                        sequence.clear()
                        sequence.add(start)
                        var current = start
                        var mult = 0.5
                        for (i in 1 until length) {
                            current = (current * mult) + 1.0
                            sequence.add(current)
                            mult += 0.5
                        }
                        explanationStr = "(Previous × 0.5)+1, (×1)+1, (×1.5)+1..."
                    }
                    1 -> {
                        val mult = Random.nextInt(2, 4).toDouble()
                        for (i in 1 until length) sequence.add(sequence.last() * mult - i)
                        explanationStr = "(Previous × ${formatNumber(mult)}) - n where n is 1, 2, 3..."
                    }
                    2 -> {
                        val startN = Random.nextInt(1, 4).toDouble()
                        for (i in 0 until length - 1) {
                            val n = startN + i
                            sequence.add(sequence.last() + (n * n * n) + (n * n))
                        }
                        explanationStr = "Difference is (n³ + n²)."
                    }
                    3 -> {
                        start = listOf(14.0, 22.0, 30.0, 42.0).random()
                        sequence.clear()
                        sequence.add(start)
                        var current = start
                        var mult = 0.5
                        for (i in 1 until length) {
                            current = (current * mult) + mult
                            sequence.add(current)
                            mult += 0.5
                        }
                        explanationStr = "(Previous × 0.5)+0.5, (×1)+1, (×1.5)+1.5..."
                    }
                    4 -> {
                        val sign = if (Random.nextBoolean()) 1 else -1
                        val diffStart = Random.nextInt(1, 4).toDouble()
                        for (i in 0 until length - 1) {
                            val n = diffStart + i
                            sequence.add(sequence.last() + (n * n * n + sign))
                        }
                        explanationStr = "Difference is (n³ ${if (sign>0) "+" else "-"} 1)."
                    }
                    5 -> {
                        var diff = Random.nextInt(2, 8).toDouble()
                        val squareStart = Random.nextInt(1, 4).toDouble()
                        for (i in 0 until length - 1) {
                            sequence.add(sequence.last() + diff)
                            val n = squareStart + i
                            diff += (n * n)
                        }
                        explanationStr = "Double difference is consecutive squares."
                    }
                    else -> {
                        val primes = listOf(2.0, 3.0, 5.0, 7.0, 11.0, 13.0, 17.0)
                        val pStart = Random.nextInt(0, 2)
                        val mult = Random.nextInt(2, 5).toDouble()
                        for (i in 0 until length - 1) {
                            sequence.add(sequence.last() + (primes[pStart + i] * mult))
                        }
                        explanationStr = "Difference is prime numbers multiplied by ${formatNumber(mult)}."
                    }
                }
            }
        }

        val answer = sequence.last()
        val questionStr = sequence.dropLast(1).joinToString(", ") { formatNumber(it) } + ", ?"

        return buildQuestion(
            title = "Number Series",
            question = questionStr,
            answer = answer,
            distractorLogic = { it + (Random.nextInt(-5, 6).takeIf { n -> n != 0 } ?: 3) },
            explanation = "💡 $explanationStr Missing term: ${formatNumber(answer)}."
        )
    }

    // =========================================================================
    // 4. QUADRATIC EQUATIONS (New Indian Bank Exam Module)
    // =========================================================================
    private fun generateQuadratic(difficulty: ArithmeticDifficulty): ArithmeticQuestion {
        val relType = Random.nextInt(5)
        // 0: x > y, 1: x < y, 2: x >= y, 3: x <= y, 4: No relationship
        
        var rx1 = 0; var rx2 = 0; var ry1 = 0; var ry2 = 0
        when (relType) {
            0 -> { // x > y
                rx1 = Random.nextInt(2, 10); rx2 = Random.nextInt(rx1, 12)
                ry1 = Random.nextInt(-5, rx1 - 1); ry2 = Random.nextInt(ry1, rx1 - 1)
            }
            1 -> { // x < y
                ry1 = Random.nextInt(2, 10); ry2 = Random.nextInt(ry1, 12)
                rx1 = Random.nextInt(-5, ry1 - 1); rx2 = Random.nextInt(rx1, ry1 - 1)
            }
            2 -> { // x >= y
                rx1 = Random.nextInt(2, 10); ry1 = rx1; ry2 = Random.nextInt(-5, ry1 - 1)
                rx2 = Random.nextInt(rx1 + 1, 12)
            }
            3 -> { // x <= y
                ry1 = Random.nextInt(2, 10); rx1 = ry1; rx2 = Random.nextInt(-5, rx1 - 1)
                ry2 = Random.nextInt(ry1 + 1, 12)
            }
            4 -> { // No relationship
                rx1 = Random.nextInt(2, 8); ry1 = rx1 - 2
                rx2 = Random.nextInt(ry1, 10); ry2 = rx2 + 2
            }
        }
        
        val xRoots = listOf(rx1, rx2).shuffled()
        val yRoots = listOf(ry1, ry2).shuffled()
        
        // Optionally scale equation to test factoring
        val aX = if (difficulty == ArithmeticDifficulty.HARD && Random.nextBoolean()) Random.nextInt(2, 4) else 1
        val aY = if (difficulty == ArithmeticDifficulty.HARD && Random.nextBoolean()) Random.nextInt(2, 4) else 1
        
        val eqX = formatQuadratic("x", aX, -aX*(xRoots[0]+xRoots[1]), aX*(xRoots[0]*xRoots[1]))
        val eqY = formatQuadratic("y", aY, -aY*(yRoots[0]+yRoots[1]), aY*(yRoots[0]*yRoots[1]))
        
        val options = listOf(
            "x > y",
            "x < y",
            "x ≥ y",
            "x ≤ y",
            "x = y or Relationship cannot be established"
        )
        
        val explanation = """
            💡 **Step-by-step factoring:**
            Equation I: $eqX
            Roots of x: ${xRoots[0]}, ${xRoots[1]}
            
            Equation II: $eqY
            Roots of y: ${yRoots[0]}, ${yRoots[1]}
            
            **Sign Table Shortcut:**
            If eq is `ax² - bx + c`, roots are (+, +).
            If eq is `ax² + bx + c`, roots are (-, -).
            
            **Comparison:**
            (${xRoots[0]}, ${yRoots[0]}) ➔ ${cmp(xRoots[0], yRoots[0])}
            (${xRoots[0]}, ${yRoots[1]}) ➔ ${cmp(xRoots[0], yRoots[1])}
            (${xRoots[1]}, ${yRoots[0]}) ➔ ${cmp(xRoots[1], yRoots[0])}
            (${xRoots[1]}, ${yRoots[1]}) ➔ ${cmp(xRoots[1], yRoots[1])}
            
            Result: ${options[relType]}
        """.trimIndent()
        
        return ArithmeticQuestion(
            title = "Quadratic Equations",
            questionText = "I. $eqX\nII. $eqY",
            options = options,
            correctIndex = relType,
            explanation = explanation
        )
    }

    private fun cmp(x: Int, y: Int): String = if (x > y) "x > y" else if (x < y) "x < y" else "x = y"
    
    private fun formatQuadratic(v: String, a: Int, b: Int, c: Int): String {
        val aStr = if (a == 1) "$v²" else if (a == -1) "-$v²" else "$a$v²"
        val bStr = if (b == 1) "+ $v" else if (b == -1) "- $v" else if (b > 0) "+ $b$v" else if (b < 0) "- ${-b}$v" else ""
        val cStr = if (c > 0) "+ $c" else if (c < 0) "- ${-c}" else ""
        return "$aStr $bStr $cStr = 0".replace(Regex(" +"), " ").trim()
    }

    // =========================================================================
    // HELPER: FORMAT & BUILD 5 OPTIONS SAFELY
    // =========================================================================
    private fun formatNumber(num: Double): String {
        return if (num == num.toLong().toDouble()) num.toLong().toString() else num.toString()
    }

    private fun buildQuestion(
        title: String,
        question: String,
        answer: Double,
        distractorLogic: (Double) -> Double,
        explanation: String
    ): ArithmeticQuestion {
        val optionsSet = mutableSetOf<Double>()
        optionsSet.add(answer)
        
        // Loop safety: limited attempts for random distractor logic
        var attempts = 0
        while (optionsSet.size < 5 && attempts < 25) {
            val distractor = distractorLogic(answer)
            // Bank exams usually use positive values for series/simplification unless otherwise noted
            if (distractor >= 0 && !optionsSet.contains(distractor)) {
                optionsSet.add(distractor)
            }
            attempts++
        }
        
        // Safe Fallback offsets to guarantee loop completion instantly
        val fallbackOffsets = listOf(-2.0, -1.0, 1.0, 2.0, 3.0, 4.0, 5.0, 10.0, -10.0)
        var offsetIdx = 0
        while (optionsSet.size < 5 && offsetIdx < fallbackOffsets.size) {
            val dist = answer + fallbackOffsets[offsetIdx++]
            if (dist >= 0) optionsSet.add(dist)
        }
        
        // Ultimate fallback
        var mult = 20.0
        while (optionsSet.size < 5) {
            optionsSet.add(answer + mult)
            mult += 5.0
        }

        val shuffledOptions = optionsSet.toList().shuffled()
        val correctIndex = shuffledOptions.indexOf(answer)

        return ArithmeticQuestion(
            title = title,
            questionText = question,
            options = shuffledOptions.map { formatNumber(it) },
            correctIndex = correctIndex,
            explanation = explanation
        )
    }
}
