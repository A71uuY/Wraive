package moye.wear.wraive.ui.components

private val latexDelimiters = Regex(
    """(`+)[\s\S]*?\1|\\\[([\s\S]*?)\\\]|\\\(([\s\S]*?)\\\)|(?<![\\${'$'}])\${'$'}\${'$'}([\s\S]*?)(?<!\\)\${'$'}\${'$'}(?!\${'$'})|(?<![\\${'$'}])\${'$'}(?!\s)([^\n${'$'}]+?)(?<![\\\s])\${'$'}(?![\d${'$'}])"""
)

// Markwon uses $$ for inline math and requires display delimiters on separate lines.
internal fun normalizeLatexDelimiters(markdown: String): String =
    latexDelimiters.replace(markdown) { match ->
        val groups = match.groups
        when {
            groups[1] != null -> match.value // Preserve inline code verbatim.
            groups[2] != null || groups[4] != null -> {
                val formula = (groups[2] ?: groups[4])!!.value.trim()
                "\n${'$'}${'$'}\n$formula\n${'$'}${'$'}\n"
            }
            else -> "${'$'}${'$'}${(groups[3] ?: groups[5])!!.value}${'$'}${'$'}"
        }
    }
