package moye.wear.wraive.ui.components

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.widget.TextView
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonPlugin
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.latex.JLatexAsyncDrawableSpan
import io.noties.markwon.image.AsyncDrawableSpan

internal class StreamingLatexPlugin(delegate: JLatexMathPlugin) : MarkwonPlugin by delegate {
    override fun beforeSetText(textView: TextView, markdown: Spanned) {
        val retained = markdown.getSpans(0, markdown.length, AsyncDrawableSpan::class.java)
            .map { it.drawable }.toSet()
        val previous = textView.text as? Spanned ?: return
        previous.getSpans(0, previous.length, AsyncDrawableSpan::class.java).forEach { span ->
            // Keep pending and completed formulas alive while their surrounding text changes.
            if (span.drawable !in retained) span.drawable.setCallback2(null)
        }
    }
}

private data class MarkdownBinding(val markwon: Markwon, val markdown: String)

internal fun setStreamingMarkdown(markwon: Markwon, textView: TextView, markdown: String) {
    val previousBinding = textView.tag as? MarkdownBinding
    if (previousBinding?.markwon === markwon && previousBinding.markdown == markdown) return

    val rendered = SpannableStringBuilder(markwon.toMarkdown(markdown))
    val previous = textView.text as? Spanned
    if (previousBinding?.markwon === markwon && previous != null) {
        val reusable = previous.getSpans(0, previous.length, JLatexAsyncDrawableSpan::class.java)
            .groupBy { it.javaClass to it.drawable.destination }
            .mapValues { (_, spans) -> ArrayDeque(spans) }
        rendered.getSpans(0, rendered.length, JLatexAsyncDrawableSpan::class.java).forEach { span ->
            val retained = reusable[span.javaClass to span.drawable.destination]?.removeFirstOrNull()
            if (retained != null) {
                val start = rendered.getSpanStart(span)
                val end = rendered.getSpanEnd(span)
                val flags = rendered.getSpanFlags(span)
                rendered.removeSpan(span)
                rendered.setSpan(retained, start, end, flags)
            }
        }
    }
    markwon.setParsedMarkdown(textView, rendered)
    textView.tag = MarkdownBinding(markwon, markdown)
}
