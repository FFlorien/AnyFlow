package be.florien.anyflow.feature.auth.ui.server

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.florien.anyflow.common.resources.R
import be.florien.anyflow.common.resources.component.LoadingButton
import be.florien.anyflow.common.resources.component.TopSnackbarHost
import be.florien.anyflow.common.resources.theming.AppTheme
import be.florien.anyflow.common.resources.utils.adaptiveIconPainterResource
import be.florien.anyflow.common.utils.startActivity
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

        setContent { Content() }
    }

    @Composable
    private fun Content() {
        val serverState = viewModel.urlStatus.collectAsStateWithLifecycle(ServerState.Idle).value
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(serverState) {
            val result: SnackbarResult? = when (serverState) {
                is ServerState.Error -> snackbarHostState
                    .showSnackbar(getString(serverState.errorMsg))

                is ServerState.Success -> {
                    val serverComponentCreator = application as ServerUrlSetter
                    serverComponentCreator.setServerUrl(serverState.url)
                    startActivity(AuthenticationActivity::class)
                    null
                }

                ServerState.Idle,
                ServerState.Loading -> null
            }

            if (result == SnackbarResult.Dismissed) {
                viewModel.messageRead()
            }
        }
        AppTheme(content = {
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
                            showKeyboardOnFocus = true,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        onKeyboardAction = { viewModel.connect(textFieldState.text.toString()) }
                    )
                    LoadingButton(
                        modifier = Modifier.fillMaxWidth(),
                        isLoading = serverState == ServerState.Loading || serverState is ServerState.Success,
                        onAction =  { viewModel.connect(textFieldState.text.toString()) }
                    ) {
                        Text(getString(R.string.confirm))
                    }
                }
            }
        })
    }
}