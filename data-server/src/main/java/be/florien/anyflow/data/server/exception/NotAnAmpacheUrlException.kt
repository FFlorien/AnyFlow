package be.florien.anyflow.data.server.exception

import java.lang.Exception

class NotAnAmpacheUrlException(override val message: String? = null, override val cause: Throwable? = null): Exception(message, cause)