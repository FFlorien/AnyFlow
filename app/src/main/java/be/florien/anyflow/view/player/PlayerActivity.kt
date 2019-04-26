package be.florien.anyflow.view.player

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
import androidx.databinding.Observable
import androidx.fragment.app.FragmentManager
import be.florien.anyflow.BR
import be.florien.anyflow.R
import be.florien.anyflow.databinding.ActivityPlayerBinding
import be.florien.anyflow.di.ActivityScope
import be.florien.anyflow.di.UserScope
import be.florien.anyflow.extension.anyFlowApp
import be.florien.anyflow.extension.startActivity
import be.florien.anyflow.persistence.PingService
import be.florien.anyflow.persistence.UpdateService
import be.florien.anyflow.persistence.server.AmpacheConnection
import be.florien.anyflow.player.PlayerService
import be.florien.anyflow.view.BaseFragment
import be.florien.anyflow.view.connect.ConnectActivity
import be.florien.anyflow.view.menu.FilterMenuHolder
import be.florien.anyflow.view.menu.MenuCoordinator
import be.florien.anyflow.view.menu.OrderMenuHolder
import be.florien.anyflow.view.player.filter.display.DisplayFilterFragment
import be.florien.anyflow.view.player.filter.display.DisplayFilterFragmentVM
import be.florien.anyflow.view.player.filter.selectType.AddFilterTypeFragmentVM
import be.florien.anyflow.view.player.filter.selectType.SelectFilterTypeFragment
import be.florien.anyflow.view.player.filter.selection.SelectFilterFragmentAlbumVM
import be.florien.anyflow.view.player.filter.selection.SelectFilterFragmentArtistVM
import be.florien.anyflow.view.player.filter.selection.SelectFilterFragmentGenreVM
import be.florien.anyflow.view.player.songlist.SongListFragment
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
    @Inject
    lateinit var vm: PlayerActivityVM

    /**
     * Public properties
     */

    val activityComponent: PlayerComponent by lazy {
        val component = anyFlowApp.userComponent
                ?.playerComponentBuilder()
                ?.activity(this)
                ?.view(binding.root)
                ?.build()
        if (component != null) {
            component
        } else {
            startActivity(ConnectActivity::class)
            finish()
            fakeComponent
        }
    }

    /**
     * Private properties
     */

    private val fakeComponent = object : PlayerComponent {
        override fun inject(playerActivity: PlayerActivity) {}
        override fun inject(songListFragment: SongListFragment) {}
        override fun inject(displayFilterFragment: DisplayFilterFragment) {}
        override fun inject(addFilterFragmentGenreVM: SelectFilterFragmentGenreVM) {}
        override fun inject(addFilterFragmentArtistVM: SelectFilterFragmentArtistVM) {}
        override fun inject(addFilterFragmentAlbumVM: SelectFilterFragmentAlbumVM) {}
        override fun inject(selectFilterTypeFragment: SelectFilterTypeFragment) {}
        override fun inject(filterFragmentVM: DisplayFilterFragmentVM) {}
        override fun inject(addFilterTypeFragmentVM: AddFilterTypeFragmentVM) {}
    }
    private lateinit var binding: ActivityPlayerBinding

    private val menuCoordinator = MenuCoordinator()
    private lateinit var filterMenu: FilterMenuHolder
    private lateinit var orderMenu: OrderMenuHolder

    /**
     * Lifecycle methods
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_player)
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbar)
        supportFragmentManager.addOnBackStackChangedListener {
            updateMenuItemVisibility()
            changeToolbarTitle()
        }

        activityComponent.inject(this)
        if (activityComponent == fakeComponent) {
            return
        }

        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        bindService(Intent(this, UpdateService::class.java), vm.updateConnection, Context.BIND_AUTO_CREATE)
        val pingJobInfo = JobInfo.Builder(6, ComponentName(this, PingService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(HALF_HOUR)
                .build()
        jobScheduler.schedule(pingJobInfo)

        binding.vm = vm
        bindService(Intent(this, PlayerService::class.java), vm.playerConnection, Context.BIND_AUTO_CREATE)

        filterMenu = FilterMenuHolder {
            displayFilters()
        }
        orderMenu = OrderMenuHolder(vm.isOrdered, this) {
            if (vm.isOrdered) {
                vm.randomOrder()
            } else {
                vm.classicOrder()
            }
        }

        menuCoordinator.addMenuHolder(filterMenu)
        menuCoordinator.addMenuHolder(orderMenu)

        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.container, SongListFragment(), SongListFragment::class.java.simpleName)
                    .runOnCommit {
                        changeToolbarTitle()
                    }
                    .commit()
        }

        vm.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                when (propertyId) {
                    BR.isOrdered -> {
                        orderMenu.changeState(vm.isOrdered)
                    }
                    BR.connectionStatus -> {
                        when (vm.connectionStatus) {
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
                    BR.songsUpdatePercentage -> {
                        if (vm.songsUpdatePercentage in 0..100) {
                            binding.updatingText.text = getString(R.string.update_songs, vm.songsUpdatePercentage)
                            animateAppearance(binding.updatingStateView)
                        } else {
                            animateDisappearance(binding.updatingStateView)
                        }
                    }
                    BR.albumsUpdatePercentage -> {
                        if (vm.albumsUpdatePercentage in 0..100) {
                            binding.updatingText.text = getString(R.string.update_albums, vm.albumsUpdatePercentage)
                            animateAppearance(binding.updatingStateView)
                        } else {
                            animateDisappearance(binding.updatingStateView)
                        }
                    }
                    BR.artistsUpdatePercentage -> {
                        if (vm.artistsUpdatePercentage in 0..100) {
                            binding.updatingText.text = getString(R.string.update_artists, vm.artistsUpdatePercentage)
                            animateAppearance(binding.updatingStateView)
                        } else {
                            animateDisappearance(binding.updatingStateView)
                        }
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        updateMenuItemVisibility()
    }

    private fun changeToolbarTitle() {
        (supportFragmentManager.findFragmentById(R.id.container) as? BaseFragment)?.getTitle()?.let {
            supportActionBar?.title = it
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
        if (anyFlowApp.userComponent == null) {
            return
        }

        menuCoordinator.removeMenuHolder(filterMenu)
        menuCoordinator.removeMenuHolder(orderMenu)
        unbindService(vm.playerConnection)
        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.cancelAll()
        vm.destroy()
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