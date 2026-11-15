package moye.wear.wraive.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchProviderConfigTest {
    private val gson = Gson()

    @Test
    fun searchTypesRoundTripWithStableNames() {
        SearchProviderType.entries.forEach { type ->
            val provider = SearchProviderConfig(name = type.name, type = type)
            val json = gson.toJsonTree(provider).asJsonObject
            assertEquals(type.name, json["type"].asString)
            assertEquals(provider, gson.fromJson(json, SearchProviderConfig::class.java))
        }
    }

    @Test
    fun readsReleaseConfigurationWithMissingTypeWithoutLosingItsFields() {
        // Field aliases come from the minified release's SearchProviderConfig mapping.
        val json = """{"a":"custom","b":"Custom","c":null,"d":"https://example.invalid/search",
            "e":"","f":true,"g":8,"h":{"Accept":"application/json"}}"""
        val provider = gson.fromJson(json, SearchProviderConfig::class.java)
        assertNull(provider.type)
        assertEquals("https://example.invalid/search", provider.baseUrl)
        assertEquals(mapOf("Accept" to "application/json"), provider.customHeaders)
        assertEquals(
            SearchProviderConfig("custom", "Custom", null, "https://example.invalid/search", resultLimit = 8,
                customHeaders = mapOf("Accept" to "application/json")),
            provider.copy()
        )
        assertEquals(provider.hashCode(), provider.copy().hashCode())
        val repaired = provider.copy(type = SearchProviderType.CUSTOM)
        assertEquals(repaired, gson.fromJson(gson.toJson(repaired), SearchProviderConfig::class.java))
    }
}
