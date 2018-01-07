package be.florien.ampacheplayer.view.player

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import be.florien.ampacheplayer.R
import be.florien.ampacheplayer.databinding.ActivityPlayerBinding
import be.florien.ampacheplayer.extension.ampacheApp
import be.florien.ampacheplayer.player.PlayerService
import be.florien.ampacheplayer.view.ActivityComponent
import be.florien.ampacheplayer.view.ActivityModule
import be.florien.ampacheplayer.view.player.songlist.SongListFragment
import javax.inject.Inject

/**
 * Activity controlling the queue, play/pause/next/previous on the PlayerService
 */
class PlayerActivity : AppCompatActivity() {

    lateinit var activityComponent: ActivityComponent
    @Inject lateinit var vm: PlayerActivityVM
    lateinit var binding: ActivityPlayerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_player)
        activityComponent = ampacheApp
                .applicationComponent
                .activityComponentBuilder()
                .activity(this)
                .activityModule(ActivityModule())
                .view(binding.root)
                .build()
        activityComponent.inject(this)
        binding.vm = vm
        bindService(Intent(this, PlayerService::class.java), vm.connection, Context.BIND_AUTO_CREATE)

        if (savedInstanceState == null) {
            val fragment = supportFragmentManager.findFragmentByTag(SongListFragment::class.java.simpleName) ?: SongListFragment()
            supportFragmentManager.beginTransaction().replace(R.id.container, fragment, SongListFragment::class.java.simpleName).commit()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(vm.connection)
        vm.destroy()
    }
}