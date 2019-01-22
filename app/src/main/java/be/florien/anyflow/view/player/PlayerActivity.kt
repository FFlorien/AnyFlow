package be.florien.anyflow.view.player

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.databinding.Observable
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentManager
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import be.florien.anyflow.BR
import be.florien.anyflow.R
import be.florien.anyflow.databinding.ActivityPlayerBinding
import be.florien.anyflow.di.ActivityScope
import be.florien.anyflow.di.UserScope
import be.florien.anyflow.extension.anyFlowApp
import be.florien.anyflow.extension.startActivity
import be.florien.anyflow.player.PlayerController
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
    @Inject
    lateinit var vm: PlayerActivityVM
    private lateinit var binding: ActivityPlayerBinding
    private lateinit var snackbar: Snackbar

    private val menuCoordinator = MenuCoordinator()
    private lateinit var filterMenu: FilterMenuHolder
    private lateinit var orderMenu: OrderMenuHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_player)
        super.onCreate(savedInstanceState)
        snackbar = Snackbar.make(binding.container, "", Snackbar.LENGTH_INDEFINITE)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setIcon(R.drawable.ic_app)
        supportFragmentManager.addOnBackStackChangedListener {
            updateMenuItemVisibility()
            (supportFragmentManager.findFragmentById(R.id.container) as? BaseFragment)?.getTitle()?.let {
                title = it
            }
        }

        activityComponent.inject(this)
        if (activityComponent == fakeComponent) {
            return
        }
        binding.vm = vm

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
                    .commit()
        }

        vm.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                when (propertyId) {
                    BR.isOrdered -> {
                        orderMenu.changeState(vm.isOrdered)
                    }
                    BR.playerState -> {
                        when (vm.playerState) {
                            PlayerController.State.RECONNECT -> snackbar.setText(R.string.display_reconnecting).show()
                            else -> snackbar.dismiss()
                        }
                    }
                }
            }
        })

    }

    override fun onResume() {
        super.onResume()
        bindService(Intent(this, PlayerService::class.java), vm.connection, Context.BIND_AUTO_CREATE)
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

    override fun onPause() {
        super.onPause()
        unbindService(vm.connection)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (anyFlowApp.userComponent == null) {
            return
        }
        vm.destroy()
    }

    fun displaySongList() {
        supportFragmentManager.popBackStack("filters", FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    private fun displayFilters() {
        val fragment = supportFragmentManager.findFragmentByTag(DisplayFilterFragment::class.java.simpleName)
                ?: DisplayFilterFragment()
        supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_top, R.anim.slide_backward, R.anim.slide_forward, R.anim.slide_out_top)
                .replace(R.id.container, fragment, DisplayFilterFragment::class.java.simpleName)
                .addToBackStack("filters")
                .commit()
    }

    private fun updateMenuItemVisibility() {
        val isSongListVisible = isSongListVisible()
        filterMenu.isVisible = isSongListVisible
        orderMenu.isVisible = isSongListVisible
    }

    private fun isSongListVisible() =
            supportFragmentManager.findFragmentById(R.id.container) is SongListFragment
}