package be.florien.anyflow.feature.menu

import be.florien.anyflow.R


class AddAlarmMenuHolder(action: () -> Unit) : MenuHolder(
        R.menu.menu_alarms,
        R.id.menu_add_alarm,
        action)



class ConfirmAlarmMenuHolder(action:() -> Unit): MenuHolder(R.menu.menu_alarms, R.id.menu_confirm_alarm, action)

class DeleteAlarmMenuHolder(action:() -> Unit): MenuHolder(R.menu.menu_alarms, R.id.menu_delete_alarm, action)