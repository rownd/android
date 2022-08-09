package io.rownd.android.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.rownd.android.Rownd
import io.rownd.android.util.RowndException


enum class HubBottomSheetBundleKeys(val key: String) {
    TargetPage("target_page")
}

class HubBottomSheet : BottomSheet() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val bundle = this.arguments
        val targetPage: HubPageSelector =
            (bundle?.getSerializable(HubBottomSheetBundleKeys.TargetPage.key) ?: HubPageSelector.Unknown) as HubPageSelector

        val context = context
            ?: Rownd.appHandleWrapper.app.get()?.applicationContext
            ?: throw RowndException("Unable to locate context. Did you forget to call Rownd.configure()?")

        val hubView = RowndWebView(context, null)
        hubView.targetPage = targetPage
        val url = Rownd.config.hubLoaderUrl()
        hubView.loadUrl(url)
        hubView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        subView = hubView

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    companion object {
        fun newInstance(targetPage: HubPageSelector): HubBottomSheet {
            val bundle = Bundle()
            bundle.putSerializable(HubBottomSheetBundleKeys.TargetPage.key, targetPage)

            val bottomSheet = HubBottomSheet()
            bottomSheet.arguments = bundle

            return bottomSheet
        }
    }

}