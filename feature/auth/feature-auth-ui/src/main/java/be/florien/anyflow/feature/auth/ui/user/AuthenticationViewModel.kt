package be.florien.anyflow.feature.auth.ui.user

import androidx.lifecycle.viewModelScope
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.common.base.BaseViewModel
import be.florien.anyflow.data.server.exception.WrongIdentificationPairException
import be.florien.anyflow.common.logging.eLog
import be.florien.anyflow.data.server.model.AmpacheAuthentication
import be.florien.anyflow.feature.auth.domain.repository.AuthRepository
import be.florien.anyflow.feature.auth.ui.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthenticationState(
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val errorMessage: Int = -1
)

class AuthenticationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    val navigator: Navigator
) : BaseViewModel() {

    val state: Flow<AuthenticationState> = MutableStateFlow(AuthenticationState())

    fun connectWithUserPasswordOrApiToken(
        username: String,
        password: String,
        apiToken: String
    ) {
        val isNotBlank = password.isNotBlank() && username.isNotBlank()
        if (isNotBlank) {
            connectWithUserPassword(username, password)
        } else if (apiToken.isNotBlank()) {
            connectWithApiToken(apiToken)
        } else {
            state.mutable.update {
                it.copy(isLoading = false, errorMessage = R.string.connect_error_empty)
            }
        }
    }

    fun connectWithUserPassword(
        username: String,
        password: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val isNotBlank = password.isNotBlank() && username.isNotBlank()
            if (isNotBlank) {
                connect { authRepository.authenticate(username, password) }
            } else {
                state.mutable.update {
                    it.copy(isLoading = false, errorMessage = R.string.connect_error_empty)
                }
            }
        }
    }

    fun connectWithApiToken(
        apiToken: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (apiToken.isNotBlank()) {
                connect { authRepository.authenticate(apiToken) }
            } else {
                state.mutable.update {
                    it.copy(isLoading = false, errorMessage = R.string.connect_error_empty)
                }
            }
        }
    }

    private suspend fun connect(authentication: suspend () -> AmpacheAuthentication) {
        state.mutable.update { it.copy(isLoading = true) }
        try {
            val auth = authentication()
            when (auth.error.errorCode) {
                0 -> state.mutable.update { it.copy(isConnected = true) }
                else -> state.mutable.update {
                    it.copy(isLoading = false, errorMessage = R.string.connect_error_credentials)
                }
            }
        } catch (it: Exception) {
            this@AuthenticationViewModel.eLog(it)
            when (it) {
                is WrongIdentificationPairException -> {
                    state.mutable.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = R.string.connect_error_credentials
                        )
                    }
                }

                else -> {
                    state.mutable.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = R.string.connect_auth_error_unknown
                        )
                    }
                    this@AuthenticationViewModel.eLog(
                        it,
                        "Connection failed with unhandled exception"
                    )
                }
            }
        }
    }
}