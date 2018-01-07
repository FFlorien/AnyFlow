package be.florien.ampacheplayer.view.connect

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import be.florien.ampacheplayer.R
import be.florien.ampacheplayer.databinding.ActivityConnectBinding
import be.florien.ampacheplayer.extension.ampacheApp
import be.florien.ampacheplayer.view.ActivityComponent
import be.florien.ampacheplayer.view.ActivityModule
import javax.inject.Inject

/**
 * Simple activity for connection
 */
class ConnectActivity : AppCompatActivity() {
    @field:Inject lateinit var vm: ConnectActivityVM
    lateinit var binding: ActivityConnectBinding

    private lateinit var activityComponent: ActivityComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_connect)
        activityComponent = ampacheApp.applicationComponent.activityComponentBuilder()
                .activity(this)
                .activityModule(ActivityModule())
                .view(binding.root)
                .build()
        activityComponent.inject(this)
        binding.vm = vm
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.unbind()
        vm.destroy()
    }
}