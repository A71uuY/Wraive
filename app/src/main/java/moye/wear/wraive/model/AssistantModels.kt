package moye.wear.wraive.model

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class AssistantProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val avatarUri: String? = null,
    val systemPrompt: String = "",
    val model: ModelRef? = null,
    val temperature: Double = 0.7,
    val topP: Double = 1.0,
    val maxOutputTokens: Int? = null,
    val reasoningEnabled: Boolean = false,
    val reasoningBudget: Int? = null,
    val webSearchEnabled: Boolean = false,
    val searchProviderId: String? = null,
    val memoryEnabled: Boolean = true,
    val autoMemoryExtraction: Boolean = false,
    val worldBookIds: Set<String> = emptySet(),
    val mcpServerIds: Set<String> = emptySet(),
    val customHeaders: Map<String, String> = emptyMap(),
    val customBody: Map<String, Any?> = emptyMap(),
    val imageSize: String = "1024x1024",
    val imageQuality: String = "auto",
    val imageCount: Int = 1,
    val sortOrder: Int = 0
)

data class QuickPhrase(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val assistantId: String? = null,
    val sortOrder: Int = 0
)

data class WorldBookEntry(
    val id: String = UUID.randomUUID().toString(),
    val bookId: String,
    val title: String,
    val keywords: List<String>,
    val content: String,
    val enabled: Boolean = true,
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
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val pattern: String,
    val replacement: String,
    val applyToUser: Boolean = false,
    val applyToAssistant: Boolean = true,
    val enabled: Boolean = true
)
