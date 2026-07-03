package be.florien.anyflow

import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue.Closed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.currentStateAsState
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
import be.florien.anyflow.component.player.controls.PlayerControls
import be.florien.anyflow.feature.alarm.ui.alarmEntry
import be.florien.anyflow.feature.auth.domain.repository.AuthRepository
import be.florien.anyflow.feature.auth.ui.authenticationEntry
import be.florien.anyflow.feature.filter.current.ui.currentFilterEntry
import be.florien.anyflow.feature.filter.saved.ui.savedFiltersEntry
import be.florien.anyflow.feature.library.ui.libraryEntries
import be.florien.anyflow.feature.mediaList.ui.nowPlayingEntry
import be.florien.anyflow.feature.player.service.PlayerService
import be.florien.anyflow.feature.sync.service.SyncRepository
import be.florien.anyflow.feature.sync.service.SyncService
import be.florien.anyflow.injection.PlayerActivityComponent
import be.florien.anyflow.injection.PlayerActivityComponentCreator
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * Activity controlling the queue, play/pause/next/previous on the PlayerService
 */
@ActivityScope
@ServerScope
class MainActivity : AppCompatActivity(), ViewModelFactoryProvider {

    /**
     * Injection
     */

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
            val scope = rememberCoroutineScope()
            val navigationState = rememberNavigationState(
                startRoute = if (isUserConnected) BottomNavDestination.NowPlaying else Server,
                topLevelRoutes = if (isUserConnected) BottomNavDestination.items.toSet() + AlarmList + Server else setOf(
                    Server
                )
            )

            val navigator = remember { ComposeNavigator(navigationState) }
            val drawerState = rememberDrawerState(initialValue = Closed)

            BackHandler(enabled = drawerState.isOpen) {
                scope.launch {
                    drawerState.close()
                }
            }

            AppTheme {
                val topLevelRoute = navigationState.topLevelRoute
                val entryProvider = if (topLevelRoute is ConnectedDestination) entryProvider {
                    authenticationEntry(navigator)
                    libraryEntries(viewModelFactory, navigator)
                    nowPlayingEntry(viewModelFactory, navigator)
                    currentFilterEntry(viewModelFactory, navigator)
                    savedFiltersEntry(viewModelFactory)
                    alarmEntry(viewModelFactory, navigator)
                } else {
                    entryProvider {
                        authenticationEntry(navigator)
                    }
                }
                if (topLevelRoute is BottomNavDestination) {
                    MainContentFrame(
                        scope = scope,
                        topLevelNavKey = topLevelRoute,
                        drawerState = drawerState,
                        navigateTo = { navigator.navigate(it) },
                        navigateToAlarm = { navigator.navigate(AlarmList) },
                        navigateToPlaylist = {
                            legacyNavigator.navigateToPlaylist(
                                this
                            )
                        },
                        navigateToShortcuts = {
                            legacyNavigator.navigateToShortcut(
                                this
                            )
                        }) {
                        NavigationContent(
                            navigationState.toEntries(entryProvider)
                        ) { navigator.goBack() }
                    }
                } else {
                    NavigationContent(
                        navigationState.toEntries(entryProvider)
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

    @Composable
    private fun MainContentFrame(
        scope: CoroutineScope,
        topLevelNavKey: BottomNavDestination,
        drawerState: DrawerState,
        navigateTo: (NavKey) -> Unit,
        navigateToAlarm: () -> Unit,
        navigateToPlaylist: () -> Unit,
        navigateToShortcuts: () -> Unit,
        content: @Composable () -> Unit
    ) {
        val viewModel = viewModel<MainActivityViewModel>(factory = viewModelFactory)

        var player: MediaController? by remember { mutableStateOf(null) }
        val lifecycleState = lifecycle.currentStateAsState().value


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


        LaunchedEffect(lifecycleState == Lifecycle.State.CREATED) {
            val sessionToken = SessionToken(
                this@MainActivity,
                ComponentName(this@MainActivity, PlayerService::class.java)
            )
            val mediaController =
                MediaController.Builder(this@MainActivity, sessionToken).buildAsync()
            mediaController.addListener({
                player = mediaController.get()
            }, MoreExecutors.directExecutor())
        }

        viewModel.player = player

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
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            val hasInternet =
                networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    ?: false
            val isWifi =
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
            val isCellular =
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    ?: false
            viewModel.setInternetPresence(hasInternet && (isWifi || isCellular))
        }
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    drawerContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Column {
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    stringResource(R.string.menu_alarms),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            },
                            selected = false,
                            onClick = {
                                navigateToAlarm()
                                scope.launch {
                                    drawerState.close()
                                }
                            })
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    stringResource(R.string.menu_playlist),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            },
                            selected = false,
                            onClick = {
                                navigateToPlaylist()
                                scope.launch {
                                    drawerState.close()
                                }
                            })
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    stringResource(R.string.menu_shortcuts),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            },
                            selected = false,
                            onClick = {
                                navigateToShortcuts()
                                scope.launch {
                                    drawerState.close()
                                }
                            })
                    }
                }
            },
        ) {
            Scaffold(
                topBar = {
                    MainTopBar(
                        topLevelNavKey.label,
                        setActions = {
                            Icon(
                                modifier = Modifier
                                    .clickable {
                                        viewModel.changeOrdering()
                                    }
                                    .padding(8.dp),
                                painter = if (mainState.isOrdered) painterResource(R.drawable.ic_order_ordered) else painterResource(
                                    R.drawable.ic_order_random
                                ),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            if (mainState.hasUncommitedChanges) {
                                Icon(
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.commitFiltersChanges()
                                        }
                                        .padding(4.dp),
                                    painter = painterResource(R.drawable.ic_confirm),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        },
                        drawerState,
                        scope
                    )
                },
                bottomBar = {
                    MainBottomBar(
                        viewModel,
                        mainState,
                        topLevelNavKey
                    ) {
                        navigateTo(it)
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier.padding(paddingValues),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    content()
                    if (mainState.connectionStatus != AuthRepository.ConnectionStatus.CONNECTED) {
                        Text(
                            modifier = Modifier.infoModifier(),
                            text = "Connection status is ${mainState.connectionStatus}",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    mainState.update?.let { update ->
                        val stringRes = when (update.subject) {
                            SyncRepository.CHANGE_SONGS -> R.string.update_songs
                            SyncRepository.CHANGE_ARTISTS -> R.string.update_artists
                            SyncRepository.CHANGE_ALBUMS -> R.string.update_albums
                            SyncRepository.CHANGE_GENRES -> R.string.update_genres
                            SyncRepository.CHANGE_PLAYLISTS -> R.string.update_playlists
                            SyncRepository.CHANGE_PODCASTS -> R.string.update_podcasts
                            else -> null
                        }
                        stringRes?.let {
                            val text = if (update.percent > 0) {
                                stringResource(stringRes, update.percent)
                            } else {
                                stringResource(stringRes)
                            }
                            Text(
                                modifier = Modifier.infoModifier(),
                                text = text,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
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
                targetContentEnter = fadeIn(tween(300)),
                initialContentExit = fadeOut(tween(300))
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    titleRes: Int,
    setActions: @Composable RowScope.() -> Unit,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    TopAppBar(
        title = {
            Text(
                stringResource(titleRes),
                color = MaterialTheme.colorScheme.tertiary
            )
        },
        navigationIcon = {
            IconButton(onClick = {
                scope.launch {
                    if (drawerState.isClosed) {
                        drawerState.open()
                    } else {
                        drawerState.close()
                    }
                }
            }) {
                Icon(
                    Icons.Outlined.Menu,
                    tint = MaterialTheme.colorScheme.tertiary,
                    contentDescription = "Menu"
                )
            }
        },
        actions = { setActions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun MainBottomBar(
    viewModel: MainActivityViewModel,
    mainState: MainActivityViewModel.State,
    currentTop: NavKey,
    onBottomItemClick: (NavKey) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        val primary = MaterialTheme.colorScheme.primaryContainer.toArgb()
        val tertiary = MaterialTheme.colorScheme.tertiary.toArgb()
        val outline = MaterialTheme.colorScheme.onPrimary.toArgb()
        val read = MaterialTheme.colorScheme.primaryFixed.toArgb()
        val coming = MaterialTheme.colorScheme.primaryFixedDim.toArgb()
        val disabled = MaterialTheme.colorScheme.scrim.toArgb()
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                PlayerControls(context).apply {
                    val dip = 64f
                    val r = resources
                    val px = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        dip,
                        r.displayMetrics
                    )
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        px.toInt(),
                    )
                    actionListener = viewModel

                    currentDuration = mainState.currentDuration
                    hasPrevious = mainState.hasPrevious

                    isSeekable = mainState.isSeekable
                    shouldShowBuffering =
                        mainState.shouldShowBuffering
                    state = mainState.playbackState
                    totalDuration = mainState.totalDuration
                    waveForm = mainState.waveForm
                    setLayoutProperties(
                        PlayerControls.LayoutProperties(
                            smallestButtonWidth = context.resources.getDimension(
                                R.dimen.minClickableSize
                            ),
                            iconColor = tertiary,
                            outlineColor = outline,
                            readBarsColor = read,
                            comingBarsColor = coming,
                            previousBackgroundColor = primary,
                            nextBackgroundColor = primary,
                            progressBackgroundColor = primary,
                            disabledColor = disabled,
                            minimumDurationForSeek = 3000
                        )
                    )
                }
            },
            update = { view ->
                view.apply {
                    currentDuration = mainState.currentDuration
                    hasPrevious = mainState.hasPrevious

                    isSeekable = mainState.isSeekable
                    shouldShowBuffering =
                        mainState.shouldShowBuffering
                    state = mainState.playbackState
                    totalDuration = mainState.totalDuration
                    waveForm = mainState.waveForm
                }
            }
        )
        NavigationBar(
            windowInsets = NavigationBarDefaults.windowInsets,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            BottomNavDestination.items.forEach { destinationMain ->
                NavigationBarItem(
                    selected = currentTop == destinationMain,
                    onClick = { onBottomItemClick(destinationMain) },
                    icon = {
                        Icon(
                            modifier = Modifier.padding(4.dp),
                            painter = painterResource(destinationMain.icon),
                            contentDescription = stringResource(destinationMain.label),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(destinationMain.label),
                            color = MaterialTheme.colorScheme.tertiary,
                            textAlign = TextAlign.Center
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun Modifier.infoModifier(): Modifier = this
    .padding(8.dp)
    .background(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        MaterialTheme.shapes.medium
    )
    .padding(16.dp)
    .zIndex(1F)