package be.florien.anyflow.feature.library.tags.ui.menu

import android.content.Context
import be.florien.anyflow.common.ui.menu.AnimatedMenuHolder
import be.florien.anyflow.common.ui.menu.MenuHolder
import be.florien.anyflow.feature.library.ui.R

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

class SelectAllMenuHolder(action: () -> Unit) :
    MenuHolder(R.menu.menu_filter_display, R.id.menu_select_all, action)

class SelectNoneMenuHolder(action: () -> Unit) :
    MenuHolder(R.menu.menu_filter_display, R.id.menu_select_none, action)