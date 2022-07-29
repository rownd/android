package io.rownd.android.util

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.annotation.Nullable

class AppLifecycleListener(parentApp: Application) : ActivityLifecycleCallbacks {
    var app: Application
    lateinit var activity: Activity
        private set

    override fun onActivityCreated(activity: Activity, @Nullable bundle: Bundle?) {
        this.activity = activity
    }

    override fun onActivityStarted(activity: Activity) {
        this.activity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        this.activity = activity
    }

    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    init {
        app = parentApp
        parentApp.registerActivityLifecycleCallbacks(this)
    }
}