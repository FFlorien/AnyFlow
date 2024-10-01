package be.florien.anyflow.ui.di

import be.florien.anyflow.ui.server.ServerViewModel


interface ServerVmInjectorContainer {
    val  serverVmInjector: ServerVmInjector
}

interface ServerVmInjector {

    fun inject(serverViewModel: ServerViewModel)
}