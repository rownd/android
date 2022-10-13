package io.rownd.android.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidViewBinding
import io.rownd.android.Rownd
import io.rownd.android.databinding.HubViewLayoutBinding
import kotlinx.serialization.json.Json

class HubComposableBottomSheet : ComposableBottomSheetFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    @Composable
    override fun Content() {
        val bundle = this.arguments
        val targetPage: HubPageSelector =
            (bundle?.getSerializable(HubBottomSheetBundleKeys.TargetPage.key)
                ?: HubPageSelector.Unknown) as HubPageSelector
        val jsFnArgsAsJson = bundle?.getString(HubBottomSheet.JS_FN_OPTIONS)

        Log.d("HubComposableBottomSheet", "jsFnArgsAsJson: ${jsFnArgsAsJson}")

        val parent = this
        AndroidViewBinding(
            factory = HubViewLayoutBinding::inflate,
            update = {
//                LogCompositions(tag = "HubComposableBottomSheet", msg = "Composing AndroidViewBinding")
                Log.d("HubComposableBottomSheet", "Parent: ${parent}")
            this.hubWebview.dialog = parent
            val url = Rownd.config.hubLoaderUrl()
            this.hubWebview.progressBar = this.hubProgressBar
            this.hubWebview.targetPage = targetPage ?: HubPageSelector.SignIn
            this.hubWebview.jsFunctionArgsAsJson = jsFnArgsAsJson ?: "{}"
            this.hubWebview.loadUrl(url)
        })
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