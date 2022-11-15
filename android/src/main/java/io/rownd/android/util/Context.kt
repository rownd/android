package io.rownd.android.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

fun getFragmentManager(context: Context?): FragmentManager? {
    val activity: Activity = getActivity(context)
    return if (activity is FragmentActivity) {
        activity.supportFragmentManager
    } else null
}

fun getActivity(context: Context?): Activity {
    var context: Context? = context
        ?: throw NullPointerException("Context must not be null")
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = (context as ContextWrapper).baseContext
    }
    throw IllegalArgumentException("The Context is not an Activity context!")
}