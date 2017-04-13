package be.florien.ampacheplayer.view

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import be.florien.ampacheplayer.R
import be.florien.ampacheplayer.databinding.ActivityPlayerBinding
import be.florien.ampacheplayer.view.viewmodel.PlayerActivityVM

/**
 * Created by florien on 3/04/17.
 */
class PlayerActivity : AppCompatActivity() {

    lateinit var vm :PlayerActivityVM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityPlayerBinding>(this, R.layout.activity_player)
        vm = PlayerActivityVM(this, binding)
    }

    override fun onDestroy() {
        super.onDestroy()
        vm?.destroy()
    }
}