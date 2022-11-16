package io.rownd.android.views

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.rownd.android.models.RowndConfig

class RowndWebViewModel(app: Application, var rowndConfig: RowndConfig) : ViewModel() {

    private val webView = RowndWebViewLiveData(app.applicationContext)
    fun webView(): MutableLiveData<RowndWebView?> = webView

    class Factory(private val app: Application, private val rowndConfig: RowndConfig) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return modelClass.getConstructor(Application::class.java, RowndConfig::class.java)
                .newInstance(app, rowndConfig)
        }
    }
}