package be.florien.ampacheplayer.view.player

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.databinding.Observable
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import be.florien.ampacheplayer.BR
import be.florien.ampacheplayer.R
import be.florien.ampacheplayer.databinding.ActivityPlayerBinding
import be.florien.ampacheplayer.di.ActivityScope
import be.florien.ampacheplayer.di.UserScope
import be.florien.ampacheplayer.extension.ampacheApp
import be.florien.ampacheplayer.extension.startActivity
import be.florien.ampacheplayer.player.PlayerController
import be.florien.ampacheplayer.player.PlayerService
import be.florien.ampacheplayer.view.connect.ConnectActivity
import be.florien.ampacheplayer.view.player.filter.display.FilterFragment
import be.florien.ampacheplayer.view.player.songlist.SongListFragment
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

    private var orderingMenu: MenuItem? = null

    private var isFilterDisplayed = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_player)
        snackbar = Snackbar.make(binding.container, "", Snackbar.LENGTH_INDEFINITE)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setIcon(R.drawable.ic_app)

        val component = ampacheApp.userComponent
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

            if (savedInstanceState == null) {
                displaySongList()
            }

            vm.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                    when (propertyId) {
                        BR.isOrderRandom -> {
                            orderingMenu?.setTitle(if (vm.isOrderRandom) {
                                R.string.menu_order_classic
                            } else {
                                R.string.menu_order_random
                            })
                        }
                        BR.playerState -> {
                            when (vm.playerState) {
                                PlayerController.State.RECONNECT -> snackbar.setText("Reconnecting").show()
                                else -> {
                                    snackbar.dismiss()
                                    binding.playerControls.isBuffering = false
                                }
                            }
                            binding.playerControls.isBuffering = vm.playerState == PlayerController.State.BUFFER // todo  through databinding
                        }
                    }
                }
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_player, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        orderingMenu = menu.findItem(R.id.order)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.filters -> {
                if (!isFilterDisplayed) {
                    displayFilters()
                } else {
                    displaySongList()
                }
            }
            R.id.order -> {
                if (vm.isOrderRandom) {
                    vm.classicOrder()
                } else {
                    vm.randomOrder()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun displaySongList() {
        if (!supportFragmentManager.popBackStackImmediate()) {
            val fragment = supportFragmentManager.findFragmentByTag(SongListFragment::class.java.simpleName)
                    ?: SongListFragment()
            supportFragmentManager.beginTransaction().replace(R.id.container, fragment, SongListFragment::class.java.simpleName).commit()
        }
        isFilterDisplayed = false
    }

    private fun displayFilters() {
        val fragment = supportFragmentManager.findFragmentByTag(FilterFragment::class.java.simpleName)
                ?: FilterFragment()
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.container, fragment, FilterFragment::class.java.simpleName)
                .setTransition(R.anim.slide_in_top)
                .addToBackStack(null)
                .commit()
        isFilterDisplayed = true
    }

    override fun onDestroy() {
        super.onDestroy()
        if (ampacheApp.userComponent == null) {
            return
        }
        unbindService(vm.connection)
        vm.destroy()
    }
}