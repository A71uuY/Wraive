package moye.wear.wraive.network

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import moye.wear.wraive.model.ChatMessage
import moye.wear.wraive.model.ProviderConfig
import moye.wear.wraive.model.ProviderProtocol
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal fun ProviderConfig.isOfficialDeepSeek(): Boolean =
    baseUrl.toHttpUrlOrNull()?.host == "api.deepseek.com"

// https://api-docs.deepseek.com/guides/web_tools/
internal fun defaultDeepSeekTools(provider: ProviderConfig, modelId: String): List<JsonObject> {
    if (!provider.isOfficialDeepSeek() || provider.protocol == ProviderProtocol.GOOGLE) return emptyList()
    val types = when (modelId) {
        "deepseek-flash", "deepseek-v4-flash", "deepseek-v4-flash-vision-exp" ->
            listOf("deepseek_web_search", "deepseek_read_url")
        "deepseek-v4-pro" -> when (provider.protocol) {
            ProviderProtocol.OPENAI_RESPONSES -> listOf("web_search")
            ProviderProtocol.ANTHROPIC -> listOf("web_search_20250305")
            else -> emptyList()
        }
        else -> emptyList()
    }
    return types.map { type ->
        JsonObject().apply {
            addProperty("type", type)
            if (provider.protocol == ProviderProtocol.ANTHROPIC) {
                addProperty("name", if (type == "web_search_20250305") "web_search" else type)
            }
        }
    }
}

internal fun CompletionRequest.savedProviderContent(message: ChatMessage): JsonElement? {
    val content = message.providerContent ?: return null
    if (!provider.isOfficialDeepSeek() || message.providerId != provider.id ||
        content.protocol != provider.protocol.name
    ) return null
    return parseJson(content.json)
}
