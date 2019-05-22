package be.florien.anyflow.view.menu

import be.florien.anyflow.R

class RollbackMenuHolder(action:() -> Unit): MenuHolder(R.menu.menu_filter, R.id.menu_save, action)

class SaveFilterGroupMenuHolder(action: () -> Unit): MenuHolder(R.menu.menu_filter_display, R.id.menu_save, action)