package moye.wear.wraive.ui.components

import android.text.Spanned
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import io.noties.markwon.Markwon
import io.noties.markwon.ext.latex.JLatexAsyncDrawableSpan
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.image.AsyncDrawableScheduler
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class StreamingLatexTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val source = "关系 \\(x^2\\)。\n\n\\[\\frac{a}{b}\\]\n"

    @Test
    fun completedFormulasStayVisibleAsMoreTextArrives() {
        val executor = Executors.newSingleThreadExecutor()
        lateinit var view: TextView
        lateinit var markwon: Markwon
        try {
            instrumentation.runOnMainSync {
                view = TextView(instrumentation.targetContext)
                markwon = markwon(executor)
                setStreamingMarkdown(markwon, view, normalizeLatexDelimiters(source))
            }
            executor.submit {}.get(10, TimeUnit.SECONDS)
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                assertEquals(2, formulas(view).size)
                assertTrue(formulas(view).all { it.drawable.hasResult() })
                repeat(20) { index ->
                    setStreamingMarkdown(markwon, view, normalizeLatexDelimiters(source + "后续说明。".repeat(index + 1)))
                    assertTrue("Appending text must not reset completed formulas to placeholders",
                        formulas(view).all { it.drawable.hasResult() })
                }
            }
        } finally {
            instrumentation.runOnMainSync { AsyncDrawableScheduler.unschedule(view) }
            executor.shutdownNow()
        }
    }

    @Test
    fun streamingDoesNotRestartPendingFormulasAndRemovedOnesAreCancelled() {
        val release = CountDownLatch(1)
        val executor = ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, LinkedBlockingQueue())
        executor.submit { check(release.await(10, TimeUnit.SECONDS)) }
        lateinit var view: TextView
        try {
            instrumentation.runOnMainSync {
                view = TextView(instrumentation.targetContext)
                val markwon = markwon(executor)
                setStreamingMarkdown(markwon, view, normalizeLatexDelimiters(source))
                val initial = formulas(view)
                repeat(20) { index ->
                    setStreamingMarkdown(markwon, view, normalizeLatexDelimiters(source + "仍在生成。".repeat(index + 1)))
                }
                assertEquals("Only the blocker and the two original formula jobs should be queued", 3L, executor.taskCount)
                setStreamingMarkdown(markwon, view, "普通文本")
                initial.forEach { assertNull(it.drawable.callback) }
                assertEquals(0, formulas(view).size)
            }
            release.countDown()
            executor.submit {}.get(10, TimeUnit.SECONDS)
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync { assertEquals("普通文本", view.text.toString()) }
        } finally {
            release.countDown()
            instrumentation.runOnMainSync { AsyncDrawableScheduler.unschedule(view) }
            executor.shutdownNow()
        }
    }

    private fun markwon(executor: ExecutorService): Markwon = Markwon.builder(instrumentation.targetContext)
        .usePlugin(MarkwonInlineParserPlugin.create())
        .usePlugin(StreamingLatexPlugin(JLatexMathPlugin.create(14f) {
            it.inlinesEnabled(true)
            it.executorService(executor)
        }))
        .build()

    private fun formulas(view: TextView): Array<JLatexAsyncDrawableSpan> {
        val text = view.text as Spanned
        return text.getSpans(0, text.length, JLatexAsyncDrawableSpan::class.java)
    }
}
