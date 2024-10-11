package be.florien.anyflow.feature.songlist.ui

import android.content.Context
import be.florien.anyflow.common.ui.menu.AnimatedMenuHolder


class SearchSongMenuHolder(isSearching: Boolean, context: Context, action: () -> Unit) : AnimatedMenuHolder(
        R.menu.menu_player,
        R.id.menu_search_songs,
        R.drawable.ic_search,
        R.drawable.ic_search_selected,
        !isSearching,
        context,
        action)