package be.florien.ampacheplayer.view

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import be.florien.ampacheplayer.R
import be.florien.ampacheplayer.databinding.ActivityPlayerBinding
import be.florien.ampacheplayer.view.viewmodel.PlayerActivityVM

/**
 * Activity controlling the queue, play/pause/next/previous on the PlayerService
 */
class PlayerActivity : BaseActivity() {

    lateinit var vm :PlayerActivityVM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityPlayerBinding>(this, R.layout.activity_player)
        vm = PlayerActivityVM(this, binding)
        vm.getSongs()
    }

    override fun onDestroy() {
        super.onDestroy()
        vm.destroy()
    }
}