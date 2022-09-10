package io.rownd.rowndtestsandbox

import android.app.Application
import io.rownd.android.Rownd

class RowndTestSandbox: Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        Rownd.configure(this, "b60bc454-c45f-47a2-8f8a-12b2062f5a77")
        Rownd.config.apiUrl = "https://api.us-east-2.dev.rownd.io"
        Rownd.config.baseUrl = "https://hub.rownd.workers.dev"
    }

    companion object {
        lateinit var instance: RowndTestSandbox
            private set
    }
}