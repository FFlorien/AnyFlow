package be.florien.anyflow.view.connect

import android.annotation.SuppressLint
import androidx.databinding.DataBindingUtil
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import be.florien.anyflow.R
import be.florien.anyflow.databinding.ActivityConnectBinding
import be.florien.anyflow.di.ActivityScope
import be.florien.anyflow.di.UserScope
import be.florien.anyflow.extension.anyFlowApp
import javax.inject.Inject

/**
 * Simple activity for connection
 */
@SuppressLint("Registered")
@ActivityScope
@UserScope
open class ConnectActivityBase : AppCompatActivity() {
    @field:Inject lateinit var vm: ConnectActivityVM
    internal lateinit var binding: ActivityConnectBinding

    private lateinit var connectComponent: ConnectComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_connect)
        connectComponent = anyFlowApp.applicationComponent.connectComponentBuilder()
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