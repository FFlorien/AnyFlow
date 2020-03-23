package be.florien.anyflow.feature.menu

import be.florien.anyflow.R

class ConfirmMenuHolder(action:() -> Unit): MenuHolder(R.menu.menu_filter_display, R.id.menu_confirm, action)

class SaveFilterGroupMenuHolder(action: () -> Unit): MenuHolder(R.menu.menu_filter_display, R.id.menu_save, action)