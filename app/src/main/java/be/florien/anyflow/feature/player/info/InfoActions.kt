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

    sealed class FieldType(
        @DrawableRes
        val iconRes: Int
    ) {
        override fun equals(other: Any?): Boolean {
            return this.javaClass.isInstance(other)
        }

        override fun hashCode(): Int {
            return iconRes
        }
    }

    sealed class ActionType(
        @DrawableRes
        val iconRes: Int
    ) {
        class None : SongActionType(0)
        class InfoTitle : SongActionType(0)
        class ExpandableTitle : SongActionType(R.drawable.ic_next_occurence)
        class ExpandedTitle : SongActionType(R.drawable.ic_previous_occurence)

        override fun equals(other: Any?): Boolean {
            return this.javaClass.isInstance(other)
        }

        override fun hashCode(): Int {
            return iconRes
        }
    }

    sealed class SongFieldType(
        @DrawableRes
        iconRes: Int
    ) : FieldType(iconRes) {

        class Title : SongFieldType(R.drawable.ic_song)
        class Track : SongFieldType(R.drawable.ic_song)
        class Artist : SongFieldType(R.drawable.ic_artist)
        class Album : SongFieldType(R.drawable.ic_album)
        class AlbumArtist : SongFieldType(R.drawable.ic_album_artist)
        class Genre : SongFieldType(R.drawable.ic_genre)
        class Year : SongFieldType(R.drawable.ic_album)
        class Duration : SongFieldType(R.drawable.ic_song)

        companion object {
            fun getClassFromName(name: String) = when (name) {
                Title().javaClass.name -> Title()
                Track().javaClass.name -> Track()
                Artist().javaClass.name -> Artist()
                Album().javaClass.name -> Album()
                AlbumArtist().javaClass.name -> AlbumArtist()
                Genre().javaClass.name -> Genre()
                Year().javaClass.name -> Year()
                Duration().javaClass.name -> Duration()
                else -> null
            }
        }
    }

    sealed class SongActionType(
        @DrawableRes
        iconRes: Int
    ) : ActionType(iconRes) {
        class AddToFilter : SongActionType(R.drawable.ic_filter)
        class AddToPlaylist : SongActionType(R.drawable.ic_add_to_playlist)
        class AddNext : SongActionType(R.drawable.ic_play_next)
        class Search : SongActionType(R.drawable.ic_search)
        class Download : SongActionType(R.drawable.ic_download)

        companion object {
            fun getClassFromName(name: String) = when (name) {
                None().javaClass.name -> None()
                InfoTitle().javaClass.name -> InfoTitle()
                ExpandableTitle().javaClass.name -> ExpandableTitle()
                ExpandedTitle().javaClass.name -> ExpandedTitle()
                AddToFilter().javaClass.name -> AddToFilter()
                AddToPlaylist().javaClass.name -> AddToPlaylist()
                AddNext().javaClass.name -> AddNext()
                Search().javaClass.name -> Search()
                Download().javaClass.name -> Download()
                else -> null
            }
        }
    }
}