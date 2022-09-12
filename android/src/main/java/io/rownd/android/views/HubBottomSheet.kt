package io.rownd.android.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import io.rownd.android.R
import io.rownd.android.Rownd
import kotlinx.serialization.json.Json

enum class HubBottomSheetBundleKeys(val key: String) {
    TargetPage("target_page")
}

class HubBottomSheet : BottomSheet() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val bundle = this.arguments
        val targetPage: HubPageSelector =
            (bundle?.getSerializable(HubBottomSheetBundleKeys.TargetPage.key)
                ?: HubPageSelector.Unknown) as HubPageSelector
        val jsFnArgsAsJson = bundle?.getString(JS_FN_OPTIONS)

        val subLayout = inflater.inflate(R.layout.hub_view_layout, container, false)

        val progressBar = subLayout.findViewById<ProgressBar>(R.id.hubProgressBar)
        val hubView = subLayout.findViewById<RowndWebView>(R.id.hub_webview)
        hubView.targetPage = targetPage
        hubView.progressBar = progressBar

        if (jsFnArgsAsJson != null) {
            hubView.jsFunctionArgsAsJson = jsFnArgsAsJson
        }

        val url = Rownd.config.hubLoaderUrl()
        hubView.loadUrl(url)

        subView = hubView

        super.onCreateView(inflater, container, savedInstanceState)

        return subLayout ?: throw Exception("View failed to create.")
    }

    companion object {
        val json = Json { encodeDefaults = true }
        const val JS_FN_OPTIONS = "JS_FN_OPTIONS"

        fun newInstance(targetPage: HubPageSelector, jsFnOptions: String?): HubBottomSheet {
            val bundle = Bundle()
            bundle.putSerializable(HubBottomSheetBundleKeys.TargetPage.key, targetPage)
            bundle.putInt("subview_id", R.id.hub_webview)

            if (jsFnOptions != null) {

                bundle.putString(JS_FN_OPTIONS, jsFnOptions)
            }

            val bottomSheet = HubBottomSheet()
            bottomSheet.arguments = bundle

            return bottomSheet
        }
    }

}