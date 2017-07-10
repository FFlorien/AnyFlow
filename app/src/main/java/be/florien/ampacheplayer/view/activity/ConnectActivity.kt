package be.florien.ampacheplayer.view.activity

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.SupportActivity
import be.florien.ampacheplayer.R
import be.florien.ampacheplayer.databinding.ActivityConnectBinding
import be.florien.ampacheplayer.view.viewmodel.ConnectActivityVM

/**
 * Simple activity for connection
 */
class ConnectActivity : SupportActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityConnectBinding>(this, R.layout.activity_connect)
        ConnectActivityVM(this, binding)
    }
}