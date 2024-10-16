package be.florien.anyflow.feature.auth.ui.server

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.feature.auth.ui.R
import be.florien.anyflow.feature.auth.ui.ServerUrlSetter
import be.florien.anyflow.feature.auth.ui.databinding.ActivityServerBinding
import be.florien.anyflow.feature.auth.ui.di.ServerViewModelInjector
import be.florien.anyflow.feature.auth.ui.user.AuthenticationActivity
import be.florien.anyflow.utils.startActivity

/**
 * Simple activity for connection
 */
class ServerActivity : AppCompatActivity() {
    lateinit var viewModel: ServerViewModel
    internal lateinit var binding: ActivityServerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[ServerViewModel::class.java]
        (application as ServerViewModelInjector).inject(viewModel)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_server)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.validatedServerUrl.observe(this) {
            if (it.isNotBlank()) {
                val serverComponentCreator = application as ServerUrlSetter
                serverComponentCreator.setServerUrl(it)
                startActivity(AuthenticationActivity::class)
            }
        }
    }
}