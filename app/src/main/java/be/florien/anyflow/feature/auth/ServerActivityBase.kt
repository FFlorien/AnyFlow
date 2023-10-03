package be.florien.anyflow.feature.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import be.florien.anyflow.R
import be.florien.anyflow.databinding.ActivityServerBinding
import be.florien.anyflow.extension.anyFlowApp
import be.florien.anyflow.extension.startActivity

/**
 * Simple activity for connection
 */
class ServerActivity : AppCompatActivity() {
    lateinit var viewModel: ServerViewModel
    internal lateinit var binding: ActivityServerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.NewInstanceFactory()
        ).get(ServerViewModel::class.java)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_server)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.isServerSetup.observe(this) {
            if (it) {
                startActivity(UserConnectActivity::class)
            }
        }
    }

    @UnstableApi
    override fun onResume() {
        super.onResume()
        anyFlowApp.applicationComponent.inject(viewModel)
    }
}