package be.florien.anyflow.data.server.exception

/**
 * Indicate that the connection couldn't occur because the username password was incorrect
 */
class WrongIdentificationPairException(message: String?) : Exception(message)