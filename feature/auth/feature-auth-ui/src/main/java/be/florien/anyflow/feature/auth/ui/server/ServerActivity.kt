package be.florien.anyflow.feature.auth.ui.server

import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.common.resources.R
import be.florien.anyflow.common.resources.component.TopSnackbarHost
import be.florien.anyflow.common.resources.theming.AppTheme
import be.florien.anyflow.common.utils.startActivity
import be.florien.anyflow.feature.auth.domain.repository.ServerValidator
import be.florien.anyflow.feature.auth.ui.ServerUrlSetter
import be.florien.anyflow.feature.auth.ui.di.ServerViewModelInjector
import be.florien.anyflow.feature.auth.ui.user.AuthenticationActivity

class ServerActivity : AppCompatActivity() {
    lateinit var viewModel: ServerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[ServerViewModel::class.java]
        (application as ServerViewModelInjector).inject(viewModel)

        viewModel.urlStatus.observe(this) {
            setContent { Content(it) }
        }
    }

    @Composable
    private fun Content(serverStatus: ServerValidator.ServerStatus?) {
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(serverStatus) {
            val result: SnackbarResult? = when (serverStatus) {
                ServerValidator.ServerStatus.ErrorConnectivity -> snackbarHostState
                    .showSnackbar(getString(R.string.connect_error_connectivity))

                ServerValidator.ServerStatus.ErrorFormatEndSlash -> snackbarHostState
                    .showSnackbar(getString(R.string.connect_error_end_slash))

                ServerValidator.ServerStatus.ErrorFormatHttp -> snackbarHostState
                    .showSnackbar(getString(R.string.connect_error_http))

                ServerValidator.ServerStatus.ErrorFormatUnknown -> snackbarHostState
                    .showSnackbar(getString(R.string.connect_error_unknown))

                is ServerValidator.ServerStatus.Success -> {
                    val serverComponentCreator = application as ServerUrlSetter
                    serverComponentCreator.setServerUrl(serverStatus.url)
                    startActivity(AuthenticationActivity::class)
                    null
                }

                null -> null
            }

            if (result == SnackbarResult.Dismissed) {
                viewModel.messageRead()
            }
        }
        AppTheme(dynamicColor = false) {
            val textFieldState = remember { TextFieldState() }
            Scaffold(
                snackbarHost = { TopSnackbarHost(snackbarHostState) }
            ) { contentPadding ->
                Column(
                    Modifier
                        .padding(contentPadding)
                        .padding(horizontal = 16.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        modifier = Modifier
                            .padding(75.dp)
                            .size(150.dp),
                        painter = adaptiveIconPainterResource(R.mipmap.ic_launcher_round),
                        contentDescription = stringResource(R.string.connect_icon_content_description)
                    )
                    OutlinedTextField(
                        state = textFieldState,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(getString(R.string.connect_server_hint)) },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        onKeyboardAction = { viewModel.connect(textFieldState.text.toString()) }
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.connect(textFieldState.text.toString()) }
                    ) {
                        Text(getString(R.string.confirm))
                    }
                }
            }
        }
    }

    @Composable
    private fun adaptiveIconPainterResource(@DrawableRes id: Int): Painter {
        val res = LocalResources.current
        val theme = LocalContext.current.theme

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android O supports adaptive icons, try loading this first (even though this is least likely to be the format).
            val adaptiveIcon = ResourcesCompat.getDrawable(res, id, theme) as? AdaptiveIconDrawable
            if (adaptiveIcon != null) {
                BitmapPainter(adaptiveIcon.toBitmap().asImageBitmap())
            } else {
                // We couldn't load the drawable as an Adaptive Icon, just use painterResource
                painterResource(id)
            }
        } else {
            // We're not on Android O or later, just use painterResource
            painterResource(id)
        }
    }
}