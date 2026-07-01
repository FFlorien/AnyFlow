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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
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
import be.florien.anyflow.common.navigation.BottomNavDestination
import be.florien.anyflow.common.navigation.ComposeNavigator
import be.florien.anyflow.common.navigation.TopDestination
import be.florien.anyflow.common.navigation.UnauthenticatedNavigation
import be.florien.anyflow.common.navigation.rememberNavigationState
import be.florien.anyflow.common.navigation.toEntries
import be.florien.anyflow.common.resources.theming.AppTheme
import be.florien.anyflow.component.player.controls.PlayerControls
import be.florien.anyflow.feature.auth.domain.repository.AuthRepository
import be.florien.anyflow.feature.filter.current.ui.currentFilterEntry
import be.florien.anyflow.feature.filter.saved.ui.savedFiltersEntry
import be.florien.anyflow.feature.library.ui.libraryEntries
import be.florien.anyflow.feature.mediaList.ui.nowPlayingEntry
import be.florien.anyflow.feature.player.service.PlayerService
import be.florien.anyflow.feature.sync.service.SyncRepository
import be.florien.anyflow.feature.sync.service.SyncService
import be.florien.anyflow.injection.PlayerActivityComponent
import be.florien.anyflow.injection.PlayerActivityComponentCreator
import com.google.common.util.concurrent.MoreExecutors
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

        if (!injectInActivity()) { //todo: multiple components in
            return
        }
        setContent {
            val viewModel = viewModel<MainActivityViewModel>(factory = viewModelFactory)

            var player: MediaController? by remember { mutableStateOf(null) }

            LaunchedEffect(viewModel) { // todo regard to lifecycle
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
            val navigationState = rememberNavigationState(
                startRoute = BottomNavDestination.NowPlaying,
                topLevelRoutes = BottomNavDestination.items.toSet()
            )

            val navigator = remember { ComposeNavigator(navigationState) }

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

            AppTheme {
                Scaffold(
                    topBar = {
                        if ((navigationState.topLevelRoute as TopDestination).isMainScreen) {
                            MainTopBar(
                                (navigationState.topLevelRoute as BottomNavDestination).label
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.changeOrdering()
                                        }
                                        .padding(8.dp),
                                    painter = if (mainState.isOrdered) painterResource(R.drawable.ic_order_ordered) else painterResource(R.drawable.ic_order_random),
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
                            }
                        }
                    },
                    bottomBar = {
                        if ((navigationState.topLevelRoute as TopDestination).isMainScreen) {
                            MainBottomBar(
                                viewModel,
                                mainState,
                                navigationState.topLevelRoute
                            ) {
                                navigator.navigate(route = it)
                            }
                        }
                    }
                ) { paddingValues ->
                    val entryProvider: (NavKey) -> NavEntry<NavKey> = entryProvider {
                        libraryEntries(viewModelFactory, navigator)
                        nowPlayingEntry(viewModelFactory, navigator)
                        currentFilterEntry(viewModelFactory, navigator)
                        savedFiltersEntry(viewModelFactory)
                    }
                    Box(
                        modifier = Modifier
                            .padding(paddingValues),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        NavDisplay(
                            modifier = Modifier
                                .fillMaxSize(),
                            entries = navigationState.toEntries(entryProvider),
                            onBack = {
                                navigator.goBack()
                            },
                            transitionSpec = {
                                ContentTransform(
                                    targetContentEnter = fadeIn(tween(300)),
                                    initialContentExit = fadeOut(tween(300))
                                )
                            }
                        )
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
            (application as UnauthenticatedNavigation).goToAuthentication(this)
            fakeComponent
        }

        activityComponent.inject(this)

        return activityComponent != fakeComponent
    }
//
//    private fun initDrawer() {
//        drawerToggle = ActionBarDrawerToggle(
//            this,
//            binding.drawerLayout,
//            binding.toolbar,
//            R.string.info_action_download_description,
//            R.string.info_action_download
//        ) // todo strings
//        binding.drawerLayout.addDrawerListener(drawerToggle)
//        binding.navigationView.setNavigationItemSelectedListener {
//            when (it.itemId) {
//                R.id.menu_alarm -> {
//                    navigator.navigateToAlarm(this)
//                    true
//                }
//
//                R.id.menu_playlist -> {
//                    navigator.navigateToPlaylist(this)
//                    true
//                }
//
//                R.id.menu_shortcut -> {
//                    navigator.navigateToShortcut(this)
//                    true
//                }
//
//                else -> false
//            }
//        }
//        drawerToggle.syncState()
//        drawerToggle.setHomeAsUpIndicator(R.drawable.ic_up)
//        drawerToggle.setToolbarNavigationClickListener {
//            supportFragmentManager.popBackStack()
//        }
//    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    titleRes: Int,
    setActions: @Composable RowScope.() -> Unit
) {
    TopAppBar(
        title = {
            Text(
                stringResource(titleRes),
                color = MaterialTheme.colorScheme.tertiary
            )
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