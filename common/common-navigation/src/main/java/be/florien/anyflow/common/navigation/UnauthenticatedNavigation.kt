package be.florien.anyflow.common.navigation

import android.app.Activity

interface UnauthenticatedNavigation {
    fun goToAuthentication(activity: Activity)
}