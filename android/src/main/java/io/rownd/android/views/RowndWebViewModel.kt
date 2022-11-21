package io.rownd.android.views

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.rownd.android.RowndClient

class RowndWebViewModel(app: Application, var rowndClient: RowndClient) : ViewModel() {

    private val webView = RowndWebViewLiveData(rowndClient)
    fun webView(): MutableLiveData<RowndWebView?> = webView

    class Factory(private val app: Application, private val rowndClient: RowndClient) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return modelClass.getConstructor(Application::class.java, RowndClient::class.java)
                .newInstance(app, rowndClient)
        }
    }
}