package be.florien.anyflow.feature.player.ui

import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.work.Configuration
import androidx.work.WorkManager
import be.florien.anyflow.architecture.di.ActivityScope
import be.florien.anyflow.architecture.di.AnyFlowViewModelFactory
import be.florien.anyflow.architecture.di.ServerScope
import be.florien.anyflow.architecture.di.ViewModelFactoryProvider
import be.florien.anyflow.common.navigation.MainScreen
import be.florien.anyflow.common.navigation.MainScreenSection
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.common.navigation.UnauthenticatedNavigation
import be.florien.anyflow.common.ui.BaseFragment
import be.florien.anyflow.common.ui.isVisiblePresent
import be.florien.anyflow.common.ui.menu.MenuCoordinator
import be.florien.anyflow.common.ui.menu.MenuCoordinatorHolder
import be.florien.anyflow.feature.auth.domain.repository.AuthRepository
import be.florien.anyflow.feature.player.service.PlayerService
import be.florien.anyflow.feature.player.ui.databinding.ActivityPlayerBinding
import be.florien.anyflow.feature.player.ui.di.PlayerActivityComponent
import be.florien.anyflow.feature.player.ui.di.PlayerActivityComponentCreator
import be.florien.anyflow.feature.sync.service.SyncService
import be.florien.anyflow.management.playlist.di.PlaylistWorkerFactory
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Activity controlling the queue, play/pause/next/previous on the PlayerService
 */
@ActivityScope
@ServerScope
class MainActivity : AppCompatActivity(), ViewModelFactoryProvider, MenuCoordinatorHolder,
    MainScreen {

    /**
     * Injection
     */

    private lateinit var activityComponent: PlayerActivityComponent

    @Inject
    override lateinit var viewModelFactory: AnyFlowViewModelFactory

    @Inject
    lateinit var workerFactory: PlaylistWorkerFactory

    @Inject
    lateinit var navigator: Navigator

    private val fakeComponent = object : PlayerActivityComponent {
        override fun inject(mainActivity: MainActivity) {}
    }

    override val menuCoordinator = MenuCoordinator()


    override val containerId: Int = R.id.container
    override val mainScreenFragmentManager: FragmentManager
        get() = supportFragmentManager

    @Inject
    lateinit var mainScreenSections: List<@JvmSuppressWildcards MainScreenSection>

    /**
     * Private properties
     */
    lateinit var viewModel: PlayerViewModel
    private lateinit var binding: ActivityPlayerBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle

    private var isSubtitleConfigured = false

    /**
     * Lifecycle methods
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val component =
            (application as PlayerActivityComponentCreator).createPlayerActivityComponent()
        activityComponent = if (component != null) {
            component
        } else {
            (application as UnauthenticatedNavigation).goToAuthentication(this)
            fakeComponent
            return
        }
        activityComponent.inject(this)
        if (!WorkManager.isInitialized()) {
            WorkManager.initialize(//todo this will maybe cause problem, find a way to initialize in AnyFlowApp
                this,
                Configuration.Builder()
                    .setWorkerFactory(workerFactory)
                    .build()
            )
        }
        viewModel = ViewModelProvider(this, viewModelFactory)[PlayerViewModel::class.java]
        binding = DataBindingUtil.setContentView(this, R.layout.activity_player)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        initToolbar()
        initDrawer()
        initBottomNavigation()
        observeNetwork()

        if (savedInstanceState == null) {
            val firstSection = mainScreenSections.first { it.isFirstSection }
            supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.container,
                    firstSection.createFragment(),
                    firstSection.tag
                )
                .runOnCommit {
                    adaptToolbarToCurrentFragment()
                    adaptBottomNavigationToCurrentFragment()
                }
                .commit()
        }
        viewModel.syncAlarms()
        viewModel.connectionStatus.observe(this) { status ->
            @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
            when (status) {
                AuthRepository.ConnectionStatus.WRONG_SERVER_URL,
                AuthRepository.ConnectionStatus.WRONG_ID_PAIR -> {
                    (application as UnauthenticatedNavigation).goToAuthentication(this)
                }

                AuthRepository.ConnectionStatus.CONNEXION -> return@observe
                AuthRepository.ConnectionStatus.CONNECTED -> {
                    bindService(
                        Intent(this, SyncService::class.java), //todo navigator?
                        viewModel.updateConnection,
                        Context.BIND_AUTO_CREATE
                    )
                }
            }
        }
        viewModel.songsUpdatePercentage.observe(this) {
            if (it in 0..100) {
                binding.updatingText.text = getString(R.string.update_songs, it)
                binding.updatingStateView.isVisiblePresent(true)
            } else {
                binding.updatingStateView.isVisiblePresent(false)
            }
        }
        viewModel.genresUpdatePercentage.observe(this) {
            if (it in 0..100) {
                binding.updatingText.text = getString(R.string.update_genres, it)
                binding.updatingStateView.isVisiblePresent(true)
            } else {
                binding.updatingStateView.isVisiblePresent(false)
            }
        }
        viewModel.albumsUpdatePercentage.observe(this) {
            if (it in 0..100) {
                binding.updatingText.text = getString(R.string.update_albums, it)
                binding.updatingStateView.isVisiblePresent(true)
            } else {
                binding.updatingStateView.isVisiblePresent(false)
            }
        }
        viewModel.artistsUpdatePercentage.observe(this) {
            if (it in 0..100) {
                binding.updatingText.text = getString(R.string.update_artists, it)
                binding.updatingStateView.isVisiblePresent(true)
            } else {
                binding.updatingStateView.isVisiblePresent(false)
            }
        }
        viewModel.playlistsUpdatePercentage.observe(this) {
            if (it in 0..100) {
                binding.updatingText.text = getString(R.string.update_playlists, it)
                binding.updatingStateView.isVisiblePresent(true)
            } else {
                binding.updatingStateView.isVisiblePresent(false)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, PlayerService::class.java))
        val mediaControllerListenableFuture = MediaController
            .Builder(this, sessionToken)
            .buildAsync()
        mediaControllerListenableFuture.addListener({
            viewModel.player = mediaControllerListenableFuture.get()
        }, MoreExecutors.directExecutor())
    }

    override fun onResume() {
        super.onResume()
        adaptToolbarToCurrentFragment()
        adaptBottomNavigationToCurrentFragment()
        lifecycleScope.launch(Dispatchers.Default) {//todo better handling of lifecycle and resource ?
            viewModel.currentDuration.collect {
                if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                    withContext(Dispatchers.Main) {
                        binding.playerControls.currentDuration = it
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuCoordinator.inflateMenus(menu, menuInflater)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menuCoordinator.prepareMenus(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return menuCoordinator.handleMenuClick(item.itemId)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!(application as PlayerActivityComponentCreator).isUserConnected()) {
            return
        }

        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.cancelAll()
    }

    /**
     * Public method
     */

    fun displaySongList() {
        supportFragmentManager.popBackStack(
            LIBRARY_STACK_NAME,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
    }

    /**
     * Private methods
     */

    private fun initToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportFragmentManager.addOnBackStackChangedListener {
            adaptToolbarToCurrentFragment()
            adaptBottomNavigationToCurrentFragment()
        }
    }

    private fun initDrawer() {
        drawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.info_action_download_description,
            R.string.info_action_download
        ) // todo strings
        binding.drawerLayout.addDrawerListener(drawerToggle)
        binding.navigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.menu_alarm -> {
                    navigator.navigateToAlarm(this)
                    true
                }

                R.id.menu_playlist -> {
                    navigator.navigateToPlaylist(this)
                    true
                }

                R.id.menu_shortcut -> {
                    navigator.navigateToShortcut(this)
                    true
                }

                else -> false
            }
        }
        drawerToggle.syncState()
        drawerToggle.setHomeAsUpIndicator(R.drawable.ic_up)
        drawerToggle.setToolbarNavigationClickListener {
            supportFragmentManager.popBackStack()
        }
    }

    private fun initBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            displayFragment(mainScreenSections.first { it.menuId == item.itemId })
            true
        }
    }

    private fun displayFragment(mainScreenSection: MainScreenSection) {
        val fragment =
            supportFragmentManager.findFragmentByTag(mainScreenSection.tag)
                ?: mainScreenSection.createFragment()
        supportFragmentManager
            .beginTransaction().apply {
                replace(R.id.container, fragment, mainScreenSection.tag)
                addToBackStack(LIBRARY_STACK_NAME)
            }
            .commit()
    }

    private fun observeNetwork() {
        val networkRequest = NetworkRequest
            .Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        val connectivityManager =
            getSystemService(ConnectivityManager::class.java) as ConnectivityManager
        connectivityManager.registerNetworkCallback(networkRequest, viewModel.networkCallback)
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val hasInternet =
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
        val isWifi =
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
        val isCellular =
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false
        viewModel.setInternetPresence(hasInternet && (isWifi || isCellular))
    }

    private fun adaptToolbarToCurrentFragment() {
        val baseFragment = supportFragmentManager.findFragmentById(R.id.container) as? BaseFragment
        baseFragment?.getTitle()?.let {
            supportActionBar?.title = it
        }
        val subtitle = baseFragment?.getSubtitle()
        supportActionBar?.subtitle = subtitle
        if (subtitle != null && !isSubtitleConfigured) {
            binding.toolbar.children.forEach { toolbarView ->
                (toolbarView as? TextView)
                    ?.takeIf { it.text == subtitle }
                    ?.ellipsize = TextUtils.TruncateAt.START
            }
        }
    }

    private fun adaptBottomNavigationToCurrentFragment() {
        val fragmentTag = supportFragmentManager.findFragmentById(R.id.container)?.tag
        val menuId = mainScreenSections.firstOrNull { it.tag == fragmentTag }?.menuId ?: return
        binding.bottomNavigationView.menu.findItem(menuId).isChecked = true
    }

    companion object {
        private const val LIBRARY_STACK_NAME = "filters"
    }
}