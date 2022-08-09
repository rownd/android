package io.rownd.android.util

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Bundle
import androidx.annotation.Nullable
import java.lang.ref.WeakReference

enum class ContextType {
    APP, ACTIVITY
}

class AppLifecycleListener(parentApp: Application) : ActivityLifecycleCallbacks {
    var app: WeakReference<Application>
        private set
    lateinit var activity: WeakReference<Activity>
        private set

    override fun onActivityCreated(activity: Activity, @Nullable bundle: Bundle?) {
        this.activity = WeakReference(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        this.activity = WeakReference(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        this.activity = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    init {
        app = WeakReference(parentApp)
        parentApp.registerActivityLifecycleCallbacks(this)
    }
}