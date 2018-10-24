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
import be.florien.anyflow.view.player.songlist.SongListFragment
import javax.inject.Inject

/**
 * Activity controlling the queue, play/pause/next/previous on the PlayerService
 */
@ActivityScope
@UserScope
class PlayerActivity : AppCompatActivity() {

    lateinit var activityComponent: PlayerComponent
    @Inject
    lateinit var vm: PlayerActivityVM
    private lateinit var binding: ActivityPlayerBinding
    private lateinit var snackbar: Snackbar

    private val menuCoordinator = MenuCoordinator()
    private lateinit var filterMenu: FilterMenuHolder
    private lateinit var orderMenu: OrderMenuHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_player)
        snackbar = Snackbar.make(binding.container, "", Snackbar.LENGTH_INDEFINITE)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setIcon(R.drawable.ic_app)
        supportFragmentManager.addOnBackStackChangedListener {
            updateMenuItemVisibility()
            (supportFragmentManager.findFragmentById(R.id.container) as? BaseFragment)?.getTitle()?.let {
                title = it
            }
        }

        val component = anyFlowApp.userComponent
                ?.playerComponentBuilder()
                ?.activity(this)
                ?.view(binding.root)
                ?.build()

        if (component == null) {
            startActivity(ConnectActivity::class)
            finish()
        } else {
            activityComponent = component
            activityComponent.inject(this)
            binding.vm = vm
            bindService(Intent(this, PlayerService::class.java), vm.connection, Context.BIND_AUTO_CREATE)

            filterMenu = FilterMenuHolder(vm.isUnfiltered, this) {
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
                supportFragmentManager.beginTransaction()
                        .replace(R.id.container, SongListFragment(), SongListFragment::class.java.simpleName)
                        .commit()
            }

            vm.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                    when (propertyId) {
                        BR.isOrdered -> {
                            orderMenu.changeState(vm.isOrdered)
                        }
                        BR.isUnfiltered -> {
                            filterMenu.changeState(vm.isUnfiltered)
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
        unbindService(vm.connection)
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