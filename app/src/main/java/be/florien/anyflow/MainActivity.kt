package be.florien.anyflow

import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import be.florien.anyflow.common.di.ActivityScope
import be.florien.anyflow.common.di.AnyFlowViewModelFactory
import be.florien.anyflow.common.di.ServerScope
import be.florien.anyflow.common.di.ViewModelFactoryProvider
import be.florien.anyflow.common.logging.iLog
import be.florien.anyflow.common.navigation.AlarmList
import be.florien.anyflow.common.navigation.BottomNavDestination
import be.florien.anyflow.common.navigation.ComposeNavigator
import be.florien.anyflow.common.navigation.ConnectedDestination
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.common.navigation.Server
import be.florien.anyflow.common.navigation.rememberNavigationState
import be.florien.anyflow.common.navigation.toEntries
import be.florien.anyflow.common.resources.theming.AppTheme
import be.florien.anyflow.feature.alarm.ui.alarmEntry
import be.florien.anyflow.feature.auth.domain.repository.AuthRepository
import be.florien.anyflow.feature.auth.ui.authenticationEntry
import be.florien.anyflow.feature.filter.current.ui.currentFilterEntry
import be.florien.anyflow.feature.filter.saved.ui.savedFiltersEntry
import be.florien.anyflow.feature.library.ui.libraryEntries
import be.florien.anyflow.feature.mediaList.ui.nowPlayingEntry
import be.florien.anyflow.feature.player.service.PlayerService
import be.florien.anyflow.feature.sync.service.SyncService
import be.florien.anyflow.injection.PlayerActivityComponent
import be.florien.anyflow.injection.PlayerActivityComponentCreator
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.google.common.util.concurrent.MoreExecutors
import okhttp3.OkHttpClient
import javax.inject.Inject

@ActivityScope
@ServerScope
class MainActivity : AppCompatActivity(), ViewModelFactoryProvider {

    private lateinit var activityComponent: PlayerActivityComponent

    @Inject
    @JvmField
    var nullableViewModelFactory: AnyFlowViewModelFactory? = null

    @Inject
    lateinit var legacyNavigator: Navigator

    private val fakeComponent = object : PlayerActivityComponent {
        override fun inject(mainActivity: MainActivity) {}
    }

    override val viewModelFactory: AnyFlowViewModelFactory
        get() {
            if (nullableViewModelFactory == null) {
                injectInActivity()
            }
            return nullableViewModelFactory
                ?: throw IllegalStateException("Cannot inject VMFactory")
        }

    /**
     * Lifecycle methods
     */

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val isUserConnected = remember {
                (application as PlayerActivityComponentCreator).isUserConnected()
            }
            LaunchedEffect(isUserConnected) {
                injectInActivity()
            }
            val navigationState = rememberNavigationState(
                startRoute = if (isUserConnected) BottomNavDestination.NowPlaying else Server,
                topLevelRoutes = if (isUserConnected) BottomNavDestination.items.toSet() + AlarmList + Server else setOf(
                    Server
                )
            )
            val navigator = remember { ComposeNavigator(navigationState) }

            AppTheme {
                val topLevelRoute = navigationState.topLevelRoute
                if (topLevelRoute is ConnectedDestination) {
                    val viewModel = viewModel<MainActivityViewModel>(factory = viewModelFactory)
                    var isSearching by remember { mutableStateOf(false) }
                    val lifecycleOwner = rememberLifecycleOwner()
                    val lifecycleState = lifecycleOwner.lifecycle.currentStateAsState().value

                    var player: MediaController? by remember { mutableStateOf(null) }
                    LaunchedEffect(lifecycleState == Lifecycle.State.CREATED) {
                        if (player != null) {
                            return@LaunchedEffect
                        }

                        val sessionToken = SessionToken(
                            this@MainActivity,
                            ComponentName(this@MainActivity, PlayerService::class.java)
                        )
                        val mediaController = MediaController
                            .Builder(this@MainActivity, sessionToken)
                            .buildAsync()
                        mediaController.addListener(
                            { player = mediaController.get() },
                            MoreExecutors.directExecutor()
                        )
                    }
                    viewModel.player = player

                    setSingletonImageLoaderFactory { context ->
                        ImageLoader.Builder(context).components {
                            add(
                                OkHttpNetworkFetcherFactory(
                                    callFactory = {
                                        OkHttpClient.Builder()
                                            .addInterceptor(viewModel.authenticationInterceptor)
                                            .build()
                                    }
                                )
                            )
                        }.build()
                    }

                    val mainState = viewModel.stateFlow.collectAsState().value

                    LaunchedEffect(mainState.connectionStatus == AuthRepository.ConnectionStatus.CONNECTED) {
                        if (mainState.connectionStatus == AuthRepository.ConnectionStatus.CONNECTED) {
                            bindService(
                                Intent(this@MainActivity, SyncService::class.java),
                                viewModel.updateConnection,
                                BIND_AUTO_CREATE
                            )
                        }
                    }

                    LaunchedEffect(true) {
                        val networkRequest = NetworkRequest
                            .Builder()
                            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                            .build()

                        val connectivityManager =
                            getSystemService(ConnectivityManager::class.java) as ConnectivityManager
                        connectivityManager.registerNetworkCallback(
                            networkRequest,
                            viewModel.networkCallback
                        )
                        val activeNetwork = connectivityManager.activeNetwork
                        val networkCapabilities =
                            connectivityManager.getNetworkCapabilities(activeNetwork)
                        val hasInternet = networkCapabilities
                            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            ?: false
                        val isWifi = networkCapabilities
                            ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                            ?: false
                        val isCellular = networkCapabilities
                            ?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                            ?: false
                        viewModel.setInternetPresence(hasInternet && (isWifi || isCellular))
                    }
                    if (topLevelRoute is BottomNavDestination) {
                        MainScreen(
                            topLevelNavKey = topLevelRoute,
                            mainState = mainState,
                            getActionListener = { viewModel },
                            toggleSearch = { isSearching = !isSearching },
                            changeOrdering = viewModel::changeOrdering,
                            commitFilters = viewModel::commitFiltersChanges,
                            navigateTo = { navigator.navigate(it) },
                            navigateToPlaylist = {
                                legacyNavigator.navigateToPlaylist(this)
                            },
                            navigateToShortcuts = {
                                legacyNavigator.navigateToShortcut(this)
                            }) {
                            NavigationContent(
                                navigationState.toEntries(entryProvider {
                                    authenticationEntry(navigator)
                                    alarmEntry(viewModelFactory, navigator)
                                    libraryEntries(viewModelFactory, navigator)
                                    nowPlayingEntry(
                                        viewModelFactory,
                                        navigator,
                                        snapshotFlow { isSearching }
                                    )
                                    currentFilterEntry(viewModelFactory, navigator)
                                    savedFiltersEntry(viewModelFactory)
                                })
                            ) { navigator.goBack() }

                        }
                    } else {
                        NavigationContent(
                            navigationState.toEntries(entryProvider {
                                authenticationEntry(navigator)
                                alarmEntry(viewModelFactory, navigator)
                                libraryEntries(viewModelFactory, navigator)
                                nowPlayingEntry(
                                    viewModelFactory,
                                    navigator,
                                    snapshotFlow { isSearching }
                                )
                                currentFilterEntry(viewModelFactory, navigator)
                                savedFiltersEntry(viewModelFactory)
                            })
                        ) { navigator.goBack() }
                    }
                } else {
                    NavigationContent(
                        navigationState.toEntries(entryProvider {
                            authenticationEntry(navigator)
                        })
                    ) { navigator.goBack() }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!(application as PlayerActivityComponentCreator).isUserConnected()) {
            return
        }

        val jobScheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.cancelAll()
    }

    /**
     * Private methods
     */

    private fun injectInActivity(): Boolean {
        val component =
            (application as PlayerActivityComponentCreator).createPlayerActivityComponent()
        activityComponent = if (component != null) {
            iLog("ActivityComponent is not null: $component")
            component
        } else {
            iLog("ActivityComponent is null, going to Authentication")
            fakeComponent
        }

        activityComponent.inject(this)

        return activityComponent != fakeComponent
    }

    /**
     * Composables
     */
}

@Composable
private fun NavigationContent(
    entries: SnapshotStateList<NavEntry<NavKey>>,
    goBack: () -> Unit
) {
    NavDisplay(
        modifier = Modifier
            .fillMaxSize(),
        entries = entries,
        onBack = goBack,
        transitionSpec = {
            ContentTransform(
                targetContentEnter = slideInVertically(initialOffsetY = { it }),
                initialContentExit = fadeOut()
            )
        },
        popTransitionSpec = {
            ContentTransform(
                targetContentEnter = fadeIn(),
                initialContentExit = slideOutVertically(targetOffsetY = { it })
            )
        },
        predictivePopTransitionSpec = {
            ContentTransform(
                targetContentEnter = fadeIn(),
                initialContentExit = slideOutVertically(targetOffsetY = { (it * 2) / 3 })
            )
        }
    )
}