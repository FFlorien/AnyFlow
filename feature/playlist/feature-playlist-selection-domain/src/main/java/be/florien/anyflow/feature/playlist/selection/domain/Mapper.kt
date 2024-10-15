package be.florien.anyflow.feature.playlist.selection.domain

import be.florien.anyflow.common.ui.data.TagType
import be.florien.anyflow.management.filters.model.Filter


fun TagType.toViewFilterType(): Filter.FilterType = when (this) {
    TagType.Genre -> be.florien.anyflow.management.filters.model.Filter.FilterType.GENRE_IS
    TagType.Title -> be.florien.anyflow.management.filters.model.Filter.FilterType.SONG_IS
    TagType.Artist -> be.florien.anyflow.management.filters.model.Filter.FilterType.ARTIST_IS
    TagType.AlbumArtist -> be.florien.anyflow.management.filters.model.Filter.FilterType.ALBUM_ARTIST_IS
    TagType.Album -> be.florien.anyflow.management.filters.model.Filter.FilterType.ALBUM_IS
    TagType.Disk -> be.florien.anyflow.management.filters.model.Filter.FilterType.DISK_IS
    TagType.Playlist -> be.florien.anyflow.management.filters.model.Filter.FilterType.PLAYLIST_IS
}