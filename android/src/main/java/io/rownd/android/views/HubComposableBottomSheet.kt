package io.rownd.android.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.lifecycle.ViewModelProvider
import com.composables.core.SheetDetent
import io.rownd.android.databinding.HubViewLayoutBinding
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

enum class HubBottomSheetBundleKeys(val key: String) {
    TargetPage("target_page")
}

class HubComposableBottomSheet : ComposableBottomSheetFragment() {
    override val shouldDisplayLoader = true

    internal var existingWebView: RowndWebView? = null
    internal var isDismissing: Boolean = false
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
        isDismissing = true
        viewModel?.webView()?.postValue(null)
        dismissAllowingStateLoss()
    }

    @ExperimentalMaterial3Api
    @Composable
    override fun Content(
        requestDetent: (detent: SheetDetent) -> Unit,
        setIsLoading: (isLoading: Boolean) -> Unit,
        setCanTouchBackgroundToDismiss: (canTouchBackgroundToDismiss: Boolean) -> Unit
    ) {
        val bundle = this.arguments
        val targetPage: HubPageSelector =
            (bundle?.getSerializable(HubBottomSheetBundleKeys.TargetPage.key)
                ?: HubPageSelector.Unknown) as HubPageSelector
        val jsFnArgsAsJson = bundle?.getString(JS_FN_OPTIONS)
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(jsFnArgsAsJson) {
            Log.d("HubComposableBottomSheet", "jsFnArgsAsJson: $jsFnArgsAsJson")
        }

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
                this.hubWebview.jsFunctionArgsAsJson = jsFnArgsAsJson ?: RowndWebView.DEFAULT_JS_FN_ARGS

                this.hubWebview.animateBottomSheet = {
                    requestDetent(it)
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