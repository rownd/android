package io.rownd.rowndtestsandbox

import android.app.Application
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.rownd.android.Rownd
import io.rownd.android.models.RowndCustomizations


class AppCustomizations(app: Application) : RowndCustomizations() {
    private var app: Application

    init {
        this.app = app
    }

    override val dynamicSheetBackgroundColor: Color
    get() {
            val uiMode = app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return if (uiMode == Configuration.UI_MODE_NIGHT_YES) {
                Color(0xff123456)
            } else {
                Color(0xfffedcba)

            }
        }

    override var sheetCornerBorderRadius: Dp = 25.dp
    override var loadingAnimation: Int? = R.raw.loading_indicator_small
}


class RowndTestSandbox: Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        Rownd.configure(this, "b60bc454-c45f-47a2-8f8a-12b2062f5a77")
        Rownd.config.apiUrl = "https://api.us-east-2.dev.rownd.io"
        Rownd.config.baseUrl = "https://hub.rownd.workers.dev"
        Rownd.config.customizations = AppCustomizations(this)
//        Rownd.config.customizations.sheetBackgroundColor = Color(0xff25265a)

    }

    companion object {
        lateinit var instance: RowndTestSandbox
            private set
    }
}