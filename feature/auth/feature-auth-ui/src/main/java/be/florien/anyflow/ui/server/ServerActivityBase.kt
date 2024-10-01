package be.florien.anyflow.ui.server

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.feature.auth.UserConnectActivity
import be.florien.anyflow.feature.auth.ui.R
import be.florien.anyflow.feature.auth.ui.databinding.ActivityServerBinding
import be.florien.anyflow.ui.di.ServerVmInjectorContainer
import be.florien.anyflow.utils.startActivity

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
        )[ServerViewModel::class.java]

        binding = DataBindingUtil.setContentView(this, R.layout.activity_server)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.isServerSetup.observe(this) {
            if (it) {
                startActivity(UserConnectActivity::class)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as ServerVmInjectorContainer).serverVmInjector.inject(viewModel)
    }
}