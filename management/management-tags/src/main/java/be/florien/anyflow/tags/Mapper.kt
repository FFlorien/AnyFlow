package be.florien.anyflow.tags

import be.florien.anyflow.tags.local.model.DbAlbumDisplay
import be.florien.anyflow.tags.local.model.DbArtist
import be.florien.anyflow.tags.local.model.DbGenre
import be.florien.anyflow.tags.local.model.DbSongDisplay
import be.florien.anyflow.tags.local.model.DbTagsFilterCount
import be.florien.anyflow.tags.local.model.DbSongInfo
import be.florien.anyflow.tags.local.query.QueryFilter
import be.florien.anyflow.tags.model.Album
import be.florien.anyflow.tags.model.Artist
import be.florien.anyflow.tags.model.Genre
import be.florien.anyflow.tags.model.SongDisplay
import be.florien.anyflow.tags.model.SongInfo
import be.florien.anyflow.management.filters.model.Filter
import be.florien.anyflow.management.filters.model.FilterTagsCount
import kotlin.time.DurationUnit
import kotlin.time.toDuration


fun DbSongInfo.toViewSongInfo() = SongInfo(
    id = song.id,
    track = song.track,
    title = song.title,
    artistName = artist.name,
    artistId = song.artistId,
    albumName = album.album.name,
    albumId = song.albumId,
    disk = song.disk,
    albumArtistName = album.artist.name,
    albumArtistId = album.artist.id,
    genreNames = genres.map { it.name },
    genreIds = genres.map { it.id },
    playlistNames = playlists.map { it.name },
    playlistIds = playlists.map { it.id },
    time = song.time,
    year = song.year,
    size = song.size,
    local = song.local
)

fun DbSongDisplay.toViewSongDisplay() = SongDisplay(
    id = id,
    title = title,
    artistName = artistName,
    albumName = albumName,
    albumId = albumId,
    time = time
)

fun DbArtist.toViewArtist() = Artist(
    id = id,
    name = name,
    basename = basename
)

fun DbAlbumDisplay.toViewAlbum() = Album(
    id = albumId,
    name = albumName,
    albumArtistName = albumArtistName,
    year = year
)

fun DbGenre.toViewGenre() = Genre(
    id = id,
    name = name
)

fun DbTagsFilterCount.toViewFilterCount() = FilterTagsCount(
    duration = duration.toDuration(DurationUnit.SECONDS),
    genres = genres,
    albumArtists = albumArtists,
    albums = albums,
    artists = artists,
    songs = songs,
    playlists = playlists
)


// region view to utilities

fun Filter<*>.toQueryFilter(level: Int = 0): QueryFilter {
    val argument = argument
    return QueryFilter(
        type = when (type) {
            Filter.FilterType.GENRE_IS -> QueryFilter.FilterType.GENRE_IS
            Filter.FilterType.SONG_IS -> QueryFilter.FilterType.SONG_IS
            Filter.FilterType.ARTIST_IS -> QueryFilter.FilterType.ARTIST_IS
            Filter.FilterType.ALBUM_ARTIST_IS -> QueryFilter.FilterType.ALBUM_ARTIST_IS
            Filter.FilterType.ALBUM_IS -> QueryFilter.FilterType.ALBUM_IS
            Filter.FilterType.DISK_IS -> QueryFilter.FilterType.DISK_IS
            Filter.FilterType.PLAYLIST_IS -> QueryFilter.FilterType.PLAYLIST_IS
            Filter.FilterType.DOWNLOADED_STATUS_IS -> QueryFilter.FilterType.DOWNLOADED_STATUS_IS
            Filter.FilterType.PODCAST_EPISODE_IS -> QueryFilter.FilterType.PODCAST_EPISODE_IS
        },
        argument = when (argument) {
            is Boolean -> if (argument) "NOT NULL" else "NULL"
            else -> argument.toString()
        },
        level = level,
        children = children.map { it.toQueryFilter(level + 1) }
    )
}

fun List<Filter<*>>.toQueryFilters() = map { it.toQueryFilter() }