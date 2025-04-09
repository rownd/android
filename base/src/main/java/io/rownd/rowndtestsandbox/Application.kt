package io.rownd.rowndtestsandbox

import android.app.Application
import android.util.Log
import io.rownd.android.Rownd

class RowndBaseApp: Application() {

    override fun onCreate() {
        super.onCreate()

        Rownd.configure(this, "key_c9vkjeiv515pt13d0lf1fyvt")
        Log.d("App.onCreate", "Rownd initialized: ${Rownd.state.value.isInitialized}")
        Rownd.config.apiUrl = "https://api.dev.rownd.io"
        Rownd.config.baseUrl = "https://hub.dev.rownd.io"
    }
}