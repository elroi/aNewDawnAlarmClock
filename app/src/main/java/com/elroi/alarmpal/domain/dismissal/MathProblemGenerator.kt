package com.elroi.alarmpal.domain.dismissal

import javax.inject.Inject
import kotlin.random.Random

class MathProblemGenerator @Inject constructor() {

    data class MathProblem(
        val question: String,
        val answer: Int
    )

    fun generateProblem(difficulty: Int): MathProblem {
        return when (difficulty) {
            1 -> generateEasy()
            2 -> generateMedium()
            3 -> generateHard()
            else -> generateEasy()
        }
    }

    private fun generateEasy(): MathProblem {
        val a = Random.nextInt(1, 10)
        val b = Random.nextInt(1, 10)
        return MathProblem("$a + $b", a + b)
    }

    private fun generateMedium(): MathProblem {
        val a = Random.nextInt(10, 50)
        val b = Random.nextInt(10, 50)
        val op = if (Random.nextBoolean()) "+" else "-"
        val answer = if (op == "+") a + b else a - b
        return MathProblem("$a $op $b", answer)
    }

    private fun generateHard(): MathProblem {
        val a = Random.nextInt(10, 100)
        val b = Random.nextInt(2, 10)
        val c = Random.nextInt(10, 100)
        return MathProblem("($a * $b) + $c", (a * b) + c)
    }
}
