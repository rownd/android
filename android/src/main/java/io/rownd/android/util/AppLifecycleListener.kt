package io.rownd.android.util

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.annotation.Nullable
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle.State
import kotlinx.collections.immutable.PersistentList
import java.lang.ref.WeakReference

enum class ContextType {
    APP, ACTIVITY
}

data class Listener(
    val states: PersistentList<State>,
    val callback: (activity: Activity) -> Unit
)

class AppLifecycleListener(parentApp: Application) : ActivityLifecycleCallbacks {
    var app: WeakReference<Application>
        private set
    var activity: WeakReference<Activity>? = null
        private set

    private var activityListeners: MutableList<Listener> = mutableListOf()

    init {
        app = WeakReference(parentApp)
        parentApp.registerActivityLifecycleCallbacks(this)
    }

    constructor(currentActivity: FragmentActivity) : this(currentActivity.application) {
        activity = WeakReference(currentActivity)
    }

    override fun onActivityCreated(activity: Activity, @Nullable bundle: Bundle?) {
        this.activity = WeakReference(activity)


        val listeners = activityListeners.filter() {
            it.states.contains(State.CREATED)
        }

        for (listener in listeners) {
            listener.callback.invoke(activity)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        this.activity = WeakReference(activity)

        val listeners = activityListeners.filter() {
            it.states.contains(State.STARTED)
        }
        for (listener in listeners) {
            listener.callback.invoke(activity)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        this.activity = WeakReference(activity)

        // This is probably one of the better trigger points for listeners
        // unless there's a need for something earlier in the lifecycle
        val listeners = activityListeners.filter() {
            it.states.contains(State.RESUMED)
        }
        for (listener in listeners) {
            listener.callback.invoke(activity)
        }
    }

    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        super.onActivityPreCreated(activity, savedInstanceState)

        val listeners = activityListeners.filter() {
            it.states.contains(State.INITIALIZED)
        }
        for (listener in listeners) {
            listener.callback.invoke(activity)
        }
    }

    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        val listeners = activityListeners.filter() {
            it.states.contains(State.DESTROYED)
        }
        for (listener in listeners) {
            listener.callback.invoke(activity)
        }
    }

    internal fun registerActivityListener(states: PersistentList<State>, immediate: Boolean = false, callback: (activity: Activity) -> Unit) {
        val activity = this.activity?.get() ?: null
        activityListeners.add(Listener(
            states,
            callback
        ))

        if (immediate && activity != null) {
            callback.invoke(activity)
        }
    }
}