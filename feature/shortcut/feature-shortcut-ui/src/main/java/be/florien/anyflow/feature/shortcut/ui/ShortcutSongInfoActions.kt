package be.florien.anyflow.feature.shortcut.ui

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.feature.song.base.ui.BaseSongInfoActions
import be.florien.anyflow.tags.local.model.DownloadProgressState
import be.florien.anyflow.management.filters.model.Filter

class ShortcutSongInfoActions(//todo this in domain module ???
    sharedPreferences: SharedPreferences
) : BaseSongInfoActions(sharedPreferences) {

    /**
     * Overridden methods
     */

    override fun getDownloadState(
        id: Long,
        type: Filter.FilterType,
        additionalInfo: Int?
    ): LiveData<DownloadProgressState> = MutableLiveData(DownloadProgressState(100, 0, 0))

    /**
     * Shortcuts
     */

    fun toggleShortcut(fieldType: FieldType, actionType: ActionType) {
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