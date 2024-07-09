package be.florien.anyflow.data.server

import be.florien.anyflow.data.server.model.AmpacheApiListResponse
import be.florien.anyflow.data.server.model.AmpacheApiResponse
import be.florien.anyflow.data.server.model.AmpacheError
import kotlin.Throwable

sealed interface NetResult<T>

class NetSuccess<T>(val data: T): be.florien.anyflow.data.server.NetResult<T>
class NetApiError<T>(val error: AmpacheError): be.florien.anyflow.data.server.NetResult<T>
class NetThrowable<T>(val throwable: Throwable): be.florien.anyflow.data.server.NetResult<T>

fun <T: AmpacheApiResponse> T.toNetResult(): be.florien.anyflow.data.server.NetResult<T> =
    if (error == null) {
        be.florien.anyflow.data.server.NetSuccess(this)
    } else {
        be.florien.anyflow.data.server.NetApiError(error)
    }

fun <T> AmpacheApiListResponse<T>.toNetResult(): be.florien.anyflow.data.server.NetResult<List<T>> =
    if (error == null) {
        be.florien.anyflow.data.server.NetSuccess(this.list)
    } else {
        be.florien.anyflow.data.server.NetApiError(error)
    }