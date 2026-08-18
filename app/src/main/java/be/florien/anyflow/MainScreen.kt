package be.florien.anyflow

import android.util.TypedValue
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue.Closed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.navigation3.runtime.NavKey
import be.florien.anyflow.common.navigation.AlarmList
import be.florien.anyflow.common.navigation.BottomNavDestination
import be.florien.anyflow.common.resources.component.ActionIcon
import be.florien.anyflow.common.resources.component.BlueTopAppBarMain
import be.florien.anyflow.component.player.controls.PlayerControls
import be.florien.anyflow.feature.auth.domain.repository.AuthRepository
import be.florien.anyflow.feature.sync.service.SyncRepository
import kotlinx.coroutines.launch


@Composable
fun MainScreen(
    topLevelNavKey: BottomNavDestination,
    mainState: MainActivityViewModel.State,
    getActionListener: () -> PlayerControls.OnActionListener,
    toggleSearch: () -> Unit,
    changeOrdering: () -> Unit,
    commitFilters: () -> Unit,
    navigateTo: (NavKey) -> Unit,
    navigateToPlaylist: () -> Unit,
    navigateToShortcuts: () -> Unit,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch {
            drawerState.close()
        }
    }
    ModalNavigationDrawer(
        gesturesEnabled = false,
        drawerState = drawerState,
        drawerContent = {
            LeftDrawerMenu(
                closeDrawer = {
                    scope.launch {
                        drawerState.close()
                    }
                },
                navigateTo = navigateTo,
                navigateToPlaylist = navigateToPlaylist,
                navigateToShortcuts = navigateToShortcuts
            )
        },
    ) {
        Scaffold(
            topBar = {
                MainTopBar(
                    mainState = mainState,
                    topLevelNavKey = topLevelNavKey,
                    toggleDrawer = {
                        scope.launch {
                            if (drawerState.isClosed) {
                                drawerState.open()
                            } else {
                                drawerState.close()
                            }
                        }
                    },
                    toggleSearch = toggleSearch,
                    changeOrdering = changeOrdering,
                    commitFilters = {
                        commitFilters()
                        navigateTo(BottomNavDestination.NowPlaying)
                    }
                )
            },
            bottomBar = {
                MainBottomBar(
                    mainState = mainState,
                    currentTop = topLevelNavKey,
                    getActionListener = getActionListener
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
                UpdateLabel(mainState.connectionStatus, mainState.update)
            }
        }
    }
}

@Composable
private fun MainTopBar(
    mainState: MainActivityViewModel.State,
    topLevelNavKey: BottomNavDestination,
    toggleDrawer: () -> Unit,
    toggleSearch: () -> Unit,
    changeOrdering: () -> Unit,
    commitFilters: () -> Unit
) {
    BlueTopAppBarMain(
        title = stringResource(topLevelNavKey.label),
        onHamburgerMenuClicked = toggleDrawer,
        actions = {
            if (topLevelNavKey.isSearchable) {
                ActionIcon(
                    painter = { painterResource(R.drawable.ic_search) },
                    contentDescription = null,
                    onClick = toggleSearch
                )
            }
            ActionIcon(
                painter = {
                    if (mainState.isOrdered) {
                        painterResource(R.drawable.ic_order_ordered)
                    } else {
                        painterResource(R.drawable.ic_order_random)
                    }
                },
                contentDescription = null,
                onClick = changeOrdering
            )
            if (mainState.hasUncommitedChanges) {
                ActionIcon(
                    painter = { painterResource(R.drawable.ic_confirm) },
                    contentDescription = null,
                    onClick = commitFilters
                )
            }
        }
    )
}

@Composable
private fun LeftDrawerMenu(
    closeDrawer: () -> Unit,
    navigateTo: (NavKey) -> Unit,
    navigateToPlaylist: () -> Unit,
    navigateToShortcuts: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.primaryContainer,
        drawerContentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Column {
            SubDrawerMenu(
                stringResource(R.string.menu_alarms),
                closeDrawer
            ) { navigateTo(AlarmList) }
            SubDrawerMenu(
                stringResource(R.string.menu_alarms),
                closeDrawer
            ) { navigateToPlaylist() }
            SubDrawerMenu(
                stringResource(R.string.menu_shortcuts),
                closeDrawer
            ) { navigateToShortcuts() }
        }
    }
}

@Composable
private fun SubDrawerMenu(
    title: String,
    closeDrawer: () -> Unit,
    navigate: () -> Unit
) {
    NavigationDrawerItem(
        label = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        },
        selected = false,
        onClick = {
            navigate()
            closeDrawer()
        })
}

@Composable
private fun MainBottomBar(
    mainState: MainActivityViewModel.State,
    currentTop: BottomNavDestination,
    getActionListener: () -> PlayerControls.OnActionListener,
    changeBottomSection: (NavKey) -> Unit
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
                    val height = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        64f,
                        resources.displayMetrics
                    )
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        height.toInt(),
                    )
                    actionListener = getActionListener()

                    currentDuration = mainState.currentDuration
                    hasPrevious = mainState.hasPrevious
                    isSeekable = mainState.isSeekable
                    shouldShowBuffering = mainState.shouldShowBuffering
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
                    shouldShowBuffering = mainState.shouldShowBuffering
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
                    onClick = { changeBottomSection(destinationMain) },
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
private fun UpdateLabel(
    connectionStatus: AuthRepository.ConnectionStatus,
    dbUpdate: SyncRepository.PercentageUpdate?
) {
    if (connectionStatus != AuthRepository.ConnectionStatus.CONNECTED) {
        val label = when (connectionStatus) {
            AuthRepository.ConnectionStatus.WRONG_SERVER_URL -> R.string.connect_error_connectivity
            AuthRepository.ConnectionStatus.WRONG_ID_PAIR -> R.string.connect_error_auth_pair
            AuthRepository.ConnectionStatus.CONNEXION -> R.string.connect_label
        }
        Text(
            modifier = Modifier.infoModifier(),
            text = stringResource(label),
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
    dbUpdate?.let { update ->
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

@Composable
private fun Modifier.infoModifier(): Modifier = this
    .padding(8.dp)
    .background(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        MaterialTheme.shapes.medium
    )
    .padding(16.dp)
    .zIndex(1F)