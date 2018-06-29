package be.florien.ampacheplayer.view.player

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import be.florien.ampacheplayer.R
import be.florien.ampacheplayer.databinding.ActivityPlayerBinding
import be.florien.ampacheplayer.di.ActivityScope
import be.florien.ampacheplayer.di.UserScope
import be.florien.ampacheplayer.extension.ampacheApp
import be.florien.ampacheplayer.extension.startActivity
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
    lateinit var binding: ActivityPlayerBinding

    private var isFilterDisplayed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_player)

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
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_player, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.filters) {
            if (!isFilterDisplayed) {
                displayFilters()
            } else {
                displaySongList()
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