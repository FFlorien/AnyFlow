package be.florien.anyflow.feature.auth.ui.server

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.common.base.BaseViewModel
import be.florien.anyflow.common.resources.R
import be.florien.anyflow.feature.auth.domain.persistence.AuthPersistence
import be.florien.anyflow.feature.auth.domain.repository.ServerValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the main activity
 */


sealed interface ServerState {
    data object Idle : ServerState
    data object Loading : ServerState
    data class Success(val url: String) : ServerState
    enum class Error(@param:StringRes val errorMsg: Int): ServerState {
        ErrorFormatEndSlash(R.string.connect_error_end_slash),
        ErrorFormatHttp(R.string.connect_error_http),
        ErrorFormatUnknown(R.string.connect_server_error_unknown),
        ErrorConnectivity(R.string.connect_error_connectivity)

    }
}

fun ServerValidator.ServerStatus.toServerState() = when (this) {
    ServerValidator.ServerStatus.ErrorConnectivity -> ServerState.Error.ErrorConnectivity
    ServerValidator.ServerStatus.ErrorFormatEndSlash -> ServerState.Error.ErrorFormatEndSlash
    ServerValidator.ServerStatus.ErrorFormatHttp -> ServerState.Error.ErrorFormatHttp
    ServerValidator.ServerStatus.ErrorFormatUnknown -> ServerState.Error.ErrorFormatUnknown
    is ServerValidator.ServerStatus.Success -> ServerState.Success(url)
}

class ServerViewModel : BaseViewModel() {
    @Inject
    lateinit var authPersistence: AuthPersistence

    @Inject
    lateinit var serverValidator: ServerValidator


    /**
     * Fields
     */
    val urlStatus: Flow<ServerState> = MutableStateFlow(ServerState.Idle)

    /**
     * Buttons calls
     */
    fun connect(serverUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            urlStatus.mutable.emit(ServerState.Loading)
            val serverValid = serverValidator.isServerValid(serverUrl)
            if (serverValid is ServerValidator.ServerStatus.Success) {
                authPersistence.saveServerInfo(serverValid.url)
            }
            urlStatus.mutable.emit(serverValid.toServerState())
        }
    }

    fun messageRead() {
        viewModelScope.launch {
            urlStatus.mutable.emit(ServerState.Idle)
        }
    }
}