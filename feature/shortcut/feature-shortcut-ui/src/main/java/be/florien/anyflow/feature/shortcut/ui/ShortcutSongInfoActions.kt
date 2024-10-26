package be.florien.anyflow.feature.shortcut.ui

import android.content.SharedPreferences
import be.florien.anyflow.feature.song.base.domain.BaseSongInfoActions
import be.florien.anyflow.feature.song.base.domain.model.SongActionType
import be.florien.anyflow.feature.song.base.domain.model.SongFieldType

class ShortcutSongInfoActions(//todo this in domain module ???
    sharedPreferences: SharedPreferences
) : BaseSongInfoActions(sharedPreferences) {

    /**
     * Shortcuts
     */

    fun toggleShortcut(fieldType: SongFieldType, actionType: SongActionType) {
        val shortcuts = getShortcuts().toMutableList()
        if (shortcuts.removeAll { it.fieldType == fieldType && it.actionType == actionType }) {
            sharedPreferences.edit()
                .putString(
                    SHORTCUTS_PREF_NAME,
                    shortcuts.joinToString(separator = "#") {
                        val fieldName = (it.fieldType as Enum<*>).name
                        val actionName = (it.actionType as Enum<*>).name
                        "$fieldName|$actionName"
                    }
                )
                .apply()
        } else {
            val fieldName = (fieldType as Enum<*>).name
            val actionName = (actionType as Enum<*>).name
            val originalString = sharedPreferences.getString(SHORTCUTS_PREF_NAME, "")
            sharedPreferences.edit()
                .putString(SHORTCUTS_PREF_NAME, "$originalString#$fieldName|$actionName")
                .apply()
        }
    }
}