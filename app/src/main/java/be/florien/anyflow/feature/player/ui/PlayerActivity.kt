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
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.work.Configuration
import androidx.work.WorkManager
import be.florien.anyflow.AnyFlowApp
import be.florien.anyflow.R
import be.florien.anyflow.architecture.di.ActivityScope
import be.florien.anyflow.architecture.di.AnyFlowViewModelFactory
import be.florien.anyflow.architecture.di.ServerScope
import be.florien.anyflow.architecture.di.ViewModelFactoryProvider
import be.florien.anyflow.common.ui.BaseFragment
import be.florien.anyflow.common.ui.isVisiblePresent
import be.florien.anyflow.common.ui.menu.MenuCoordinator
import be.florien.anyflow.common.ui.menu.MenuCoordinatorHolder
import be.florien.anyflow.databinding.ActivityPlayerBinding
import be.florien.anyflow.feature.auth.domain.repository.AuthRepository
import be.florien.anyflow.feature.library.podcast.ui.info.LibraryPodcastInfoFragment
import be.florien.anyflow.feature.library.tags.ui.info.LibraryTagsInfoFragment
import be.florien.anyflow.feature.library.ui.BaseFilteringFragment
import be.florien.anyflow.feature.player.service.PlayerService
import be.florien.anyflow.feature.filter.current.ui.CurrentFilterFragment
import be.florien.anyflow.feature.playlist.PlaylistsActivity
import be.florien.anyflow.feature.shortcut.ui.ShortcutsActivity
import be.florien.anyflow.feature.song.base.ui.di.SongViewModelProvider
import be.florien.anyflow.feature.song.ui.SongInfoViewModel
import be.florien.anyflow.feature.songlist.ui.OrderMenuHolder
import be.florien.anyflow.injection.PlayerComponent
import be.florien.anyflow.injection.ViewModelFactoryHolder
import be.florien.anyflow.management.playlist.di.PlaylistWorkerFactory
import be.florien.anyflow.ui.server.ServerActivity
import be.florien.anyflow.utils.startActivity
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
class PlayerActivity : AppCompatActivity(), ViewModelFactoryHolder, ViewModelFactoryProvider, MenuCoordinatorHolder,
    SongViewModelProvider<SongInfoViewModel> {

    /**
     * Injection
     */

    private lateinit var activityComponent: PlayerComponent

    @Inject
    override lateinit var viewModelFactory: AnyFlowViewModelFactory

    @Inject
    lateinit var workerFactory: PlaylistWorkerFactory

    private val fakeComponent = object : PlayerComponent {
        override fun inject(playerActivity: PlayerActivity) {}
    }

    override val menuCoordinator = MenuCoordinator()

    /**
     * Private properties
     */
    lateinit var viewModel: PlayerViewModel
    private lateinit var binding: ActivityPlayerBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle

    private lateinit var orderMenu: OrderMenuHolder
    private var isSubtitleConfigured = false

    /**
     * Lifecycle methods
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val component = (application as AnyFlowApp).serverComponent
            ?.playerComponentBuilder()
            ?.build()
        activityComponent = if (component != null) {
            component
        } else {
            startActivity(ServerActivity::class)
            finish()
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
        initMenus()
        observeNetwork()

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.container,
                    be.florien.anyflow.feature.songlist.ui.SongListFragment(),
                    be.florien.anyflow.feature.songlist.ui.SongListFragment::class.java.simpleName
                )
                .runOnCommit {
                    adaptToolbarToCurrentFragment()
                    adaptBottomNavigationToCurrentFragment()
                }
                .commit()
        }
        viewModel.syncAlarms()
        viewModel.isOrdered.observe(this) {
            orderMenu.changeState(it)
        }
        viewModel.connectionStatus.observe(this) { status ->
            @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
            when (status) {
                AuthRepository.ConnectionStatus.WRONG_SERVER_URL,
                AuthRepository.ConnectionStatus.WRONG_ID_PAIR -> {
                    startActivity(ServerActivity::class)
                    finish()
                }

                AuthRepository.ConnectionStatus.CONNEXION -> return@observe
                AuthRepository.ConnectionStatus.CONNECTED -> {
                    bindService(
                        Intent(this, be.florien.anyflow.feature.sync.service.SyncService::class.java),
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
        updateMenuItemVisibility()
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
        if ((application as AnyFlowApp).serverComponent == null) {
            return
        }

        menuCoordinator.removeMenuHolder(orderMenu)
        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.cancelAll()
    }

    override fun getFactory() = viewModelFactory

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
            updateMenuItemVisibility()
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
                    startActivity(Intent(this@PlayerActivity, be.florien.anyflow.feature.alarm.ui.AlarmActivity::class.java))
                    true
                }

                R.id.menu_playlist -> {
                    startActivity(Intent(this, PlaylistsActivity::class.java))
                    true
                }

                R.id.menu_shortcut -> {
                    startActivity(Intent(this, ShortcutsActivity::class.java))
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
        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.menu_library -> {
                    displayLibrary()
                    true
                }

                R.id.menu_podcast -> {
                    displayPodcast()
                    true
                }

                R.id.menu_filters -> {
                    displayFilters()
                    true
                }

                else -> {
                    displaySongList()
                    true
                }
            }
        }
    }


    private fun initMenus() {
        orderMenu = OrderMenuHolder(
            viewModel.isOrdered.value == true,
            this
        ) {
            if (viewModel.isOrdered.value == true) {
                viewModel.randomOrder()
            } else {
                viewModel.classicOrder()
            }
        }

        menuCoordinator.addMenuHolder(orderMenu)
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

    private fun displayLibrary() {
        val fragment =
            supportFragmentManager.findFragmentByTag(LibraryTagsInfoFragment::class.java.simpleName)
                ?: LibraryTagsInfoFragment()
        supportFragmentManager
            .beginTransaction().apply {
                replace(R.id.container, fragment, LibraryTagsInfoFragment::class.java.simpleName)
                addToBackStack(LIBRARY_STACK_NAME)
            }
            .commit()
    }

    private fun displayPodcast() {
        val fragment =
            supportFragmentManager.findFragmentByTag(LibraryPodcastInfoFragment::class.java.simpleName)
                ?: LibraryPodcastInfoFragment()
        supportFragmentManager
            .beginTransaction().apply {
                replace(R.id.container, fragment, LibraryPodcastInfoFragment::class.java.simpleName)
                addToBackStack(LIBRARY_STACK_NAME)
            }
            .commit()
    }

    private fun displayFilters() {
        val fragment =
            supportFragmentManager.findFragmentByTag(be.florien.anyflow.feature.filter.current.ui.CurrentFilterFragment::class.java.simpleName)
                ?: be.florien.anyflow.feature.filter.current.ui.CurrentFilterFragment()
        supportFragmentManager
            .beginTransaction().apply {
                replace(R.id.container, fragment, be.florien.anyflow.feature.filter.current.ui.CurrentFilterFragment::class.java.simpleName)
                addToBackStack(LIBRARY_STACK_NAME)
            }
            .commit()
    }

    private fun updateMenuItemVisibility() {
        orderMenu.isVisible = isSongListVisible()
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
        when (val fragment = supportFragmentManager.findFragmentById(R.id.container)) {
            is be.florien.anyflow.feature.filter.current.ui.CurrentFilterFragment -> binding.bottomNavigationView
                .menu
                .findItem(R.id.menu_filters)
                .isChecked = true

            is BaseFilteringFragment -> {
                if (fragment.tag?.contains("Tags", ignoreCase = true) == true) {
                    binding.bottomNavigationView
                        .menu
                        .findItem(R.id.menu_library)
                        .isChecked = true
                } else {
                    binding.bottomNavigationView
                        .menu
                        .findItem(R.id.menu_podcast)
                        .isChecked = true
                }
            }

            else -> binding.bottomNavigationView.menu.findItem(R.id.menu_song_list).isChecked = true
        }
    }

    private fun isSongListVisible() =
        supportFragmentManager.findFragmentById(R.id.container) is be.florien.anyflow.feature.songlist.ui.SongListFragment

    companion object {
        private const val LIBRARY_STACK_NAME = "filters"
    }

    override fun getSongViewModel(owner: ViewModelStoreOwner?) =
        ViewModelProvider(owner ?: this, viewModelFactory)[SongInfoViewModel::class.java]
}