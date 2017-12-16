package be.florien.ampacheplayer.view.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import be.florien.ampacheplayer.view.viewmodel.PlayerActivityVM

/**
 * Activity controlling the queue, play/pause/next/previous on the PlayerService
 */
class PlayerActivity : AppCompatActivity() {

    lateinit var vm: PlayerActivityVM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = PlayerActivityVM(this)
        vm.onViewCreated()
    }

    override fun onDestroy() {
        super.onDestroy()
        vm.destroy()
    }
}