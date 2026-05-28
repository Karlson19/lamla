package app.lamla.data.local

import androidx.room.TypeConverter
import app.lamla.domain.model.CaptureType
import app.lamla.domain.model.DeadlineStatus
import app.lamla.domain.model.OfficeHourSlot
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.DayOfWeek

/**
 * Room TypeConverters.
 *
 * All custom types persist via kotlinx-serialization JSON. Cheaper than wiring
 * Moshi/Gson, and we already have it for export/import.
 *
 * Enums via name() — not ordinal, which would silently re-map on reorder.
 */
class Converters {

    @TypeConverter
    fun dayToInt(value: DayOfWeek?): Int? = value?.value

    @TypeConverter
    fun intToDay(value: Int?): DayOfWeek? = value?.let { DayOfWeek.of(it) }

    @TypeConverter
    fun statusToString(value: DeadlineStatus?): String? = value?.name

    @TypeConverter
    fun stringToStatus(value: String?): DeadlineStatus? = value?.let { DeadlineStatus.valueOf(it) }

    @TypeConverter
    fun captureTypeToString(value: CaptureType?): String? = value?.name

    @TypeConverter
    fun stringToCaptureType(value: String?): CaptureType? = value?.let { CaptureType.valueOf(it) }

    @TypeConverter
    fun intListToJson(value: List<Int>?): String =
        if (value == null) "[]" else Json.encodeToString(ListSerializer(Int.serializer()), value)

    @TypeConverter
    fun jsonToIntList(value: String?): List<Int> =
        if (value.isNullOrBlank()) emptyList()
        else Json.decodeFromString(ListSerializer(Int.serializer()), value)

    @TypeConverter
    fun stringListToJson(value: List<String>?): String =
        if (value == null) "[]" else Json.encodeToString(ListSerializer(String.serializer()), value)

    @TypeConverter
    fun jsonToStringList(value: String?): List<String> =
        if (value.isNullOrBlank()) emptyList()
        else Json.decodeFromString(ListSerializer(String.serializer()), value)

    @TypeConverter
    fun officeHoursToJson(value: List<OfficeHourSlot>?): String =
        if (value == null) "[]" else Json.encodeToString(ListSerializer(OfficeHourSlot.serializer()), value)

    @TypeConverter
    fun jsonToOfficeHours(value: String?): List<OfficeHourSlot> =
        if (value.isNullOrBlank()) emptyList()
        else Json.decodeFromString(ListSerializer(OfficeHourSlot.serializer()), value)
}
