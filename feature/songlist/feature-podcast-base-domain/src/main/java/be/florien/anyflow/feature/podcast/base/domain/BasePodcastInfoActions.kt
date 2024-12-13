package be.florien.anyflow.feature.podcast.base.domain

import android.content.SharedPreferences
import be.florien.anyflow.feature.podcast.base.domain.model.BasePodcastInfoRow
import be.florien.anyflow.feature.podcast.base.domain.model.PodcastActionType
import be.florien.anyflow.feature.podcast.base.domain.model.PodcastFieldType

abstract class BasePodcastInfoActions(
    val sharedPreferences: SharedPreferences
) {
    fun getShortcuts(): List<BasePodcastInfoRow.ShortcutInfoRow> {
        val string = sharedPreferences.getString(SHORTCUTS_PREF_NAME, "") ?: return emptyList()

        return string.split("#").filter { it.isNotEmpty() }.mapIndexedNotNull { index, it ->
            val fieldTypeString = it.substringBefore('|')
            val actionTypeString = it.substringAfter('|')

            val fieldType = PodcastFieldType.entries.firstOrNull { it.name == fieldTypeString }
            if (fieldType != null) {
                val actionType = PodcastActionType.entries.firstOrNull { it.name == actionTypeString }
                if (actionType != null) {
                    BasePodcastInfoRow.ShortcutInfoRow(
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
        const val SHORTCUTS_PREF_NAME = "PodcastShortcuts"
    }
}