package com.ritesh.cashiro.data.database.entity

import kotlinx.serialization.json.*

/** Edit the JSON tree so future rule fields survive a rename. Invalid JSON aborts the transaction. */
internal fun RuleEntity.withCategoryRenamed(oldName: String, newName: String): RuleEntity {
    fun rename(json: String, conditions: Boolean): String {
        val original = Json.parseToJsonElement(json).jsonArray
        val renamed = JsonArray(original.map { element ->
            val item = element.jsonObject
            if (item["field"]?.jsonPrimitive?.content != "CATEGORY") return@map element
            val value = item.getValue("value").jsonPrimitive.content
            val replacement = if (conditions) {
                when (item.getValue("operator").jsonPrimitive.content) {
                    "IN", "NOT_IN" -> value.split(",").joinToString(",") {
                        if (it.trim().equals(oldName, ignoreCase = true)) newName else it
                    }
                    "EQUALS", "NOT_EQUALS", "CONTAINS", "NOT_CONTAINS", "STARTS_WITH", "ENDS_WITH" ->
                        if (value.equals(oldName, ignoreCase = true)) newName else value
                    else -> value
                }
            } else {
                if (item.getValue("actionType").jsonPrimitive.content == "SET" && value == oldName)
                    newName else value
            }
            if (replacement == value) element else JsonObject(item + ("value" to JsonPrimitive(replacement)))
        })
        return if (renamed == original) json else renamed.toString()
    }
    return copy(conditions = rename(conditions, true), actions = rename(actions, false))
}
