package io.rownd.android.models

import io.rownd.android.models.repos.GlobalState
import io.rownd.android.models.repos.StateAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface State

interface Action

typealias Reducer<State, Action> = (State, Action) -> State

class Store<S, A>(
    initialState: S,
    private val reducer: Reducer<S, A>,
) where S : State, A : Action {
    private val state = MutableStateFlow(initialState)

    val currentState get() = state.value

    fun dispatch(action: A) {
        state.update { reducer(it, action) }
    }

    fun stateAsStateFlow(): StateFlow<S> {
        return state.asStateFlow()
    }
}