package io.rownd.android.models.repos

import android.content.Context
import android.util.Log
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import io.rownd.android.models.Action
import io.rownd.android.models.State
import io.rownd.android.models.Store
import io.rownd.android.models.domain.AppConfigState
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.domain.SignInState
import io.rownd.android.models.domain.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore by dataStore("rownd_state.json", GlobalStateSerializer)

@Serializable
data class GlobalState(
    val appConfig: AppConfigState = AppConfigState(),
    val auth: AuthState = AuthState(),
    val user: User = User(),
    val signIn: SignInState = SignInState(),

    @Transient
    val isInitialized: Boolean = false,
) : State

object GlobalStateSerializer : Serializer<GlobalState> {

    override val defaultValue = GlobalState()
    val json = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun readFrom(input: InputStream): GlobalState {
        try {
            return json.decodeFromString(
                GlobalState.serializer(), input.readBytes().decodeToString()
            )
        } catch (serialization: SerializationException) {
            throw CorruptionException("Unable to read GlobalState", serialization)
        }
    }

    override suspend fun writeTo(t: GlobalState, output: OutputStream) {
        withContext(Dispatchers.IO) {
            output.write(
                json.encodeToString(GlobalState.serializer(), t)
                    .encodeToByteArray()
            )
        }
    }
}

sealed class StateAction : Action {
    data class SetGlobalState(val value: GlobalState) : StateAction()
    data class SetAuth(val value: AuthState) : StateAction()
    data class SetSignIn(val value: SignInState): StateAction()
    data class SetAppConfig(val value: AppConfigState) : StateAction()
    data class SetUser(val value: User) : StateAction()
}

@Singleton
class StateRepo @Inject constructor() {

    @Inject
    lateinit var appConfigRepo: AppConfigRepo

    // Manually inject these
    lateinit var userRepo: UserRepo
    lateinit var authRepo: AuthRepo

    private lateinit var dataStore: DataStore<GlobalState>

    private val store = Store<GlobalState, StateAction>(GlobalState()) { state, action ->
        when (action) {
            is StateAction.SetAuth -> state.copy(auth = action.value)
            is StateAction.SetSignIn -> state.copy(signIn = action.value)
            is StateAction.SetAppConfig -> state.copy(appConfig = action.value)
            is StateAction.SetUser -> state.copy(user = action.value)
            is StateAction.SetGlobalState -> action.value
        }
    }

    fun setup(ds: DataStore<GlobalState>): Store<GlobalState, StateAction> {
        dataStore = ds

        // Re-inflate store from persistence
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var persistedState = dataStore.data.first()
                persistedState = persistedState.copy(isInitialized = true)
                store.dispatch(StateAction.SetGlobalState(persistedState))
            } catch (err: Exception) {
                Log.d("Rownd.StateRepo", "Failed to load existing state from device", err)
                store.dispatch(StateAction.SetGlobalState(GlobalState(isInitialized = true)))
            }

            // Fetch latest app config
            appConfigRepo.loadAppConfigAsync(this@StateRepo).await()

            // Refresh token if needed
            if (store.currentState.auth.isAuthenticated && !store.currentState.auth.isAccessTokenValid) {
                authRepo.getAccessToken()
            }

            // Fetch latest user data if we're authenticated
            if (store.currentState.auth.isAccessTokenValid) {
                userRepo.loadUserAsync().await()
            }

            // Persist all state updates to cache
            store.stateAsStateFlow().collect {
                val updatedState = it
                dataStore.updateData {
                    updatedState
                }
            }
        }

        return store
    }

    val state = store.stateAsStateFlow()

    fun getStore(): Store<GlobalState, StateAction> {
        return store
    }
}
