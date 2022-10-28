package io.rownd.android.views

import android.content.Context
import android.view.ViewGroup
import android.webkit.WebView
import androidx.lifecycle.MutableLiveData

class RowndWebViewLiveData(appContext: Context) : MutableLiveData<RowndWebView?>() {
    var parentHolder: ViewGroup? = null
    var parentViewIndex: Int? = 0

    override fun onActive() {
        if (value != null && value?.parent == null) {
            parentHolder?.addView(value, parentViewIndex ?: 0)
        }
    }
    override fun onInactive() {
        value?.detachFromParent()
    }

    private fun WebView.detachFromParent() {
        parentHolder = parent as? ViewGroup
        parentViewIndex = parentHolder?.indexOfChild(value)
        parentHolder?.removeView(this)
    }
}