package moye.wear.wraive.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.wear.compose.material3.AppScaffold
import com.google.gson.Gson
import moye.wear.wraive.model.SearchProviderConfig
import moye.wear.wraive.model.SearchProviderType
import moye.wear.wraive.ui.screens.SearchProvidersScreen
import moye.wear.wraive.ui.theme.WraiveTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SearchProvidersUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun customSearchCanBeAddedAndAnOlderMissingTypeCanBeRepaired() {
        val older = SearchProviderConfig(
            id = "older", name = "旧搜索", type = null,
            baseUrl = "https://example.invalid/older", customHeaders = mapOf("Accept" to "application/json")
        )
        val providers = mutableStateOf(listOf(older))
        val gson = Gson()
        compose.setContent {
            WraiveTheme(dynamicColor = false, localeTag = "zh") {
                AppScaffold {
                    SearchProvidersScreen(providers.value, gson, onSave = { provider ->
                        providers.value = providers.value.filterNot { it.id == provider.id } + provider
                    }, onDelete = {})
                }
            }
        }
        compose.onNodeWithText("添加搜索服务").performClick()
        selectCustom()
        scrollTo("API URL")
        compose.onNodeWithText("API URL").performTextReplacement("https://example.invalid/search")
        showSaveButton()
        compose.onNodeWithText("保存").performClick()
        compose.runOnIdle {
            val added = providers.value.single { it.id != older.id }
            assertEquals(SearchProviderType.CUSTOM, added.type)
            assertEquals("https://example.invalid/search", added.baseUrl)
        }

        scrollTo("旧搜索")
        compose.onNodeWithText("旧搜索").performClick()
        compose.onNode(hasText("搜索服务") and hasText("请重新选择搜索服务类型")).assertIsDisplayed()
        showSaveButton()
        compose.onNodeWithText("保存").assertIsNotEnabled()
        selectCustom()
        showSaveButton()
        compose.onNodeWithText("保存").performClick()
        compose.runOnIdle {
            assertEquals(older.copy(type = SearchProviderType.CUSTOM), providers.value.single { it.id == older.id })
        }
    }

    private fun selectCustom() {
        scrollTo("搜索服务")
        compose.onNodeWithText("搜索服务").performClick()
        scrollTo("Custom")
        compose.onAllNodesWithText("Custom").onLast().performClick()
    }

    private fun showSaveButton() {
        scrollTo("取消")
        compose.onAllNodes(hasScrollAction()).onLast().performTouchInput { swipeUp() }
    }

    private fun scrollTo(text: String) {
        compose.onAllNodes(hasScrollAction()).onLast().performScrollToNode(hasText(text))
    }
}
