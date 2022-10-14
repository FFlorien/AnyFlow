package be.florien.anyflow.data.local

import androidx.room.TypeConverter

class CustomTypeConverters {

    @TypeConverter
    fun intListToString(list: IntArray): String {
        val copy = listOf(*(list.toTypedArray()))
        return copy.joinToString(separator = "|")
    }

    @TypeConverter
    fun stringToIntList(listString: String?): IntArray {
        if (listString.isNullOrBlank()) return IntArray(0)
        return listString.split('|').map { it.toInt() }.toIntArray()
    }
}
