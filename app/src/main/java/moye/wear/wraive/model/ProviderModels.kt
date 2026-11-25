package moye.wear.wraive.model

import com.google.gson.annotations.SerializedName
import java.util.UUID

enum class ProviderProtocol {
    OPENAI_CHAT,
    OPENAI_RESPONSES,
    ANTHROPIC,
    GOOGLE
}

enum class KeyRotationStrategy { ROUND_ROBIN, RANDOM }

enum class ModelCapability {
    TEXT,
    VISION,
    REASONING,
    TOOLS,
    WEB_SEARCH,
    IMAGE_GENERATION
}

internal fun normalizeModelCapabilities(
    capabilities: Set<*>?
): Set<ModelCapability> {
    val normalized = capabilities.orEmpty().mapNotNull { capability ->
        when (capability) {
            is ModelCapability -> capability
            is String -> ModelCapability.entries.firstOrNull { it.name == capability }
            else -> null
        }
    }.toSet()
    return normalized.ifEmpty { setOf(ModelCapability.TEXT) }
}

data class ProviderConfig(
    @SerializedName(value = "id", alternate = ["a"])
    val id: String = UUID.randomUUID().toString(),
    @SerializedName(value = "name", alternate = ["b"])
    val name: String,
    @SerializedName(value = "protocol", alternate = ["c"])
    val protocol: ProviderProtocol,
    @SerializedName(value = "baseUrl", alternate = ["d"])
    val baseUrl: String,
    @SerializedName(value = "apiKey", alternate = ["e"])
    val apiKey: String = "",
    @SerializedName(value = "apiKeys", alternate = ["f"])
    val apiKeys: List<String> = emptyList(),
    @SerializedName(value = "keyRotation", alternate = ["g"])
    val keyRotation: KeyRotationStrategy = KeyRotationStrategy.ROUND_ROBIN,
    @SerializedName(value = "group", alternate = ["h"])
    val group: String = "",
    @SerializedName(value = "enabled", alternate = ["i"])
    val enabled: Boolean = true,
    @SerializedName(value = "customHeaders", alternate = ["j"])
    val customHeaders: Map<String, String> = emptyMap(),
    @SerializedName(value = "customBody", alternate = ["k"])
    val customBody: Map<String, Any?> = emptyMap(),
    @SerializedName(value = "usePromptCaching", alternate = ["l"])
    val usePromptCaching: Boolean = false,
    @SerializedName(value = "sortOrder", alternate = ["m"])
    val sortOrder: Int = 0
)

@Suppress("UNCHECKED_CAST")
fun ProviderConfig.availableApiKeys(): List<String> =
    (listOf(apiKey as String?) + (apiKeys as? List<String?>).orEmpty())
        .mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
        .distinct()

data class ModelConfig(
    @SerializedName(value = "id", alternate = ["a"])
    val id: String,
    @SerializedName(value = "providerId", alternate = ["b"])
    val providerId: String,
    @SerializedName(value = "displayName", alternate = ["c"])
    val displayName: String = id,
    @SerializedName(value = "capabilities", alternate = ["d"])
    val capabilities: Set<ModelCapability> = setOf(ModelCapability.TEXT),
    @SerializedName(value = "maxContextTokens", alternate = ["e"])
    val maxContextTokens: Int = 128_000,
    @SerializedName(value = "maxOutputTokens", alternate = ["f"])
    val maxOutputTokens: Int = 8_192,
    @SerializedName(value = "enabled", alternate = ["g"])
    val enabled: Boolean = true,
    @SerializedName(value = "sortOrder", alternate = ["h"])
    val sortOrder: Int = 0
)

data class ModelRef(
    @SerializedName(value = "providerId", alternate = ["a"])
    val providerId: String,
    @SerializedName(value = "modelId", alternate = ["b"])
    val modelId: String
)

fun ProviderProtocol.defaultBaseUrl(): String = when (this) {
    ProviderProtocol.OPENAI_CHAT,
    ProviderProtocol.OPENAI_RESPONSES -> "https://api.openai.com/v1"
    ProviderProtocol.ANTHROPIC -> "https://api.anthropic.com/v1"
    ProviderProtocol.GOOGLE -> "https://generativelanguage.googleapis.com/v1beta"
}
