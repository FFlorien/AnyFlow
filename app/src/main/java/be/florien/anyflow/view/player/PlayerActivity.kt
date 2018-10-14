package be.florien.anyflow.view.player

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.databinding.Observable
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.graphics.drawable.Animatable2Compat
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
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
import be.florien.anyflow.view.connect.ConnectActivity
import be.florien.anyflow.view.player.filter.display.FilterFragment
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

    private var orderingMenu: MenuItem? = null
    private var filteringMenu: MenuItem? = null

    private var isFilterDisplayed = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_player)
        snackbar = Snackbar.make(binding.container, "", Snackbar.LENGTH_INDEFINITE)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setIcon(R.drawable.ic_app)

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

            if (savedInstanceState == null) {
                displaySongList()
            }

            vm.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                    when (propertyId) {
                        BR.isOrderRandom -> {
                            changeOrderMenu()
                        }
                        BR.isFiltered -> {
                            changeFilterMenu()
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
        menuInflater.inflate(R.menu.menu_player, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        orderingMenu = menu.findItem(R.id.order)
        filteringMenu = menu.findItem(R.id.filters)
        changeOrderMenu()
        changeFilterMenu()
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

    private fun changeOrderMenu() {
        orderingMenu?.run {
            setTitle(if (vm.isOrderRandom) {
                R.string.menu_order_classic
            } else {
                R.string.menu_order_random
            })
            icon = if (vm.isOrderRandom) {
                AnimatedVectorDrawableCompat.create(this@PlayerActivity, R.drawable.ic_order_to_random_anim)
            } else {
                AnimatedVectorDrawableCompat.create(this@PlayerActivity, R.drawable.ic_order_to_classic_anim)
            }
            (icon as? Animatable)?.start()
        }
    }

    private fun changeFilterMenu() {
        filteringMenu?.run {
            icon = if (vm.isFiltered) {
                AnimatedVectorDrawableCompat.create(this@PlayerActivity, R.drawable.ic_filter_to_filtered)?.apply {
                    registerAnimationCallback(object : Animatable2Compat.AnimationCallback(){
                        override fun onAnimationEnd(drawable: Drawable?) {
                            super.onAnimationEnd(drawable)
                        }
                    })
                }
            } else {
                AnimatedVectorDrawableCompat.create(this@PlayerActivity, R.drawable.ic_filter_to_unfiltered)
            }
            (icon as? Animatable)?.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (anyFlowApp.userComponent == null) {
            return
        }
        unbindService(vm.connection)
        vm.destroy()
    }
}