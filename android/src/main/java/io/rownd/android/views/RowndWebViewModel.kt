package io.rownd.android.views

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class RowndWebViewModel(private val app: Application) : ViewModel() {

    private val webView = RowndWebViewLiveData(app.applicationContext)
    fun webView(): MutableLiveData<RowndWebView?> = webView

    fun test(view: RowndWebView) {
        webView.value = view
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return modelClass.getConstructor(Application::class.java)
                .newInstance(app)
        }
    }
}