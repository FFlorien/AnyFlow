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
import be.florien.ampacheplayer.extension.ampacheApp
import be.florien.ampacheplayer.player.PlayerService
import be.florien.ampacheplayer.view.ActivityComponent
import be.florien.ampacheplayer.view.ActivityModule
import be.florien.ampacheplayer.view.player.filter.FilterFragment
import be.florien.ampacheplayer.view.player.songlist.SongListFragment
import javax.inject.Inject

/**
 * Activity controlling the queue, play/pause/next/previous on the PlayerService
 */
class PlayerActivity : AppCompatActivity() {

    lateinit var activityComponent: ActivityComponent
    @Inject lateinit var vm: PlayerActivityVM
    lateinit var binding: ActivityPlayerBinding

    var isFilterDisplayed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_player)
        activityComponent = ampacheApp
                .applicationComponent
                .activityComponentBuilder()
                .activity(this)
                .activityModule(ActivityModule())
                .view(binding.root)
                .build() // todo yes, but when the activity is recreated ???
        activityComponent.inject(this)
        binding.vm = vm
        bindService(Intent(this, PlayerService::class.java), vm.connection, Context.BIND_AUTO_CREATE)

        if (savedInstanceState == null) {
            displaySongList()
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
            }else{
                displaySongList()
            }

        }
        return super.onOptionsItemSelected(item)
    }

    private fun displaySongList() {
        val fragment = supportFragmentManager.findFragmentByTag(SongListFragment::class.java.simpleName) ?: SongListFragment()
        supportFragmentManager.beginTransaction().replace(R.id.container, fragment, SongListFragment::class.java.simpleName).commit()
        isFilterDisplayed = false
    }

    private fun displayFilters() {
        val fragment = supportFragmentManager.findFragmentByTag(FilterFragment::class.java.simpleName) ?: FilterFragment()
        supportFragmentManager.beginTransaction().replace(R.id.container, fragment, FilterFragment::class.java.simpleName).commit()
        isFilterDisplayed = true
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(vm.connection)
        vm.destroy()
    }
}