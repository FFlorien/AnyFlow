package be.florien.anyflow.feature.player.ui

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.job.JobScheduler
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.R
import be.florien.anyflow.databinding.ActivityPlayerBinding
import be.florien.anyflow.extension.anyFlowApp
import be.florien.anyflow.extension.startActivity
import be.florien.anyflow.feature.BaseFragment
import be.florien.anyflow.feature.alarms.AlarmActivity
import be.florien.anyflow.feature.auth.AuthRepository
import be.florien.anyflow.feature.auth.ServerActivity
import be.florien.anyflow.feature.menu.MenuCoordinator
import be.florien.anyflow.feature.menu.implementation.LibraryMenuHolder
import be.florien.anyflow.feature.menu.implementation.OrderMenuHolder
import be.florien.anyflow.feature.player.services.PlayerService
import be.florien.anyflow.feature.player.ui.info.song.quickActions.QuickActionsActivity
import be.florien.anyflow.feature.player.ui.library.info.LibraryInfoFragment
import be.florien.anyflow.feature.player.ui.songlist.SongListFragment
import be.florien.anyflow.feature.playlist.PlaylistsActivity
import be.florien.anyflow.feature.sync.SyncService
import be.florien.anyflow.injection.*
import javax.inject.Inject

/**
 * Activity controlling the queue, play/pause/next/previous on the PlayerService
 */
@ActivityScope
@ServerScope
class PlayerActivity : AppCompatActivity(), ViewModelFactoryHolder {

    /**
     * Injection
     */

    private lateinit var activityComponent: PlayerComponent

    @Inject
    lateinit var viewModelFactory: AnyFlowViewModelFactory

    private val fakeComponent = object : PlayerComponent {
        override fun inject(playerActivity: PlayerActivity) {}
    }

    val menuCoordinator = MenuCoordinator()

    /**
     * Private properties
     */
    lateinit var viewModel: PlayerViewModel
    private lateinit var binding: ActivityPlayerBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle

    private lateinit var libraryMenu: LibraryMenuHolder
    private lateinit var orderMenu: OrderMenuHolder
    private var isSubtitleConfigured = false

    /**
     * Lifecycle methods
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        val component = anyFlowApp.serverComponent
            ?.playerComponentBuilder()
            ?.build()
        activityComponent = if (component != null) {
            component
        } else {
            startActivity(ServerActivity::class)
            finish()
            fakeComponent
        }
        super.onCreate(savedInstanceState)

        if (activityComponent == fakeComponent) {
            return
        }
        activityComponent.inject(this)
        viewModel = ViewModelProvider(this, viewModelFactory)[PlayerViewModel::class.java]
        binding = DataBindingUtil.setContentView(this, R.layout.activity_player)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        initToolbar()
        initDrawer()
        initMenus()
        initPlayerService()

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.container,
                    SongListFragment(),
                    SongListFragment::class.java.simpleName
                )
                .runOnCommit {
                    adaptToolbarToCurrentFragment()
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

                AuthRepository.ConnectionStatus.CONNEXION -> animateAppearance(binding.connectionStateView)
                AuthRepository.ConnectionStatus.CONNECTED -> {
                    bindService(
                        Intent(this, SyncService::class.java),
                        viewModel.updateConnection,
                        Context.BIND_AUTO_CREATE
                    )
                    animateDisappearance(binding.connectionStateView)
                }
            }
        }
        viewModel.hasInternet.observe(this) { hasInternet ->
            if (hasInternet) {
                animateDisappearance(binding.internetStateView)
            } else {
                animateAppearance(binding.internetStateView)
            }
        }
        viewModel.songsUpdatePercentage.observe(this) {
            if (it in 0..100) {
                binding.updatingText.text = getString(R.string.update_songs, it)
                animateAppearance(binding.updatingStateView)
            } else {
                animateDisappearance(binding.updatingStateView)
            }
        }
        viewModel.genresUpdatePercentage.observe(this) {
            if (it in 0..100) {
                binding.updatingText.text = getString(R.string.update_genres, it)
                animateAppearance(binding.updatingStateView)
            } else {
                animateDisappearance(binding.updatingStateView)
            }
        }
        viewModel.albumsUpdatePercentage.observe(this) {
            if (it in 0..100) {
                binding.updatingText.text = getString(R.string.update_albums, it)
                animateAppearance(binding.updatingStateView)
            } else {
                animateDisappearance(binding.updatingStateView)
            }
        }
        viewModel.artistsUpdatePercentage.observe(this) {
            if (it in 0..100) {
                binding.updatingText.text = getString(R.string.update_artists, it)
                animateAppearance(binding.updatingStateView)
            } else {
                animateDisappearance(binding.updatingStateView)
            }
        }
        viewModel.playlistsUpdatePercentage.observe(this) {
            if (it in 0..100) {
                binding.updatingText.text = getString(R.string.update_playlists, it)
                animateAppearance(binding.updatingStateView)
            } else {
                animateDisappearance(binding.updatingStateView)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateMenuItemVisibility()
        adaptToolbarToCurrentFragment()
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
        if (anyFlowApp.serverComponent == null) {
            return
        }

        menuCoordinator.removeMenuHolder(libraryMenu)
        menuCoordinator.removeMenuHolder(orderMenu)
        unbindService(viewModel.playerConnection)
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
                    startActivity(Intent(this@PlayerActivity, AlarmActivity::class.java))
                    true
                }

                R.id.menu_playlist -> {
                    startActivity(Intent(this, PlaylistsActivity::class.java))
                    true
                }

                R.id.menu_quick_actions -> {
                    startActivity(Intent(this, QuickActionsActivity::class.java))
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

    private fun initMenus() {
        libraryMenu = LibraryMenuHolder {
            displayLibrary()
        }
        orderMenu = OrderMenuHolder(viewModel.isOrdered.value == true, this) {
            if (viewModel.isOrdered.value == true) {
                viewModel.randomOrder()
            } else {
                viewModel.classicOrder()
            }
        }

        menuCoordinator.addMenuHolder(libraryMenu)
        menuCoordinator.addMenuHolder(orderMenu)
    }

    private fun initPlayerService() {
        bindService(
            Intent(this, PlayerService::class.java),
            viewModel.playerConnection,
            BIND_AUTO_CREATE
        )
    }

    private fun displayLibrary() {
        val fragment =
            supportFragmentManager.findFragmentByTag(LibraryInfoFragment::class.java.simpleName)
                ?: LibraryInfoFragment()
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_top,
                R.anim.slide_backward,
                R.anim.slide_forward,
                R.anim.slide_out_top
            )
            .replace(R.id.container, fragment, LibraryInfoFragment::class.java.simpleName)
            .addToBackStack(LIBRARY_STACK_NAME)
            .commit()
    }

    private fun updateMenuItemVisibility() {
        val isSongListVisible = isSongListVisible()
        libraryMenu.isVisible = isSongListVisible
        orderMenu.isVisible = isSongListVisible
    }

    private fun adaptToolbarToCurrentFragment() {
        val isSongListVisible = isSongListVisible()
        val baseFragment = supportFragmentManager.findFragmentById(R.id.container) as? BaseFragment
        baseFragment?.getTitle()?.let {
            supportActionBar?.title = it
        }
        val subtitle = baseFragment?.getSubtitle()
        supportActionBar?.subtitle = subtitle
        drawerToggle.isDrawerIndicatorEnabled = isSongListVisible
        if (subtitle != null && !isSubtitleConfigured) {
            binding.toolbar.children.forEach { toolbarView ->
                (toolbarView as? TextView)
                    ?.takeIf { it.text == subtitle }
                    ?.ellipsize = TextUtils.TruncateAt.START
            }
        }
    }

    private fun isSongListVisible() =
        supportFragmentManager.findFragmentById(R.id.container) is SongListFragment

    private fun animateAppearance(view: View) {
        if (view.visibility != View.VISIBLE) {
            view.visibility = View.VISIBLE
        }
    }

    private fun animateDisappearance(view: View) {
        if (view.visibility != View.GONE) {
            view.visibility = View.GONE
        }
    }

    companion object {
        private const val LIBRARY_STACK_NAME = "filters"
        private const val HALF_HOUR = 1800000L
    }
}