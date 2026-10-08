package com.ritesh.cashiro.data.database.entity

import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDateTime

class RuleCategoryRenameTest {
    private fun rule(conditions: String = "[]", actions: String = "[]") = RuleEntity(
        id = "example", name = "Example", description = null, priority = 1, conditions = conditions,
        actions = actions, isActive = true, createdAt = LocalDateTime.MIN, updatedAt = LocalDateTime.MIN
    )

    @Test fun renamesCaseInsensitiveConditionsAndListMembersWithoutDroppingUnknownFields() {
        val original = rule(conditions = """[
            {"field":"CATEGORY","operator":"IN","value":" old ,Other,OLDER","future":{"a":1}},
            {"field":"CATEGORY","operator":"NOT_EQUALS","value":"OLD"},
            {"field":"MERCHANT","operator":"EQUALS","value":"Old"}
        ]""")
        val result = Json.parseToJsonElement(original.withCategoryRenamed("Old", "New").conditions).jsonArray
        assertEquals("New,Other,OLDER", result[0].jsonObject["value"]!!.jsonPrimitive.content)
        assertEquals(Json.parseToJsonElement("""{"a":1}"""), result[0].jsonObject["future"])
        assertEquals("New", result[1].jsonObject["value"]!!.jsonPrimitive.content)
        assertEquals("Old", result[2].jsonObject["value"]!!.jsonPrimitive.content)
    }

    @Test fun onlyExactSetActionsAreRenamedAndUnknownFieldsArePreserved() {
        val original = rule(actions = """[
            {"field":"CATEGORY","actionType":"SET","value":"Old","future":[1,2]},
            {"field":"CATEGORY","actionType":"SET","value":"old"},
            {"field":"CATEGORY","actionType":"APPEND","value":"Old"}
        ]""")
        val result = Json.parseToJsonElement(original.withCategoryRenamed("Old", "New").actions).jsonArray
        assertEquals("New", result[0].jsonObject["value"]!!.jsonPrimitive.content)
        assertEquals(Json.parseToJsonElement("[1,2]"), result[0].jsonObject["future"])
        assertEquals("old", result[1].jsonObject["value"]!!.jsonPrimitive.content)
        assertEquals("Old", result[2].jsonObject["value"]!!.jsonPrimitive.content)
    }

    @Test fun preservesUnrelatedJsonFormattingRegexAndFutureOperators() {
        val original = rule(conditions = """[ {"field":"CATEGORY","operator":"REGEX_MATCHES","value":"Old"},
            {"field":"CATEGORY","operator":"FUTURE_OPERATOR","value":"Old"} ]""")
        assertEquals(original, original.withCategoryRenamed("Old", "New"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidJsonCannotSilentlySkipCategoryReferences() {
        rule(actions = "broken").withCategoryRenamed("Old", "New")
    }
}
