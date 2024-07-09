package be.florien.anyflow.management.playlist

import be.florien.anyflow.tags.local.model.DbPlaylistWithCount
import be.florien.anyflow.tags.local.model.DbPlaylistWithCountAndPresence
import be.florien.anyflow.management.playlist.model.Playlist
import be.florien.anyflow.management.playlist.model.PlaylistWithPresence


fun DbPlaylistWithCount.toViewPlaylist() =
    Playlist(id, name, songCount)

fun DbPlaylistWithCountAndPresence.toViewPlaylist() =
    PlaylistWithPresence(
        id,
        name,
        songCount,
        presence
    )