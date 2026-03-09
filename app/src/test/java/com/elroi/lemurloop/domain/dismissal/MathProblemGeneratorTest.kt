package com.elroi.lemurloop.domain.dismissal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * BUG-9: MathProblemGenerator.generateMedium() produces subtraction problems where
 * b can be larger than a (e.g. a=10, b=49 → answer=-39). Negative answers on a
 * number keyboard are confusing on first wake-up.
 * Fix: ensure a >= b before subtraction in generateMedium().
 */
class MathProblemGeneratorTest {

    private val generator = MathProblemGenerator()

    @Test
    fun `easy difficulty produces correct addition answer`() {
        repeat(50) {
            val problem = generator.generateProblem(1)
            val parts = problem.question.split(" + ")
            val a = parts[0].trim().toInt()
            val b = parts[1].trim().toInt()
            assertEquals("Easy addition answer should be a+b", a + b, problem.answer)
        }
    }

    @Test
    fun `medium difficulty answer is never negative`() {
        repeat(100) {
            val problem = generator.generateProblem(2)
            assertTrue(
                "Medium difficulty answer '${problem.answer}' for '${problem.question}' should never be negative",
                problem.answer >= 0
            )
        }
    }

    @Test
    fun `medium difficulty produces correct answer for addition`() {
        repeat(100) {
            val problem = generator.generateProblem(2)
            val question = problem.question
            when {
                question.contains(" + ") -> {
                    val parts = question.split(" + ")
                    assertEquals(parts[0].trim().toInt() + parts[1].trim().toInt(), problem.answer)
                }
                question.contains(" - ") -> {
                    val parts = question.split(" - ")
                    assertEquals(parts[0].trim().toInt() - parts[1].trim().toInt(), problem.answer)
                }
            }
        }
    }

    @Test
    fun `hard difficulty produces correct answer for multiplication and addition`() {
        repeat(50) {
            val problem = generator.generateProblem(3)
            // Expected format: "(a * b) + c"
            val match = Regex("""^\((\d+) \* (\d+)\) \+ (\d+)$""").find(problem.question)
            if (match != null) {
                val (a, b, c) = match.destructured
                val expected = (a.toInt() * b.toInt()) + c.toInt()
                assertEquals("Hard difficulty answer should be (a*b)+c", expected, problem.answer)
            }
        }
    }

    @Test
    fun `difficulty 0 fallback generates easy problem`() {
        val problem = generator.generateProblem(0)
        assertTrue("Fallback should be easy (addition)", problem.question.contains("+"))
        assertTrue("Fallback answer should be positive", problem.answer > 0)
    }

    @Test
    fun `unknown difficulty fallback generates easy problem`() {
        val problem = generator.generateProblem(99)
        assertTrue("Unknown difficulty fallback should use easy addition", problem.question.contains("+"))
    }
}
