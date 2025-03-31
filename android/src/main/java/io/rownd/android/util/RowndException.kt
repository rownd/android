package io.rownd.android.util

open class RowndException(message: String) : Exception(message)

class NoAccessTokenPresentException(message: String) : RowndException(message)
class InvalidRefreshTokenException(message: String) : RowndException(message)
class NetworkConnectionFailureException(message: String) : RowndException(message)
class ServerException(message: String) : RowndException(message)