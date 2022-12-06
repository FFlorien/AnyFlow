package be.florien.anyflow.feature.player.info

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import be.florien.anyflow.R

abstract class InfoActions<T> {

    abstract fun getInfoRows(infoSource: T): List<InfoRow>

    abstract fun getActionsRows(
        infoSource: T,
        fieldType: FieldType
    ): List<InfoRow>

    class InfoRow(
        @StringRes val title: Int,
        val text: String?,
        @StringRes val textRes: Int?,
        val fieldType: FieldType,
        val actionType: ActionType,
        val additionalInfo: Int? = null
    ) {
        constructor(other: InfoRow, order: Int? = null) : this(
            other.title,
            other.text,
            other.textRes,
            other.fieldType,
            other.actionType,
            order ?: other.additionalInfo
        )
    }

    enum class FieldType(
        @DrawableRes
        val iconRes: Int
    ) {
        TITLE(R.drawable.ic_song),
        TRACK(R.drawable.ic_song),
        ARTIST(R.drawable.ic_artist),
        ALBUM(R.drawable.ic_album),
        ALBUM_ARTIST(R.drawable.ic_album_artist),
        GENRE(R.drawable.ic_genre),
        YEAR(R.drawable.ic_album),
        DURATION(R.drawable.ic_song)
    }

    enum class ActionType(
        @DrawableRes
        val iconRes: Int
    ) {
        NONE(0),
        INFO_TITLE(0),
        EXPANDABLE_TITLE(R.drawable.ic_next_occurence),
        EXPANDED_TITLE(R.drawable.ic_previous_occurence),
        ADD_TO_FILTER(R.drawable.ic_filter),
        ADD_TO_PLAYLIST(R.drawable.ic_add_to_playlist),
        ADD_NEXT(R.drawable.ic_play_next),
        SEARCH(R.drawable.ic_search),
        DOWNLOAD(R.drawable.ic_download)
    }
}