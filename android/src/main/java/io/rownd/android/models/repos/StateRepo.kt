package io.rownd.android.models.repos

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import io.rownd.android.models.Action
import io.rownd.android.models.State
import io.rownd.android.models.Store
import io.rownd.android.models.domain.AppConfigState
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.domain.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

val Context.dataStore by dataStore("rownd_state.json", GlobalStateSerializer)

@Serializable
data class GlobalState(
    val appConfig: AppConfigState = AppConfigState(),
    val auth: AuthState = AuthState(),
    val user: User = User()
) : State

object GlobalStateSerializer : Serializer<GlobalState> {

    override val defaultValue = GlobalState()

    override suspend fun readFrom(input: InputStream): GlobalState {
        try {
            return Json.decodeFromString(
                GlobalState.serializer(), input.readBytes().decodeToString()
            )
        } catch (serialization: SerializationException) {
            throw CorruptionException("Unable to read GlobalState", serialization)
        }
    }

    override suspend fun writeTo(t: GlobalState, output: OutputStream) {
        output.write(
            Json.encodeToString(GlobalState.serializer(), t)
                .encodeToByteArray()
        )
    }
}

sealed class StateAction : Action {
    data class SetGlobalState(val value: GlobalState) : StateAction()
    data class SetAuth(val value: AuthState) : StateAction()
    data class SetAppConfig(val value: AppConfigState) : StateAction()
    data class SetUser(val value: User) : StateAction()
}

object StateRepo {
    private lateinit var dataStore: DataStore<GlobalState>

    private val store = Store<GlobalState, StateAction>(GlobalState()) { state, action ->
        when (action) {
            is StateAction.SetAuth -> state.copy(auth = action.value)
            is StateAction.SetAppConfig -> state.copy(appConfig = action.value)
            is StateAction.SetUser -> state.copy(user = action.value)
            is StateAction.SetGlobalState -> action.value
        }
    }

    fun setup(ds: DataStore<GlobalState>): Store<GlobalState, StateAction> {
        dataStore = ds

        // Re-inflate store from persistence
        CoroutineScope(Dispatchers.IO).async {
            val persistedState = dataStore.data.first()
            store.dispatch(StateAction.SetGlobalState(persistedState))

            // Fetch latest app config
            AppConfigRepo.loadAppConfigAsync().await()

            // Fetch latest user data if we're authenticated
            if (store.currentState.auth.isAuthenticated) {
                UserRepo.loadUserAsync().await()
            }

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