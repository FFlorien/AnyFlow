package be.florien.anyflow.feature.auth.ui

import androidx.activity.compose.LocalActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import be.florien.anyflow.common.navigation.Authentication
import be.florien.anyflow.common.navigation.BottomNavDestination
import be.florien.anyflow.common.navigation.ComposeNavigator
import be.florien.anyflow.common.navigation.Server
import be.florien.anyflow.feature.auth.ui.di.AuthenticationViewModelComponentCreator
import be.florien.anyflow.feature.auth.ui.di.ServerViewModelInjector
import be.florien.anyflow.feature.auth.ui.server.ServerScreen
import be.florien.anyflow.feature.auth.ui.server.ServerState
import be.florien.anyflow.feature.auth.ui.server.ServerViewModel
import be.florien.anyflow.feature.auth.ui.user.AuthenticationScreen
import be.florien.anyflow.feature.auth.ui.user.AuthenticationState
import be.florien.anyflow.feature.auth.ui.user.AuthenticationViewModel


fun EntryProviderScope<NavKey>.authenticationEntry(
    navigator: ComposeNavigator
) {
    entry<Server> {
        val viewModel = viewModel<ServerViewModel>()
        (LocalActivity.current?.application as ServerViewModelInjector).inject(viewModel)
        val serverState = viewModel.urlStatus.collectAsStateWithLifecycle(ServerState.Idle).value
        ServerScreen(
            serverState,
            viewModel::connect,
            { navigator.navigate(Authentication) },
            viewModel::messageRead
        )
    }
    entry<Authentication> {
        val userConnectVMComponent =
            (LocalActivity.current?.application as AuthenticationViewModelComponentCreator).createUserConnectComponent()
                ?: throw IllegalStateException()
        val viewModel = viewModel<AuthenticationViewModel>()
        userConnectVMComponent.inject(viewModel)


        val state = viewModel.state.collectAsStateWithLifecycle(AuthenticationState()).value
        AuthenticationScreen(
            state,
            viewModel::connectWithUserPasswordOrApiToken,
            viewModel::connectWithUserPassword,
            viewModel::connectWithApiToken,
            {
                navigator.navigate(
                    BottomNavDestination.NowPlaying
                )
            })
    }
}