package io.rownd.android.views

import android.os.Bundle
import android.util.Log
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.viewinterop.AndroidViewBinding
import io.rownd.android.Rownd
import io.rownd.android.databinding.HubViewLayoutBinding
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

enum class HubBottomSheetBundleKeys(val key: String) {
    TargetPage("target_page")
}

class HubComposableBottomSheet : ComposableBottomSheetFragment() {
    override val shouldDisplayLoader = true

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    override fun Content(bottomSheetState: ModalBottomSheetState, setIsLoading: (isLoading: Boolean) -> Unit) {
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
            factory = HubViewLayoutBinding::inflate,
            update = {
                this.hubWebview.dialog = parent
                val url = Rownd.config.hubLoaderUrl()
                this.hubWebview.progressBar = this.hubProgressBar
                this.hubWebview.setIsLoading = setIsLoading
                this.hubWebview.targetPage = targetPage ?: HubPageSelector.SignIn
                this.hubWebview.jsFunctionArgsAsJson = jsFnArgsAsJson ?: "{}"
                this.hubWebview.animateBottomSheet = {
                    coroutineScope.launch {
                        bottomSheetState.animateTo(it)
                    }
                }
                if (!hasLoadedUrl) {
                    this.hubWebview.loadUrl(url)
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