package io.rownd.android.util

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle.State
import kotlinx.collections.immutable.PersistentList
import java.lang.ref.WeakReference

enum class ContextType {
    APP, ACTIVITY
}

data class Listener(
    val states: PersistentList<State>,
    val once: Boolean? = false,
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

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        this.activity = WeakReference(activity)


        val listeners = activityListeners.filter() {
            it.states.contains(State.CREATED)
        }

        callListeners(listeners, activity)
    }

    override fun onActivityStarted(activity: Activity) {
        this.activity = WeakReference(activity)

        val listeners = activityListeners.filter() {
            it.states.contains(State.STARTED)
        }

        callListeners(listeners, activity)
    }

    override fun onActivityResumed(activity: Activity) {
        this.activity = WeakReference(activity)
        isAppInForeground = true

        // This is probably one of the better trigger points for listeners
        // unless there's a need for something earlier in the lifecycle
        val listeners = activityListeners.filter() {
            it.states.contains(State.RESUMED)
        }

        callListeners(listeners, activity)
    }

    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        super.onActivityPreCreated(activity, savedInstanceState)

        val listeners = activityListeners.filter() {
            it.states.contains(State.INITIALIZED)
        }

        callListeners(listeners, activity)
    }

    override fun onActivityPaused(activity: Activity) {
        isAppInForeground = false
    }
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        val listeners = activityListeners.filter() {
            it.states.contains(State.DESTROYED)
        }

        callListeners(listeners, activity)
    }

    internal fun registerActivityListener(
        states: PersistentList<State>,
        immediate: Boolean = false,
        immediateIfBefore: State? = null,
        once: Boolean? = false,
        callback: (activity: Activity) -> Unit
    ) {
        val activity = this.activity?.get() as FragmentActivity?
        activityListeners.add(Listener(
            states,
            once,
            callback
        ))

        if (immediateIfBefore != null &&
            activity != null &&
            !activity.lifecycle.currentState.isAtLeast(immediateIfBefore)
        ) {
            callback.invoke(activity)
        } else if (immediate && activity != null) {
            callback.invoke(activity)
        }
    }

    private fun callListeners(listeners: List<Listener>, activity: Activity) {
        for (listener in listeners) {
            listener.callback.invoke(activity)
            if (listener.once == true) {
                activityListeners.remove(listener)
            }
        }
    }

    companion object {
        var isAppInForeground: Boolean = false
    }
}