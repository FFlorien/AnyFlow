package be.florien.anyflow.feature.playlist.selection.domain

import be.florien.anyflow.common.ui.data.TagType
import be.florien.anyflow.management.filters.model.FilterType
import be.florien.anyflow.management.filters.model.TagFilterType


fun TagType.toViewFilterType(): FilterType = when (this) {
    TagType.Genre -> TagFilterType.GENRE_IS
    TagType.Title -> TagFilterType.SONG_IS
    TagType.Artist -> TagFilterType.ARTIST_IS
    TagType.AlbumArtist -> TagFilterType.ALBUM_ARTIST_IS
    TagType.Album -> TagFilterType.ALBUM_IS
    TagType.Disk -> TagFilterType.DISK_IS
    TagType.Playlist -> TagFilterType.PLAYLIST_IS
}