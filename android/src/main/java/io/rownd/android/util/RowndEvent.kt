package io.rownd.android.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
enum class RowndEventType {
    @SerialName("sign_in_started")
    SignInStarted,

    @SerialName("sign_in_completed")
    SignInCompleted,

    @SerialName("sign_in_failed")
    SignInFailed,

    @SerialName("user_updated")
    UserUpdated,

    @SerialName("sign_out")
    SignOut,

    @SerialName("user_data")
    UserData,

    @SerialName("user_data_saved")
    UserDataSaved,

    @SerialName("verification_started")
    VerificationStarted,

    @SerialName("verification_completed")
    VerificationCompleted,

    @SerialName("auth")
    Auth
}

@Serializable
data class RowndEvent (
    var event: RowndEventType,
    var data: Map<String, String?>
)

class RowndEventEmitter<T> @Inject constructor() {
    private val observers = mutableSetOf<(T) -> Unit>()

    fun addListener(observer: (T) -> Unit) {
        observers.add(observer)
    }

    fun removeListener(observer: (T) -> Unit) {
        observers.remove(observer)
    }

    internal fun emit(value: T) {
        for (observer in observers)
            observer(value)
    }
}