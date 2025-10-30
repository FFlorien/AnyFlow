package be.florien.anyflow.feature.library.tags.domain

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import be.florien.anyflow.common.resources.R

enum class LibraryTagsFieldType(
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val titleRes: Int,
    val artType: String?
) {
    Duration(R.drawable.ic_duration, R.string.filter_info_duration, null),
    Genre(R.drawable.ic_genre, R.string.filter_info_genre, null),
    AlbumArtist(R.drawable.ic_album_artist, R.string.filter_info_album_artist, ART_TYPE_ARTIST),
    Album(R.drawable.ic_album, R.string.filter_info_album, ART_TYPE_ALBUM),
    Artist(R.drawable.ic_artist, R.string.filter_info_artist, ART_TYPE_ARTIST),
    Song(R.drawable.ic_song, R.string.filter_info_song, ART_TYPE_SONG),
    Playlist(R.drawable.ic_playlist, R.string.filter_info_playlist, ART_TYPE_PLAYLIST),
    Downloaded(R.drawable.ic_downloaded, R.string.filter_info_downloaded, null);
}

enum class LibraryTagsActionType {
    SubFilter,
    InfoTitle;
}

data class LibraryInfoRow(
    val fieldType: LibraryTagsFieldType,
    val actionType: LibraryTagsActionType,
    val count: Int
)

const val ART_TYPE_SONG = "song"
const val ART_TYPE_ALBUM = "album"
const val ART_TYPE_ARTIST = "artist"
const val ART_TYPE_PLAYLIST = "playlist"