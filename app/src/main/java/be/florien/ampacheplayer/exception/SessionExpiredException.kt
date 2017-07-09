package be.florien.ampacheplayer.exception

/**
 * Indicate that the request couldn't finish because the session has expired
 */
class SessionExpiredException(message: String?) : Exception(message)