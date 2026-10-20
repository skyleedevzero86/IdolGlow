package com.sleekydz86.idolglow.global.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import org.springframework.boot.jackson2.autoconfigure.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime

@Configuration
@Suppress("TooManyFunctions")
class JacksonDateTimeConfig {
    @Bean
    fun jackson2ObjectMapperBuilderCustomizer(): Jackson2ObjectMapperBuilderCustomizer =
        Jackson2ObjectMapperBuilderCustomizer { builder ->
            builder.modules(JavaTimeModule(), idolGlowDateTimeModule())
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }

    private fun idolGlowDateTimeModule(): SimpleModule {
        val module = SimpleModule("IdolGlowDateTimes")
        module.addSerializer(LocalDateTime::class.java, DisplayLocalDateTimeSerializer)
        module.addDeserializer(LocalDateTime::class.java, DisplayLocalDateTimeDeserializer)
        module.addSerializer(LocalDate::class.java, DisplayLocalDateSerializer)
        module.addDeserializer(LocalDate::class.java, DisplayLocalDateDeserializer)
        module.addSerializer(LocalTime::class.java, DisplayLocalTimeSerializer)
        module.addDeserializer(LocalTime::class.java, DisplayLocalTimeDeserializer)
        module.addSerializer(Instant::class.java, DisplayInstantSerializer)
        module.addDeserializer(Instant::class.java, DisplayInstantDeserializer)
        module.addSerializer(OffsetDateTime::class.java, DisplayOffsetDateTimeSerializer)
        module.addDeserializer(OffsetDateTime::class.java, DisplayOffsetDateTimeDeserializer)
        return module
    }

    private object DisplayLocalDateTimeSerializer : JsonSerializer<LocalDateTime>() {
        override fun serialize(
            value: LocalDateTime,
            gen: JsonGenerator,
            serializers: SerializerProvider,
        ) {
            gen.writeString(IdolGlowDateTimes.toDisplay(value))
        }
    }

    private object DisplayLocalDateTimeDeserializer : JsonDeserializer<LocalDateTime>() {
        override fun deserialize(
            p: JsonParser,
            ctxt: DeserializationContext,
        ): LocalDateTime? = IdolGlowDateTimes.parseDateTime(p.valueAsString)
    }

    private object DisplayLocalDateSerializer : JsonSerializer<LocalDate>() {
        override fun serialize(
            value: LocalDate,
            gen: JsonGenerator,
            serializers: SerializerProvider,
        ) {
            gen.writeString(IdolGlowDateTimes.toDisplay(value))
        }
    }

    private object DisplayLocalDateDeserializer : JsonDeserializer<LocalDate>() {
        override fun deserialize(
            p: JsonParser,
            ctxt: DeserializationContext,
        ): LocalDate? = IdolGlowDateTimes.parseDate(p.valueAsString)
    }

    private object DisplayLocalTimeSerializer : JsonSerializer<LocalTime>() {
        override fun serialize(
            value: LocalTime,
            gen: JsonGenerator,
            serializers: SerializerProvider,
        ) {
            gen.writeString(IdolGlowDateTimes.toDisplay(value))
        }
    }

    private object DisplayLocalTimeDeserializer : JsonDeserializer<LocalTime>() {
        override fun deserialize(
            p: JsonParser,
            ctxt: DeserializationContext,
        ): LocalTime? = IdolGlowDateTimes.parseTime(p.valueAsString)
    }

    private object DisplayInstantSerializer : JsonSerializer<Instant>() {
        override fun serialize(
            value: Instant,
            gen: JsonGenerator,
            serializers: SerializerProvider,
        ) {
            gen.writeString(IdolGlowDateTimes.toDisplay(value))
        }
    }

    private object DisplayInstantDeserializer : JsonDeserializer<Instant>() {
        override fun deserialize(
            p: JsonParser,
            ctxt: DeserializationContext,
        ): Instant? = IdolGlowDateTimes.parseInstant(p.valueAsString)
    }

    private object DisplayOffsetDateTimeSerializer : JsonSerializer<OffsetDateTime>() {
        override fun serialize(
            value: OffsetDateTime,
            gen: JsonGenerator,
            serializers: SerializerProvider,
        ) {
            gen.writeString(IdolGlowDateTimes.toDisplay(value))
        }
    }

    private object DisplayOffsetDateTimeDeserializer : JsonDeserializer<OffsetDateTime>() {
        override fun deserialize(
            p: JsonParser,
            ctxt: DeserializationContext,
        ): OffsetDateTime? =
            IdolGlowDateTimes.parseDateTime(p.valueAsString)?.atZone(IdolGlowDateTimes.ZONE)?.toOffsetDateTime()
    }
}
