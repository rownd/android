package io.rownd.android.models.repos

import com.auth0.android.jwt.JWT
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.domain.SignInState
import io.rownd.android.models.network.Auth
import io.rownd.android.util.RowndContext
import kotlinx.coroutines.Deferred
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignInRepo @Inject constructor() {

    @Inject
    lateinit var stateRepo: StateRepo

    internal fun get(): SignInState {
        return stateRepo.getStore().currentState.signIn
    }

    internal fun setLastSignInMethod(method: String) {
        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        stateRepo.getStore().dispatch(StateAction.SetSignIn(SignInState(lastSignIn = method, lastSignInDate = currentDateTime.format(formatter))))
    }

    internal fun reset() {
        stateRepo.getStore().dispatch(StateAction.SetSignIn(SignInState()))
    }

}