package io.rownd.android.views

import android.view.ViewGroup
import android.webkit.WebView
import androidx.lifecycle.MutableLiveData
import io.rownd.android.RowndClient

class RowndWebViewLiveData(private val rowndClient: RowndClient) : MutableLiveData<RowndWebView?>() {
    var parentHolder: ViewGroup? = null
    var parentViewIndex: Int? = 0

    override fun postValue(value: RowndWebView?) {
        value?.rowndClient = rowndClient
        super.postValue(value)
    }

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