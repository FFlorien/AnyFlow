package be.florien.ampacheplayer.view

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import be.florien.ampacheplayer.R
import be.florien.ampacheplayer.databinding.ActivityConnectBinding
import be.florien.ampacheplayer.view.viewmodel.ConnectActivityVM

/**
 * Simple activity for connection
 */
class ConnectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityConnectBinding>(this, R.layout.activity_connect)
        ConnectActivityVM(this, binding)
    }
}