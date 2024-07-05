package be.florien.anyflow.data.server

import be.florien.anyflow.data.server.model.AmpacheApiListResponse
import be.florien.anyflow.data.server.model.AmpacheApiResponse
import be.florien.anyflow.data.server.model.AmpacheError
import kotlin.Throwable

sealed interface NetResult<T>

class NetSuccess<T>(val data: T): NetResult<T>
class NetApiError<T>(val error: AmpacheError): NetResult<T>
class NetThrowable<T>(val throwable: Throwable): NetResult<T>

fun <T: AmpacheApiResponse> T.toNetResult(): NetResult<T> =
    if (error == null) {
        NetSuccess(this)
    } else {
        NetApiError(error)
    }

fun <T> AmpacheApiListResponse<T>.toNetResult(): NetResult<List<T>> =
    if (error == null) {
        NetSuccess(this.list)
    } else {
        NetApiError(error)
    }