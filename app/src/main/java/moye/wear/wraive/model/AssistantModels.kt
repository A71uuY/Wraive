package moye.wear.wraive.model

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class AssistantProfile(
    @SerializedName(value = "id", alternate = ["a"])
    val id: String = UUID.randomUUID().toString(),
    @SerializedName(value = "name", alternate = ["b"])
    val name: String,
    @SerializedName(value = "description", alternate = ["c"])
    val description: String = "",
    @SerializedName(value = "avatarUri", alternate = ["d"])
    val avatarUri: String? = null,
    @SerializedName(value = "systemPrompt", alternate = ["e"])
    val systemPrompt: String = "",
    @SerializedName(value = "model", alternate = ["f"])
    val model: ModelRef? = null,
    @SerializedName(value = "temperature", alternate = ["g"])
    val temperature: Double = 0.7,
    @SerializedName(value = "topP", alternate = ["h"])
    val topP: Double = 1.0,
    @SerializedName(value = "maxOutputTokens", alternate = ["i"])
    val maxOutputTokens: Int? = null,
    @SerializedName(value = "reasoningEnabled", alternate = ["j"])
    val reasoningEnabled: Boolean = false,
    @SerializedName(value = "reasoningBudget", alternate = ["k"])
    val reasoningBudget: Int? = null,
    @SerializedName(value = "webSearchEnabled", alternate = ["l"])
    val webSearchEnabled: Boolean = false,
    @SerializedName(value = "searchProviderId", alternate = ["m"])
    val searchProviderId: String? = null,
    @SerializedName(value = "memoryEnabled", alternate = ["n"])
    val memoryEnabled: Boolean = true,
    @SerializedName(value = "autoMemoryExtraction", alternate = ["o"])
    val autoMemoryExtraction: Boolean = false,
    @SerializedName(value = "worldBookIds", alternate = ["p"])
    val worldBookIds: Set<String> = emptySet(),
    @SerializedName(value = "mcpServerIds", alternate = ["q"])
    val mcpServerIds: Set<String> = emptySet(),
    @SerializedName(value = "customHeaders", alternate = ["r"])
    val customHeaders: Map<String, String> = emptyMap(),
    @SerializedName(value = "customBody", alternate = ["s"])
    val customBody: Map<String, Any?> = emptyMap(),
    @SerializedName(value = "imageSize", alternate = ["t"])
    val imageSize: String = "1024x1024",
    @SerializedName(value = "imageQuality", alternate = ["u"])
    val imageQuality: String = "auto",
    @SerializedName(value = "imageCount", alternate = ["v"])
    val imageCount: Int = 1,
    @SerializedName(value = "sortOrder", alternate = ["w"])
    val sortOrder: Int = 0
)

data class QuickPhrase(
    @SerializedName(value = "id", alternate = ["a"])
    val id: String = UUID.randomUUID().toString(),
    @SerializedName(value = "title", alternate = ["b"])
    val title: String,
    @SerializedName(value = "content", alternate = ["c"])
    val content: String,
    @SerializedName(value = "assistantId", alternate = ["d"])
    val assistantId: String? = null,
    @SerializedName(value = "sortOrder", alternate = ["e"])
    val sortOrder: Int = 0
)

data class WorldBookEntry(
    @SerializedName(value = "id", alternate = ["a"])
    val id: String = UUID.randomUUID().toString(),
    @SerializedName(value = "bookId", alternate = ["b"])
    val bookId: String,
    @SerializedName(value = "title", alternate = ["c"])
    val title: String,
    @SerializedName(value = "keywords", alternate = ["d"])
    val keywords: List<String>,
    @SerializedName(value = "content", alternate = ["e"])
    val content: String,
    @SerializedName(value = "enabled", alternate = ["f"])
    val enabled: Boolean = true,
    @SerializedName(value = "priority", alternate = ["g"])
    val priority: Int = 0
)

data class MemoryEntry(
    @SerializedName(value = "id", alternate = ["a"])
    val id: String = UUID.randomUUID().toString(),
    @SerializedName(value = "assistantId", alternate = ["b"])
    val assistantId: String? = null,
    @SerializedName(value = "category", alternate = ["c"])
    val category: MemoryCategory?,
    @SerializedName(value = "content", alternate = ["d"])
    val content: String,
    @SerializedName(value = "enabled", alternate = ["e"])
    val enabled: Boolean = true,
    @SerializedName(value = "createdAt", alternate = ["f"])
    val createdAt: Long = System.currentTimeMillis(),
    @SerializedName(value = "updatedAt", alternate = ["g"])
    val updatedAt: Long = createdAt
) {
    // Older releases may have persisted a missing category through Gson.
    val effectiveCategory: MemoryCategory
        get() = category ?: MemoryCategory.IDENTITY
}

enum class MemoryCategory {
    @SerializedName(value = "IDENTITY", alternate = ["g"]) IDENTITY,
    @SerializedName("WORKFLOW") WORKFLOW,
    @SerializedName("VOICE") VOICE,
    @SerializedName("INSTRUCTION") INSTRUCTION
}

data class PromptTransform(
    @SerializedName(value = "id", alternate = ["a"])
    val id: String = UUID.randomUUID().toString(),
    @SerializedName(value = "name", alternate = ["b"])
    val name: String,
    @SerializedName(value = "pattern", alternate = ["c"])
    val pattern: String,
    @SerializedName(value = "replacement", alternate = ["d"])
    val replacement: String,
    @SerializedName(value = "applyToUser", alternate = ["e"])
    val applyToUser: Boolean = false,
    @SerializedName(value = "applyToAssistant", alternate = ["f"])
    val applyToAssistant: Boolean = true,
    @SerializedName(value = "enabled", alternate = ["g"])
    val enabled: Boolean = true
)
