package moye.wear.wraive.ui

import android.net.Uri
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.platform.app.InstrumentationRegistry
import androidx.wear.compose.material3.AppScaffold
import moye.wear.wraive.ui.components.LocalFilePicker
import moye.wear.wraive.ui.theme.WraiveTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

class WearFilePickerTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun nativePickerBrowsesFoldersAndSelectsMultipleFiles() = selectFiles(multiple = true)

    @Test
    fun singleSelectionReplacesThePreviousFile() = selectFiles(multiple = false)

    private fun selectFiles(multiple: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val root = File(instrumentation.targetContext.cacheDir, "picker-${UUID.randomUUID()}")
        val directory = File(root, "samples")
        var result = emptyList<Uri>()
        try {
            check(directory.mkdirs())
            val first = File(directory, "a.txt").apply { writeText("first") }
            val second = File(directory, "b.txt").apply { writeText("second") }
            File(directory, "hidden.png").writeText("filtered by MIME type")
            compose.setContent {
                WraiveTheme(dynamicColor = false, localeTag = "zh") {
                    AppScaffold {
                        LocalFilePicker(root, arrayOf("text/plain"), multiple,
                            onDismiss = {}, onFilesSelected = { result = it })
                    }
                }
            }
            fun clickInList(text: String) {
                compose.onAllNodes(hasScrollAction()).onLast().performScrollToNode(hasText(text))
                compose.onNodeWithText(text).performClick()
            }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText(directory.name).fetchSemanticsNodes().isNotEmpty()
            }
            clickInList(directory.name)
            clickInList("上一级")
            compose.waitUntil(5_000) { compose.onAllNodesWithText(directory.name).fetchSemanticsNodes().isNotEmpty() }
            clickInList(directory.name)
            compose.waitUntil(5_000) { compose.onAllNodesWithText(first.name).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("hidden.png").assertDoesNotExist()
            clickInList(first.name)
            clickInList(second.name)
            compose.onAllNodes(hasScrollAction()).onLast().performSemanticsAction(SemanticsActions.ScrollBy) {
                it(0f, 10_000f)
            }
            compose.waitForIdle()
            compose.onNodeWithText("选择 (${if (multiple) 2 else 1})").performClick()
            compose.waitForIdle()
            val expected = if (multiple) listOf(first, second) else listOf(second)
            assertEquals(expected.map(Uri::fromFile), result)
        } finally {
            root.deleteRecursively()
        }
    }
}
