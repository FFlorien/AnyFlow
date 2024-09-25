package be.florien.anyflow.management.playlist

import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.management.playlist.model.Playlist
import be.florien.anyflow.tags.local.model.DbPlaylistWithCount
import be.florien.anyflow.tags.local.model.DbPlaylistWithCountAndPresence
import be.florien.anyflow.management.playlist.model.PlaylistWithCount
import be.florien.anyflow.management.playlist.model.PlaylistSong
import be.florien.anyflow.management.playlist.model.PlaylistWithPresence
import be.florien.anyflow.tags.UrlRepository
import be.florien.anyflow.resources.R
import be.florien.anyflow.tags.local.model.DbPlaylist
import be.florien.anyflow.tags.local.model.DbSongDisplay


fun DbPlaylist.toViewPlaylist(urlRepository: UrlRepository) =
    Playlist(
        id,
        name,
        ImageConfig(urlRepository.getPlaylistArtUrl(id), R.drawable.ic_playlist)
    )

fun DbPlaylistWithCount.toViewPlaylist(urlRepository: UrlRepository) =
    PlaylistWithCount(
        id,
        name,
        songCount,
        ImageConfig(urlRepository.getPlaylistArtUrl(id), R.drawable.ic_playlist)
    )

fun DbPlaylistWithCountAndPresence.toViewPlaylist(urlRepository: UrlRepository) =
    PlaylistWithPresence(
        id,
        name,
        songCount,
        presence,
        ImageConfig(urlRepository.getPlaylistArtUrl(id), R.drawable.ic_playlist)
    )

fun DbSongDisplay.toViewPlaylistSong() = PlaylistSong(
    id = id,
    title = title,
    artistName = artistName,
    albumName = albumName,
    albumId = albumId,
    time = time
)