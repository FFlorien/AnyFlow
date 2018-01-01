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

/**
 * Activity controlling the queue, play/pause/next/previous on the PlayerService
 */
class PlayerActivity : AppCompatActivity() {

    lateinit var activityComponent: ActivityComponent
    lateinit var vm: PlayerActivityVM
    lateinit var binding: ActivityPlayerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_player)
        vm = PlayerActivityVM()
        binding.vm = vm
        activityComponent = ampacheApp
                .applicationComponent
                .activityComponentBuilder()
                .activity(this)
                .view(binding.root)
                .build()
        activityComponent.inject(this)
        vm.onViewCreated()
        bindService(Intent(this, PlayerService::class.java), vm.connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(vm.connection)
        vm.destroy()
    }
}