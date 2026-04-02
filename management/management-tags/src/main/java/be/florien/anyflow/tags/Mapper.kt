package be.florien.anyflow.tags

import be.florien.anyflow.management.filters.domain.model.FilterTagsCount
import be.florien.anyflow.tags.local.model.DbAlbumDisplay
import be.florien.anyflow.tags.local.model.DbArtist
import be.florien.anyflow.tags.local.model.DbDownloadedCount
import be.florien.anyflow.tags.local.model.DbGenre
import be.florien.anyflow.tags.local.model.DbSongDisplay
import be.florien.anyflow.tags.local.model.DbSongInfo
import be.florien.anyflow.tags.local.model.DbTagsFilterCount
import be.florien.anyflow.tags.model.Album
import be.florien.anyflow.tags.model.Artist
import be.florien.anyflow.tags.model.DownloadedCount
import be.florien.anyflow.tags.model.Genre
import be.florien.anyflow.tags.model.SongDisplayDomain
import be.florien.anyflow.tags.model.SongInfo


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

fun DbSongDisplay.toDomainSongDisplay() = SongDisplayDomain(
    id = id,
    title = title,
    artistName = artistName,
    albumName = albumName,
    albumId = albumId,
    time = time,
    section = titleForSort.first().uppercase()
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
    year = year,
    section = basename.first().uppercase()
)

fun DbGenre.toViewGenre() = Genre(
    id = id,
    name = name
)

fun DbDownloadedCount.toViewDownloadedCount() = DownloadedCount(
    isDownloaded = downloaded == 1,
    count = count
)

fun DbTagsFilterCount.toViewFilterCount() = FilterTagsCount(
    duration = duration,
    genres = genres,
    albumArtists = albumArtists,
    albums = albums,
    artists = artists,
    songs = songs,
    playlists = playlists,
    downloaded = downloaded
)
