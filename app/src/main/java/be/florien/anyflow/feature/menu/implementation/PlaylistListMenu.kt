package be.florien.anyflow.feature.menu.implementation

import be.florien.anyflow.R
import be.florien.anyflow.feature.menu.MenuHolder


class NewPlaylistMenuHolder(action: () -> Unit) : MenuHolder(
    R.menu.menu_playlist_list,
    R.id.menu_new_playlist,
    action
)