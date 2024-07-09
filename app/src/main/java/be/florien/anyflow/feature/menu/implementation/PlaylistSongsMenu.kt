package be.florien.anyflow.feature.menu.implementation

import be.florien.anyflow.R
import be.florien.anyflow.common.ui.menu.MenuHolder

class RemoveSongsMenuHolder(action: () -> Unit) : MenuHolder(
    R.menu.menu_playlist_songs,
    R.id.menu_remove_playlist,
    action
)

class PlayPlaylistSongsMenuHolder(action: () -> Unit) : MenuHolder(
    R.menu.menu_playlist_songs,
    R.id.menu_play_playlist,
    action
)