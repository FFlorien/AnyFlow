package be.florien.ampacheplayer.view.connect

import android.annotation.SuppressLint
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import be.florien.ampacheplayer.R
import be.florien.ampacheplayer.databinding.ActivityConnectBinding
import be.florien.ampacheplayer.di.ActivityScope
import be.florien.ampacheplayer.di.UserScope
import be.florien.ampacheplayer.extension.ampacheApp
import javax.inject.Inject

/**
 * Simple activity for connection
 */
@SuppressLint("Registered")
@ActivityScope
@UserScope
open class ConnectActivityBase : AppCompatActivity() {
    @field:Inject lateinit var vm: ConnectActivityVMBase
    internal lateinit var binding: ActivityConnectBinding

    private lateinit var connectComponent: ConnectComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_connect)
        connectComponent = ampacheApp.applicationComponent.connectComponentBuilder()
                .activity(this)
                .view(binding.root)
                .build()
        connectComponent.inject(this)
        binding.vm = vm
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.unbind()
        vm.destroy()
    }
}