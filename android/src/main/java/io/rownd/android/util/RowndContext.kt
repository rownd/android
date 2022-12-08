package io.rownd.android.util

import com.lyft.kronos.KronosClock
import io.rownd.android.models.RowndConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RowndContext @Inject constructor() {
    lateinit var config: RowndConfig
    var kronosClock: KronosClock? = null
}