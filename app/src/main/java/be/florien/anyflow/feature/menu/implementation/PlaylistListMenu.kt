package be.florien.anyflow.feature.menu.implementation

import android.content.Context
import be.florien.anyflow.R
import be.florien.anyflow.feature.menu.AnimatedMenuHolder
import be.florien.anyflow.feature.menu.MenuHolder


class NewPlaylistMenuHolder(action: () -> Unit) : MenuHolder(
    R.menu.menu_playlist_list,
    R.id.menu_new_playlist,
    action
)

class DeletePlaylistMenuHolder(action: () -> Unit) : MenuHolder(
    R.menu.menu_playlist_list,
    R.id.menu_delete_playlist,
    action
)

class PlayPlaylistMenuHolder(action: () -> Unit) : MenuHolder(
    R.menu.menu_playlist_list,
    R.id.menu_play_playlist,
    action
)

class SelectionModeMenuHolder(isSelectMode: Boolean, context: Context, action: () -> Unit) : AnimatedMenuHolder(
    R.menu.menu_playlist_list,
    R.id.menu_select_playlist,
    R.drawable.ic_selection_mode,
    R.drawable.ic_selection_mode_selected,
    isSelectMode,
    context,
    action)