package io.rownd.android.views

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

enum class HubBottomSheetBundleKeys(val key: String) {
    TargetPage("target_page")
}

class HubComposableBottomSheet(
    activity: RowndBottomSheetActivity,
    override val onDismiss: () -> Unit = {},
    private val targetPage: HubPageSelector = HubPageSelector.Unknown,
    private val jsFnArgsAsJson: String? = null
) : ComposableBottomSheet(activity) {
    override val shouldDisplayLoader = true

    internal var existingWebView: RowndWebView? = null
    internal var isDismissing: Boolean = false
    private var viewModel: RowndWebViewModel? = null

    init {
        viewModel = ViewModelProvider(this.activity)[RowndWebViewModel::class.java]
        viewModel?.webView()?.observe(this.activity) {
            existingWebView = it
        }
    }
        // Internal dismiss function to recycle web view
        override fun dismiss() {
            isDismissing = true
            viewModel?.webView()?.postValue(null)

            super.dismiss()
        }

        @ExperimentalMaterial3Api
        @Composable
        override fun Content(
            requestDetent: (detent: SheetDetent) -> Unit,
            setIsLoading: (isLoading: Boolean) -> Unit,
            setCanTouchBackgroundToDismiss: (canTouchBackgroundToDismiss: Boolean) -> Unit
        ) {
            val coroutineScope = rememberCoroutineScope()

            LaunchedEffect(this.jsFnArgsAsJson) {
                Log.d("HubComposableBottomSheet", "jsFnArgsAsJson: $jsFnArgsAsJson")
            }

            val (hasLoadedUrl, setHasLoadedUrl) = remember { mutableStateOf(false) }

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
                    this.hubWebview.progressBar = this.hubProgressBar
                    this.hubWebview.setIsLoading = setIsLoading

                    this.hubWebview.animateBottomSheet = {
                        requestDetent(it)
                    }
                    this.hubWebview.setCanTouchBackgroundToDismiss = {
                        coroutineScope.launch {
                            setCanTouchBackgroundToDismiss(it)
                        }
                    }
                    this.hubWebview.dismiss = {
                        onDismiss()
                    }
                    if (!hasLoadedUrl) {
                        this.hubWebview.targetPage = this@HubComposableBottomSheet.targetPage
                        this.hubWebview.jsFunctionArgsAsJson = this@HubComposableBottomSheet.jsFnArgsAsJson ?: RowndWebView.DEFAULT_JS_FN_ARGS

                        this.let {
                            coroutineScope.launch {
                                val url = viewModel?.rowndClient?.config?.hubLoaderUrl()
                                it.hubWebview.loadUrl(url!!)
                            }
                        }
                        setHasLoadedUrl(true)
                    }
                }
            )
        }
    }