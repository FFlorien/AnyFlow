package be.florien.anyflow.feature.menu.implementation

import android.content.Context
import be.florien.anyflow.R
import be.florien.anyflow.feature.menu.AnimatedMenuHolder
import be.florien.anyflow.feature.menu.MenuHolder

class SearchMenuHolder(isSearching: Boolean, context: Context, action: () -> Unit) :
    AnimatedMenuHolder(
        R.menu.menu_filter_display, R.id.menu_search_filters,
        R.drawable.ic_search,
        R.drawable.ic_search_selected,
        !isSearching,
        context,
        action
    )

class ConfirmMenuHolder(action: () -> Unit) :
    MenuHolder(R.menu.menu_filter_display, R.id.menu_confirm, action)

class SaveFilterGroupMenuHolder(action: () -> Unit) :
    MenuHolder(R.menu.menu_filter_display, R.id.menu_save, action)