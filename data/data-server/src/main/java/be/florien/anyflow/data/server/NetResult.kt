package be.florien.anyflow.data.server

import be.florien.anyflow.common.logging.eLog
import be.florien.anyflow.data.server.model.AmpacheApiListResponse
import be.florien.anyflow.data.server.model.AmpacheError
import be.florien.anyflow.data.server.model.AmpacheErrorResponse

sealed interface NetResult<T>

class NetSuccess<T>(val data: T): NetResult<T>
class NetApiError<T>(val error: AmpacheError): NetResult<T>
class NetThrowable<T>(val throwable: Throwable): NetResult<T>

fun <T: AmpacheErrorResponse> T.toNetResult(): NetResult<T> {
    val ampacheError = error
    return if (ampacheError == null) {
        NetSuccess(this)
    } else {
        NetApiError(ampacheError)
    }
}

fun <T> AmpacheApiListResponse<T>.toNetResult(): NetResult<List<T>> {
    val ampacheError = error
    return if (ampacheError == null) {
        NetSuccess(list)
    } else {
        NetApiError(ampacheError)
    }
}

fun NetResult<*>.logError(method: String) {
    when (this) {
        is NetThrowable -> logThrowable(method, throwable)
        is NetApiError -> logApiError(error)
        else -> Unit
    }
}

private fun NetResult<*>.logThrowable(method: String, throwable: Throwable) {
    eLog("$method received Throwable $throwable")
}

private fun NetResult<*>.logApiError(error: AmpacheError) {
    eLog("Action ${error.errorAction} received API error ${error.errorCode}: ${error.errorMessage}")
}