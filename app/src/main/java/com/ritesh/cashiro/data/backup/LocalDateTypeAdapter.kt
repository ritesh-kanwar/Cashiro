package com.ritesh.cashiro.data.backup

import com.google.gson.*
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class LocalDateTypeAdapter : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    
    override fun serialize(
        src: LocalDate?,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return if (src == null) {
            JsonNull.INSTANCE
        } else {
            JsonPrimitive(src.format(formatter))
        }
    }
    
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): LocalDate? {
        return when {
            json.isJsonNull -> null
            json.isJsonPrimitive -> {
                val dateString = json.asString
                // Handle invalid dates
                if (dateString.isNullOrBlank() || dateString == "0000-00-00") {
                    null
                } else {
                    try {
                        LocalDate.parse(dateString, formatter)
                    } catch (e: DateTimeParseException) {
                        // Log the error and return null for invalid dates
                        android.util.Log.w("LocalDateTypeAdapter", "Invalid date format: $dateString", e)
                        null
                    }
                }
            }
            else -> null
        }
    }
}