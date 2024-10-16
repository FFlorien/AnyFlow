package be.florien.anyflow.feature.auth.ui.di

import be.florien.anyflow.feature.auth.ui.server.ServerViewModel

interface ServerViewModelInjector {
    fun inject(viewModel: ServerViewModel)
}