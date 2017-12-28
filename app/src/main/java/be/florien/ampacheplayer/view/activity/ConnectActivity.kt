package be.florien.ampacheplayer.view.activity

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import be.florien.ampacheplayer.R
import be.florien.ampacheplayer.databinding.ActivityConnectBinding
import be.florien.ampacheplayer.extension.ampacheApp
import be.florien.ampacheplayer.view.viewmodel.ConnectActivityVM

/**
 * Simple activity for connection
 */
class ConnectActivity : AppCompatActivity() {
    lateinit var vm: ConnectActivityVM
    lateinit var binding: ActivityConnectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView<ActivityConnectBinding>(this, R.layout.activity_connect)
        vm = ConnectActivityVM()
        binding.vm = vm
        ampacheApp.applicationComponent.inject(vm)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.unbind()
        vm.destroy()
    }
}