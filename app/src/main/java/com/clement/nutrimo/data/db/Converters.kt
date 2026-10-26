// Room type converters and the LocalDate JSON serializer used by entities.
package com.clement.nutrimo.data.db

import androidx.room.TypeConverter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun dateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToDate(day: Long?): LocalDate? = day?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun microsToJson(micros: Map<String, Double>): String = Json.encodeToString(micros)

    @TypeConverter
    fun jsonToMicros(json: String): Map<String, Double> = Json.decodeFromString(json)
}

/** ISO yyyy-MM-dd in backups. */
object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}
