package be.florien.anyflow.view.menu

import be.florien.anyflow.R

class ConfirmMenuHolder(action:() -> Unit): MenuHolder(R.menu.menu_filter, R.id.menu_confirm, action)

class CancelMenuHolder(action:() -> Unit): MenuHolder(R.menu.menu_filter, R.id.menu_cancel, action)