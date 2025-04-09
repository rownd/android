package io.rownd.android.util

import com.lyft.kronos.KronosClock
import io.rownd.android.RowndClient
import io.rownd.android.models.RowndConfig
import io.rownd.android.models.Store
import io.rownd.android.models.repos.AuthRepo
import io.rownd.android.models.repos.GlobalState
import io.rownd.android.models.repos.StateAction
import io.rownd.android.views.ComposableBottomSheet
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
    var hubView: WeakReference<ComposableBottomSheet>? = null
    var hubViewModel: RowndWebViewModel? = null
    var eventEmitter: RowndEventEmitter<RowndEvent>? = null
    var telemetry: Telemetry? = null

    fun isDisplayingHub(): Boolean {
        hubView?.get().let {
            return it?.activity != null
        }
    }
}