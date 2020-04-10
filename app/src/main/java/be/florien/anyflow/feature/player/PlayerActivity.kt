package be.florien.anyflow.feature.player

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import be.florien.anyflow.R
import be.florien.anyflow.data.PingService
import be.florien.anyflow.data.UpdateService
import be.florien.anyflow.data.server.AmpacheConnection
import be.florien.anyflow.databinding.ActivityPlayerBinding
import be.florien.anyflow.extension.anyFlowApp
import be.florien.anyflow.extension.startActivity
import be.florien.anyflow.feature.BaseFragment
import be.florien.anyflow.feature.connect.ConnectActivity
import be.florien.anyflow.feature.menu.FilterMenuHolder
import be.florien.anyflow.feature.menu.MenuCoordinator
import be.florien.anyflow.feature.menu.OrderMenuHolder
import be.florien.anyflow.feature.observeValue
import be.florien.anyflow.feature.player.filter.display.DisplayFilterFragment
import be.florien.anyflow.feature.player.songlist.SongListFragment
import be.florien.anyflow.injection.ActivityScope
import be.florien.anyflow.injection.AnyFlowViewModelFactory
import be.florien.anyflow.injection.UserScope
import be.florien.anyflow.player.PlayerService
import javax.inject.Inject

/**
 * Activity controlling the queue, play/pause/next/previous on the PlayerService
 */
@ActivityScope
@UserScope
class PlayerActivity : AppCompatActivity() {

    /**
     * Injection
     */

    private lateinit var activityComponent: PlayerComponent

    @Inject
    lateinit var viewModelFactory: AnyFlowViewModelFactory

    private val fakeComponent = object : PlayerComponent {
        override fun inject(playerActivity: PlayerActivity) {}
    }

    /**
     * Private properties
     */
    lateinit var viewModel: PlayerViewModel
    private lateinit var binding: ActivityPlayerBinding

    private val menuCoordinator = MenuCoordinator()
    private lateinit var filterMenu: FilterMenuHolder
    private lateinit var orderMenu: OrderMenuHolder

    /**
     * Lifecycle methods
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        val component = anyFlowApp.userComponent
                ?.playerComponentBuilder()
                ?.build()
        activityComponent = if (component != null) {
            component
        } else {
            startActivity(ConnectActivity::class)
            finish()
            fakeComponent
        }
        super.onCreate(savedInstanceState)

        if (activityComponent == fakeComponent) {
            return
        }
        activityComponent.inject(this)
        viewModel = ViewModelProvider(this, viewModelFactory).get(PlayerViewModel::class.java)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_player)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        initToolbar()
        initMenus()
        initPingService()

        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.container, SongListFragment(), SongListFragment::class.java.simpleName)
                    .runOnCommit {
                        adaptToolbarToCurrentFragment()
                    }
                    .commit()
        }
        viewModel.isOrdered.observe(this) {
            orderMenu.changeState(it)
        }
        viewModel.connectionStatus.observeValue(this) {
            when (it) {
                AmpacheConnection.ConnectionStatus.WRONG_ID_PAIR -> {
                    startActivity(ConnectActivity::class)
                    finish()
                }
                AmpacheConnection.ConnectionStatus.CONNEXION -> animateAppearance(binding.connectionStateView)
                AmpacheConnection.ConnectionStatus.CONNECTED -> animateDisappearance(binding.connectionStateView)
                AmpacheConnection.ConnectionStatus.TIMEOUT -> AlertDialog
                        .Builder(this@PlayerActivity)
                        .setMessage(R.string.connect_error_timeout)
                        .setNeutralButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                        .create()
                        .show()
            }
        }
        viewModel.songsUpdatePercentage.observeValue(this) {
            if (it in 0..100) {
                binding.updatingText.text = getString(R.string.update_songs, it)
                animateAppearance(binding.updatingStateView)
            } else {
                animateDisappearance(binding.updatingStateView)
            }
        }
        viewModel.albumsUpdatePercentage.observeValue(this) {
            if (it in 0..100) {
                binding.updatingText.text = getString(R.string.update_albums, it)
                animateAppearance(binding.updatingStateView)
            } else {
                animateDisappearance(binding.updatingStateView)
            }
        }
        viewModel.artistsUpdatePercentage.observeValue(this) {
            if (it in 0..100) {
                binding.updatingText.text = getString(R.string.update_artists, it)
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
        if (anyFlowApp.userComponent == null) {
            return
        }

        menuCoordinator.removeMenuHolder(filterMenu)
        menuCoordinator.removeMenuHolder(orderMenu)
        unbindService(viewModel.playerConnection)
        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.cancelAll()
    }

    /**
     * Public method
     */

    fun displaySongList() {
        supportFragmentManager.popBackStack(FILTER_STACK_NAME, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    /**
     * Private methods
     */

    private fun initToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_cancel)
        supportFragmentManager.addOnBackStackChangedListener {
            updateMenuItemVisibility()
            adaptToolbarToCurrentFragment()
        }
    }

    private fun initMenus() {
        filterMenu = FilterMenuHolder {
            displayFilters()
        }
        orderMenu = OrderMenuHolder(viewModel.isOrdered.value == true, this) {
            if (viewModel.isOrdered.value == true) {
                viewModel.randomOrder()
            } else {
                viewModel.classicOrder()
            }
        }

        menuCoordinator.addMenuHolder(filterMenu)
        menuCoordinator.addMenuHolder(orderMenu)
    }

    private fun initPingService() {
        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        bindService(Intent(this, UpdateService::class.java), viewModel.updateConnection, Context.BIND_AUTO_CREATE)
        val pingJobInfo = JobInfo.Builder(6, ComponentName(this, PingService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(HALF_HOUR)
                .build()
        jobScheduler.schedule(pingJobInfo)

        bindService(Intent(this, PlayerService::class.java), viewModel.playerConnection, Context.BIND_AUTO_CREATE)
    }

    private fun displayFilters() {
        val fragment = supportFragmentManager.findFragmentByTag(DisplayFilterFragment::class.java.simpleName)
                ?: DisplayFilterFragment()
        supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_top, R.anim.slide_backward, R.anim.slide_forward, R.anim.slide_out_top)
                .replace(R.id.container, fragment, DisplayFilterFragment::class.java.simpleName)
                .addToBackStack(FILTER_STACK_NAME)
                .commit()
    }

    private fun updateMenuItemVisibility() {
        val isSongListVisible = isSongListVisible()
        filterMenu.isVisible = isSongListVisible
        orderMenu.isVisible = isSongListVisible
    }

    private fun adaptToolbarToCurrentFragment() {
        (supportFragmentManager.findFragmentById(R.id.container) as? BaseFragment)?.getTitle()?.let {
            supportActionBar?.title = it
        }
    }

    private fun isSongListVisible() =
            supportFragmentManager.findFragmentById(R.id.container) is SongListFragment

    private fun animateAppearance(view: View) {
        if (view.visibility != View.VISIBLE) {
            val maxHeight = resources.getDimensionPixelOffset(R.dimen.infoTextViewHeight)
            ValueAnimator.ofInt(0, maxHeight).apply {
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(p0: Animator?) {}

                    override fun onAnimationEnd(p0: Animator?) {}

                    override fun onAnimationCancel(p0: Animator?) {}

                    override fun onAnimationStart(p0: Animator?) {
                        view.visibility = View.VISIBLE
                    }

                })
                addUpdateListener { view.layoutParams.height = it.animatedValue as Int }
                duration = 150
            }.start()
        }
    }

    private fun animateDisappearance(view: View) {
        if (view.visibility != View.GONE) {
            val maxHeight = resources.getDimensionPixelOffset(R.dimen.infoTextViewHeight)
            ValueAnimator.ofInt(maxHeight, 0).apply {
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(p0: Animator?) {}

                    override fun onAnimationEnd(p0: Animator?) {
                        view.visibility = View.GONE
                    }

                    override fun onAnimationCancel(p0: Animator?) {}

                    override fun onAnimationStart(p0: Animator?) {}

                })
                addUpdateListener { view.layoutParams.height = it.animatedValue as Int }
                duration = 150
            }.start()
        }
    }

    companion object {
        private const val FILTER_STACK_NAME = "filters"
        private const val HALF_HOUR = 1800000L
    }
}