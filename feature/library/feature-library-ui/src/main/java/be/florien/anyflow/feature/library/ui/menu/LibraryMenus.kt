package be.florien.anyflow.feature.library.ui.menu

import be.florien.anyflow.component.menu.MenuHolder
import be.florien.anyflow.feature.library.ui.R

class ConfirmMenuHolder(action: () -> Unit) :
    MenuHolder(R.menu.menu_filter_display, R.id.menu_confirm, action)