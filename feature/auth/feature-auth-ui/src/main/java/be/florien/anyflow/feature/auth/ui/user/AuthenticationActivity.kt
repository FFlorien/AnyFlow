package be.florien.anyflow.feature.auth.ui.user

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.common.di.AnyFlowViewModelFactory
import be.florien.anyflow.feature.auth.ui.R
import be.florien.anyflow.feature.auth.ui.databinding.ActivityConnectBinding
import be.florien.anyflow.feature.auth.ui.di.AuthenticationActivityComponentCreator
import com.google.android.material.snackbar.Snackbar
import javax.inject.Inject

@SuppressLint("Registered")
open class AuthenticationActivity : AppCompatActivity() {

    @Inject
    lateinit var viewModelProvider: AnyFlowViewModelFactory

    lateinit var viewModel: UserConnectViewModel
    internal lateinit var binding: ActivityConnectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userConnectActivityComponent =
            (application as AuthenticationActivityComponentCreator).createUserConnectComponent()
                ?: throw IllegalStateException()
        userConnectActivityComponent.inject(this)

        viewModel = ViewModelProvider(this, viewModelProvider)[UserConnectViewModel::class.java]

        binding = DataBindingUtil.setContentView(this, R.layout.activity_connect)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view: View, windowInsets: WindowInsetsCompat ->
            val insets =
                windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.updatePadding(
                bottom = insets.bottom,
                left = insets.left,
                right = insets.right,
                top = insets.top
            )
            windowInsets
        }

        viewModel.isConnected.observe(this) {
            if (it) {
                viewModel.navigator.navigateToMain(this)
                finish()
            }
        }
        viewModel.errorMessage.observe(this) {
            if (it > 0) {
                Snackbar.make(binding.loadingProgress, it, Snackbar.LENGTH_LONG).show()
            }
        }
    }
}