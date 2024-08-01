package io.rownd.android.util

import com.lyft.kronos.KronosClock
import io.rownd.android.RowndClient
import io.rownd.android.models.RowndConfig
import io.rownd.android.models.Store
import io.rownd.android.models.repos.AuthRepo
import io.rownd.android.models.repos.GlobalState
import io.rownd.android.models.repos.StateAction
import io.rownd.android.views.ComposableBottomSheetFragment
import io.rownd.android.views.RowndWebViewModel
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RowndContext @Inject constructor() {
    lateinit var config: RowndConfig
    var client: RowndClient? = null
    var kronosClock: KronosClock? = null
    var store: Store<GlobalState, StateAction>? = null

    var authRepo: AuthRepo? = null
    var hubView: WeakReference<ComposableBottomSheetFragment>? = null
    var hubViewModel: RowndWebViewModel? = null
    var eventEmitter: RowndEventEmitter<RowndEvent>? = null

    fun isDisplayingHub(): Boolean {
        return hubView != null && hubView?.get() != null
    }
}