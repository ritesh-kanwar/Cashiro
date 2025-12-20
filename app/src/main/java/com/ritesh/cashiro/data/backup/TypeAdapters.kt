package com.ritesh.cashiro.data.backup

import com.google.gson.*
import java.lang.reflect.Type
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Gson TypeAdapter for LocalDateTime
 */
class LocalDateTimeTypeAdapter : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    override fun serialize(
        src: LocalDateTime?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(src?.format(formatter))
    }
    
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): LocalDateTime? {
        return json?.asString?.let { 
            LocalDateTime.parse(it, formatter)
        }
    }
}

/**
 * Gson TypeAdapter for BigDecimal
 */
class BigDecimalTypeAdapter : JsonSerializer<BigDecimal>, JsonDeserializer<BigDecimal> {
    override fun serialize(
        src: BigDecimal?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(src?.toPlainString())
    }
    
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): BigDecimal? {
        return json?.asString?.let { BigDecimal(it) }
    }
}