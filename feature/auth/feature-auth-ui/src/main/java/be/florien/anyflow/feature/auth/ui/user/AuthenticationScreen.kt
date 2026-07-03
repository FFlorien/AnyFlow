package be.florien.anyflow.feature.auth.ui.user

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import be.florien.anyflow.common.resources.component.LoadingButton
import be.florien.anyflow.common.resources.component.TopSnackbarHost
import be.florien.anyflow.common.resources.utils.adaptiveIconPainterResource
import be.florien.anyflow.feature.auth.ui.R

@Composable
fun AuthenticationScreen(
    state: AuthenticationState,
    onConnect: (String, String, String) -> Unit,
    onConnectUserPwd: (String, String) -> Unit,
    onConnectToken: (String) -> Unit,
    onConnected: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(state) {
        if (state.isConnected) {
            onConnected()
        }
        if (state.errorMessage > 0) {
            snackbarHostState.showSnackbar(resources.getString(state.errorMessage))
        }
    }

    val userFieldState = remember { TextFieldState() }
    val passwordFieldState = remember { TextFieldState() }
    val apiKeyFieldState = remember { TextFieldState() }
    val isPasswordVisible = remember { mutableStateOf(false) }
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
                state = userFieldState,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentType = ContentType.Username },
                label = { Text(resources.getString(R.string.connect_username_hint)) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )
            )
            OutlinedSecureTextField(
                state = passwordFieldState,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(resources.getString(R.string.connect_password_hint)) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Go
                ),
                textObfuscationMode = if (isPasswordVisible.value) TextObfuscationMode.Visible else TextObfuscationMode.RevealLastTyped,
                trailingIcon = {
                    IconButton(onClick = {
                        isPasswordVisible.value = !isPasswordVisible.value
                    }) {
                        val resource = if (isPasswordVisible.value) {
                            R.drawable.ic_eye_closed
                        } else {
                            R.drawable.ic_eye_open
                        }
                        Icon(
                            painterResource(resource),
                            contentDescription = resources.getString(R.string.connect_pwd_eye_content_description)
                        )
                    }
                },
                onKeyboardAction = {
                    onConnectUserPwd(
                        userFieldState.text.toString(),
                        passwordFieldState.text.toString()
                    )
                }
            )
            HorizontalDivider()
            OutlinedTextField(
                state = apiKeyFieldState,
                modifier = Modifier
                    .fillMaxWidth(),
                label = { Text(resources.getString(R.string.connect_api_token_hint)) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Go
                ),
                onKeyboardAction = { onConnectToken(apiKeyFieldState.text.toString()) }
            )
            LoadingButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentType = ContentType.Password },
                isLoading = state.isLoading,
                onAction = {
                    onConnect(
                        userFieldState.text.toString(),
                        passwordFieldState.text.toString(),
                        apiKeyFieldState.text.toString()
                    )
                }
            ) {
                Text(resources.getString(be.florien.anyflow.common.resources.R.string.confirm))
            }
        }
    }
}