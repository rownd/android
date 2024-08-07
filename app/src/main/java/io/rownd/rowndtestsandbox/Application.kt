package io.rownd.rowndtestsandbox

import android.app.Application
import android.content.res.Configuration
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.rownd.android.Rownd
import io.rownd.android.models.RowndCustomizations
import io.rownd.android.util.RowndEventType


class AppCustomizations(app: Application) : RowndCustomizations() {
    private var app: Application

    init {
        this.app = app
    }

    override val dynamicSheetBackgroundColor: Color
    get() {
            val uiMode = app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return if (uiMode == Configuration.UI_MODE_NIGHT_YES) {
                Color(red = 30, green = 30, blue = 30)
            } else {
                Color(red = 30, green = 30, blue = 30)
            }
        }

    override var sheetCornerBorderRadius: Dp = 25.dp
    //override var loadingAnimation: Int? = R.raw.loading_indicator_small
}


class RowndTestSandbox: Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        Rownd.configure(this, "key_pko8eul59xz33hr21jgxvx6s")
        Log.d("App.onCreate", "Rownd initialized: ${Rownd.state.value.isInitialized}")
        Rownd.config.apiUrl = "https://api.dev.rownd.io"
        Rownd.config.baseUrl = "https://hub.dev.rownd.io"
        Rownd.config.customizations = AppCustomizations(this)
        Rownd.config.customizations.sheetBackgroundColor = Color(red = 50, green = 50, blue = 50)
        Rownd.config.appleIdCallbackUrl = "https://api.us-east-2.dev.rownd.io/hub/auth/apple/callback"

        Rownd.addEventListener {
            when (it.event) {
                RowndEventType.SignInStarted -> {
                    // Do stuff
                }
                RowndEventType.SignInCompleted -> {
                    it.data?.get("user_type")?.let { it1 -> Log.d("App", it1.toString()) }
                }

                else -> {
                    // no-op
                }
            }
        }
    }

    companion object {
        lateinit var instance: RowndTestSandbox
            private set
    }
}