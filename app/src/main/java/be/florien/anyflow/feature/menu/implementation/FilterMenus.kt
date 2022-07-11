package be.florien.anyflow.feature.menu.implementation

import be.florien.anyflow.R
import be.florien.anyflow.feature.menu.MenuHolder

class SearchMenuHolder(action: () -> Unit): MenuHolder(R.menu.menu_filter_display, R.id.menu_search_filters, action)

class ConfirmMenuHolder(action:() -> Unit): MenuHolder(R.menu.menu_filter_display, R.id.menu_confirm, action)

class SaveFilterGroupMenuHolder(action: () -> Unit): MenuHolder(R.menu.menu_filter_display, R.id.menu_save, action)