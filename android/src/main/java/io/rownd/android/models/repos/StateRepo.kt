package io.rownd.android.models.repos

import android.content.Context
import android.util.Log
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.FileStorage
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import io.opentelemetry.api.trace.StatusCode
import io.rownd.android.Rownd
import io.rownd.android.RowndSignInIntent
import io.rownd.android.RowndSignInJsOptions
import io.rownd.android.RowndSignInLoginStep
import io.rownd.android.RowndSignInOptions
import io.rownd.android.models.Action
import io.rownd.android.models.State
import io.rownd.android.models.Store
import io.rownd.android.models.domain.AppConfigState
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.domain.SignInState
import io.rownd.android.models.domain.User
import io.rownd.android.util.AppLifecycleListener
import io.rownd.android.util.AuthLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
        } catch (ex: SerializationException) {
            throw CorruptionException("Unable to read GlobalState", ex)
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
    data class SetUserIsLoading(val value: Boolean) : StateAction()
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
            is StateAction.SetUserIsLoading -> state.copy(user = state.user.copy(isLoading = action.value))
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

            // Check to see if we were handling an existing auth challenge
            // (maybe the app crashed or got OOM killed)
            store.currentState.auth.let {
                if (it.challengeId != null && it.userIdentifier != null) {
                    Rownd.requestSignIn(
                        RowndSignInJsOptions (
                            loginStep = RowndSignInLoginStep.Completing,
                            challengeId = it.challengeId,
                            userIdentifier = it.userIdentifier
                        )
                    );
                }
            }

            // Refresh token if needed
            if (store.currentState.auth.isAuthenticated && !store.currentState.auth.isAccessTokenValid) {
                try {
                    authRepo.getAccessToken()
                } catch (err: Exception) {
                    Log.d("Rownd.StateRepo", "Failed to fetch access token during startup", err)
                }
            }

            // Fetch latest user data if we're authenticated and app is in foreground
            if (AppLifecycleListener.isAppInForeground && store.currentState.auth.isAccessTokenValid) {
                userRepo.loadUserAsync().await()
            }

            tmpForceInstantUserConversionIfRequested(CoroutineScope(Dispatchers.IO))

            // Persist all state updates to cache when changes occur
            store.stateAsStateFlow().collect {
                val updatedState = it
                dataStore.updateData {
                    updatedState
                }
            }
        }

        return store
    }

    private fun tmpForceInstantUserConversionIfRequested(scope: CoroutineScope) {
        if (!Rownd.config.forceInstantUserConversion) {
            return
        }

        scope.launch {
            Rownd.state
                .map { it.auth.isAuthenticated to it.user }
                .distinctUntilChanged()
                .collect { (isAuthenticated, user) ->
                    if (
                        isAuthenticated &&
                        user.authLevel == AuthLevel.Instant
                    ) {
                        val signInOptions = RowndSignInOptions(
                            intent = RowndSignInIntent.SignUp,
                            title = "Add a sign-in method",
                            subtitle = "To ensure you can always access your account, please add a sign-in method."
                        )
                        Rownd.requestSignIn(signInOptions)
                    }
                }
        }
    }

    val state = store.stateAsStateFlow()

    fun getStore(): Store<GlobalState, StateAction> {
        return store
    }

    companion object {
        private var dataStore: DataStore<GlobalState>? = null
        fun defaultDataStore(context: Context): DataStore<GlobalState> {
            // DataStore must be a singleton
            dataStore?.let {
                return it
            }

            dataStore = DataStoreFactory.create(
                storage = FileStorage(GlobalStateSerializer) {
                    context.dataStoreFile(Rownd.config.stateFileName)
                },
                corruptionHandler = ReplaceFileCorruptionHandler { ex ->
                    // Handle cases where on-device state has become corrupt.
                    // First, log the exception and send a trace
                    Log.w("Rownd.StateRepo", "Failed to load existing state from device", ex)
                    val tracer = Rownd.rowndContext.telemetry?.getTracer()
                    val span = tracer?.spanBuilder("globalStateCorruption")?.startSpan()
                    span?.setStatus(StatusCode.ERROR)
                    span?.recordException(ex)
                    span?.end()

                    // Finally, reset the state to default (unfortunately, signing-out the user)
                    return@ReplaceFileCorruptionHandler GlobalState()
                }
            )
            return dataStore!!
        }
    }
}
