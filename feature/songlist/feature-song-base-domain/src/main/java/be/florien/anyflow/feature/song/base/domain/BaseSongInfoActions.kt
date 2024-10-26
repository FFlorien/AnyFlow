package be.florien.anyflow.feature.song.base.domain

import android.content.SharedPreferences
import be.florien.anyflow.feature.song.base.domain.model.ShortcutInfoRow
import be.florien.anyflow.feature.song.base.domain.model.SongActionType
import be.florien.anyflow.feature.song.base.domain.model.SongFieldType

abstract class BaseSongInfoActions(
    val sharedPreferences: SharedPreferences
) {
    fun getShortcuts(): List<ShortcutInfoRow> {
        val string = sharedPreferences.getString(SHORTCUTS_PREF_NAME, "") ?: return emptyList()

        return string.split("#").filter { it.isNotEmpty() }.mapIndexedNotNull { index, it ->
            val fieldTypeString = it.substringBefore('|')
            val actionTypeString = it.substringAfter('|')

            val fieldType = SongFieldType.entries.firstOrNull { it.name == fieldTypeString }
            if (fieldType != null) {
                val actionType = SongActionType.entries.firstOrNull { it.name == actionTypeString }
                if (actionType != null) {
                    ShortcutInfoRow(
                        fieldType,
                        actionType,
                        order = index
                    )
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    companion object {
        const val DUMMY_SONG_ID = -5L
        const val SHORTCUTS_PREF_NAME = "Shortcuts"
    }
}