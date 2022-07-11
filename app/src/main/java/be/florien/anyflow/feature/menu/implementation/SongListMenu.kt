package be.florien.anyflow.feature.menu.implementation

import android.content.Context
import be.florien.anyflow.R
import be.florien.anyflow.feature.menu.AnimatedMenuHolder
import be.florien.anyflow.feature.menu.MenuHolder


class FilterMenuHolder(action: () -> Unit) : MenuHolder(
        R.menu.menu_player,
        R.id.menu_filters,
        action)

class SearchSongMenuHolder(action: () -> Unit) : MenuHolder(
        R.menu.menu_player,
        R.id.menu_search_songs,
        action)

class OrderMenuHolder(isOrdered: Boolean, context: Context, action: () -> Unit) : AnimatedMenuHolder(
        R.menu.menu_player,
        R.id.menu_order,
        R.drawable.ic_order_ordered,
        R.drawable.ic_order_random,
        isOrdered,
        context,
        action)