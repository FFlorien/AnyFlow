package be.florien.anyflow.view.menu

import be.florien.anyflow.R

class DeleteMenuHolder(action: () -> Unit) : MenuHolder(
        R.menu.menu_saved_filters,
        R.id.delete,
        action)

class EditMenuHolder(action: () -> Unit) : MenuHolder(
        R.menu.menu_saved_filters,
        R.id.edit,
        action)