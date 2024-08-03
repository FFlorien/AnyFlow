package be.florien.anyflow.ui.di

import be.florien.anyflow.ui.user.UserConnectViewModel


interface UserVmInjectorContainer {
    val userVmInjector: UserVmInjector?

    suspend fun createServerComponentIfServerValid(serverUrl: String): Boolean
}

interface UserVmInjector {

    fun inject(userConnectViewModel: UserConnectViewModel)
}