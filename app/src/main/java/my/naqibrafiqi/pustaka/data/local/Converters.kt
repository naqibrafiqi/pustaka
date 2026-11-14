package my.naqibrafiqi.pustaka.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import my.naqibrafiqi.pustaka.data.model.Creator
import java.time.Instant

/** Stores times as epoch milliseconds, and lists as JSON arrays. */
class Converters {

    @TypeConverter
    fun instantToEpochMillis(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(millis: Long?): Instant? = millis?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun stringsToJson(strings: List<String>?): String? = strings?.let { json.encodeToString(it) }

    @TypeConverter
    fun jsonToStrings(value: String?): List<String>? =
        value?.let { json.decodeFromString<List<String>>(it) }

    @TypeConverter
    fun creatorsToJson(creators: List<Creator>?): String? =
        creators?.let { json.encodeToString(it) }

    @TypeConverter
    fun jsonToCreators(value: String?): List<Creator>? =
        value?.let { json.decodeFromString<List<Creator>>(it) }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
