package be.florien.anyflow.feature.menu.implementation

import be.florien.anyflow.R
import be.florien.anyflow.feature.menu.MenuHolder

class RemoveSongsMenuHolder(action: () -> Unit) : MenuHolder(
    R.menu.menu_playlist_songs,
    R.id.menu_remove_playlist,
    action
)