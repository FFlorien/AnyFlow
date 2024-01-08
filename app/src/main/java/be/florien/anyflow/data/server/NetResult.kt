package be.florien.anyflow.data.server

import be.florien.anyflow.data.server.model.AmpacheError
import kotlin.Throwable

sealed interface NetResult<T>

class NetSuccess<T>(val data: T): NetResult<T>
class NetApiError<T>(val error: AmpacheError): NetResult<T>
class NetThrowable<T>(val throwable: Throwable): NetResult<T>