package io.rownd.android.util

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.annotation.Nullable
import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference
import kotlin.properties.Delegates

enum class ContextType {
    APP, ACTIVITY
}

class AppLifecycleListener(parentApp: Application) : ActivityLifecycleCallbacks {
    var app: WeakReference<Application>
        private set
    var activity: WeakReference<Activity>? by Delegates.observable(null) { property, oldValue, newValue ->
        // Listeners probably don't want to be called unless the activity has focus
//        if (newValue?.get()?.hasWindowFocus() != true) {
//            return
//        }

        for (listener in activityListeners) {
            listener.invoke(newValue?.get()!!)
        }

        activityListeners = mutableListOf()
    }
        private set

    private var activityListeners: MutableList<(activity: Activity) -> Unit> = mutableListOf()

    init {
        app = WeakReference(parentApp)
        parentApp.registerActivityLifecycleCallbacks(this)
    }

    constructor(currentActivity: FragmentActivity) : this(currentActivity.application) {
        activity = WeakReference(currentActivity)
    }

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

    internal fun onActivityInitialized(callback: (activity: Activity) -> Unit) {
        val activity = this.activity?.get() ?: null
        if (activity != null) {
            return callback.invoke(activity)
        }

        activityListeners.add(callback)
    }
}