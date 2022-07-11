package be.florien.anyflow.feature.menu.implementation

import be.florien.anyflow.R
import be.florien.anyflow.feature.menu.MenuHolder


class AddAlarmMenuHolder(action: () -> Unit) : MenuHolder(
        R.menu.menu_alarms,
        R.id.menu_add_alarm,
        action)



class ConfirmAlarmMenuHolder(action:() -> Unit): MenuHolder(R.menu.menu_alarms, R.id.menu_confirm_alarm, action)

class DeleteAlarmMenuHolder(action:() -> Unit): MenuHolder(R.menu.menu_alarms, R.id.menu_delete_alarm, action)