package io.rownd.android.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.lifecycle.ViewModelProvider
import io.rownd.android.databinding.HubViewLayoutBinding
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

import io.rownd.android.util.bottom.sheet.*

enum class HubBottomSheetBundleKeys(val key: String) {
    TargetPage("target_page")
}

class HubComposableBottomSheet : ComposableBottomSheetFragment() {
    override val shouldDisplayLoader = true

    private var existingWebView: RowndWebView? = null
    private var viewModel: RowndWebViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this.requireActivity())[RowndWebViewModel::class.java]
        viewModel?.webView()?.observe(this) {
            existingWebView = it
        }
    }

    // Internal dismiss function to recycle web view
    private fun _dismiss() {
        viewModel?.webView()?.postValue(null)
        dismissAllowingStateLoss()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(bottomSheetState: SheetState, setIsLoading: (isLoading: Boolean) -> Unit, setDynamicHeight: (dynamicHeight: Float) -> Unit, setCanTouchBackgroundToDismiss: (canTouchBackgroundToDismiss: Boolean) -> Unit) {
        val bundle = this.arguments
        val targetPage: HubPageSelector =
            (bundle?.getSerializable(HubBottomSheetBundleKeys.TargetPage.key)
                ?: HubPageSelector.Unknown) as HubPageSelector
        val jsFnArgsAsJson = bundle?.getString(JS_FN_OPTIONS)
        val coroutineScope = rememberCoroutineScope()

        Log.d("HubComposableBottomSheet", "jsFnArgsAsJson: $jsFnArgsAsJson")

        val (hasLoadedUrl, setHasLoadedUrl) = remember { mutableStateOf(false) }

        val parent = this
        AndroidViewBinding(
            factory = { layoutInflater: LayoutInflater, viewGroup: ViewGroup, b: Boolean ->
                val view = HubViewLayoutBinding.inflate(layoutInflater, viewGroup, b)
                val rootViewGroup = view.root
                val currentWebView = view.hubWebview

                if (existingWebView != null && existingWebView?.parent == null) {
                    val oldWebViewIndex = viewGroup.indexOfChild(currentWebView)
                    rootViewGroup.removeView(currentWebView)
                    rootViewGroup.addView(existingWebView, oldWebViewIndex, currentWebView.layoutParams)
                } else {
                    viewModel?.webView()?.postValue(currentWebView)
                }

                return@AndroidViewBinding view
            },
            update = {
                this.hubWebview.dialog = parent
                this.hubWebview.progressBar = this.hubProgressBar
                this.hubWebview.setIsLoading = setIsLoading
                this.hubWebview.targetPage = targetPage
                this.hubWebview.jsFunctionArgsAsJson = jsFnArgsAsJson ?: "{}"
                this.hubWebview.animateBottomSheet = {
                    coroutineScope.launch {
                        setDynamicHeight(it)
                    }
                }
                this.hubWebview.setCanTouchBackgroundToDismiss = {
                    coroutineScope.launch {
                        setCanTouchBackgroundToDismiss(it)
                    }
                }
                this.hubWebview.dismiss = {
                    _dismiss()
                }
                if (!hasLoadedUrl) {
                    val parentScope = this
                    coroutineScope.launch {
                        val url = viewModel?.rowndClient?.config?.hubLoaderUrl()
                        parentScope.hubWebview.loadUrl(url!!)
                    }
                    setHasLoadedUrl(true)
                }
            }
        )
    }

    companion object {
        const val TAG = "HubComposableBottomSheet"

        val json = Json { encodeDefaults = true }
        private const val JS_FN_OPTIONS = "JS_FN_OPTIONS"

        fun newInstance(targetPage: HubPageSelector, jsFnOptions: String?): ComposableBottomSheetFragment {
            val bundle = Bundle()
            bundle.putSerializable(HubBottomSheetBundleKeys.TargetPage.key, targetPage)

            if (jsFnOptions != null) {
                bundle.putString(JS_FN_OPTIONS, jsFnOptions)
            }

            val bottomSheet = HubComposableBottomSheet()
            bottomSheet.arguments = bundle

            return bottomSheet
        }
    }
}