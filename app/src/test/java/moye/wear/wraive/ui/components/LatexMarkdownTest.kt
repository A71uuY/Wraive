package moye.wear.wraive.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class LatexMarkdownTest {
    @Test
    fun supportsCommonInlineAndDisplayDelimiters() {
        assertEquals("解：${'$'}${'$'}x^2${'$'}${'$'}。", normalizeLatexDelimiters("解：${'$'}x^2${'$'}。"))
        assertEquals("${'$'}${'$'}\\frac{a}{b}${'$'}${'$'}", normalizeLatexDelimiters("\\(\\frac{a}{b}\\)"))
        val display = "\n${'$'}${'$'}\nE = mc^2\n${'$'}${'$'}\n"
        assertEquals(display, normalizeLatexDelimiters("\\[E = mc^2\\]"))
        assertEquals(display, normalizeLatexDelimiters("${'$'}${'$'}E = mc^2${'$'}${'$'}"))
        assertEquals(display, normalizeLatexDelimiters("${'$'}${'$'}\nE = mc^2\n${'$'}${'$'}"))
    }

    @Test
    fun preservesCodeCurrencyEscapesAndUnfinishedStreamingMath() {
        listOf(
            "代码 `${'$'}x${'$'}` 和 ``\\(x\\)``",
            "价格 ${'$'}5 和 ${'$'}10",
            "转义 \\${'$'}x\\${'$'}",
            "未完成 ${'$'}x^2",
            "未完成 \\[x^2"
        ).forEach { assertEquals(it, normalizeLatexDelimiters(it)) }
    }
}
