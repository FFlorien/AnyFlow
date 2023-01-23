package be.florien.anyflow.feature.player.info

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import be.florien.anyflow.R
import be.florien.anyflow.extension.ImageConfig

abstract class InfoActions<T> {

    abstract suspend fun getInfoRows(infoSource: T): List<InfoRow>

    abstract suspend fun getActionsRows(
        infoSource: T,
        fieldType: FieldType
    ): List<InfoRow>

    data class InfoRow(
        @StringRes val title: Int,
        val text: String?,
        @StringRes val textRes: Int?,
        val fieldType: FieldType,
        val actionType: ActionType,
        val additionalInfo: Int? = null,
        val progress: LiveData<Int>? = null
    ) {
        constructor(other: InfoRow, order: Int? = null) : this(
            other.title,
            other.text,
            other.textRes,
            other.fieldType,
            other.actionType,
            order ?: other.additionalInfo,
            other.progress,
        )
    }

    sealed class FieldType(
        val imageConfig: ImageConfig
    ) {
        override fun equals(other: Any?): Boolean {
            return this.javaClass.isInstance(other)
        }

        override fun hashCode(): Int {
            return imageConfig.hashCode()
        }
    }

    sealed class ActionType(@DrawableRes val iconRes: Int) {
        class None : ActionType(0)
        class InfoTitle : ActionType(0)
        class ExpandableTitle : ActionType(R.drawable.ic_next_occurence)
        class ExpandedTitle : ActionType(R.drawable.ic_previous_occurence)

        override fun equals(other: Any?): Boolean {
            return this.javaClass.isInstance(other)
        }

        override fun hashCode(): Int {
            return iconRes
        }
    }

    sealed class SongFieldType(@DrawableRes iconRes: Int) : FieldType(ImageConfig(null, iconRes)) {

        class Title : SongFieldType(R.drawable.ic_song)
        class Track : SongFieldType(R.drawable.ic_track)
        class Artist : SongFieldType(R.drawable.ic_artist)
        class Album : SongFieldType(R.drawable.ic_album)
        class AlbumArtist : SongFieldType(R.drawable.ic_album_artist)
        class Genre : SongFieldType(R.drawable.ic_genre)
        class Year : SongFieldType(R.drawable.ic_year)
        class Duration : SongFieldType(R.drawable.ic_duration)

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
                AddToFilter().javaClass.name -> AddToFilter()
                AddToPlaylist().javaClass.name -> AddToPlaylist()
                AddNext().javaClass.name -> AddNext()
                Search().javaClass.name -> Search()
                Download().javaClass.name -> Download()
                else -> null
            }
        }
    }

    sealed class LibraryFieldType(
        @DrawableRes
        iconRes: Int,
        url: String?
    ) : FieldType(ImageConfig(url, iconRes)) {
        class Duration : LibraryFieldType(R.drawable.ic_duration, null)
        class Genre(url: String? = null) : LibraryFieldType(R.drawable.ic_genre, url)
        class AlbumArtist(url: String? = null) : LibraryFieldType(R.drawable.ic_album_artist, url)
        class Album(url: String? = null) : LibraryFieldType(R.drawable.ic_album, url)
        class Artist(url: String? = null) : LibraryFieldType(R.drawable.ic_artist, url)
        class Song(url: String? = null) : LibraryFieldType(R.drawable.ic_song, url)
        class Playlist(url: String? = null) : LibraryFieldType(R.drawable.ic_playlist, url)
    }

    sealed class LibraryActionType(
        @DrawableRes
        iconRes: Int
    ) : ActionType(iconRes) {
        class SubFilter : LibraryActionType(R.drawable.ic_go)
    }
}