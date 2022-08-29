package io.rownd.android.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import io.rownd.android.R
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
    ): View {
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

        val view = super.onCreateView(inflater, container, savedInstanceState)
            ?: throw Exception("View failed to create.")

        val constraintLayout = view.findViewById<ConstraintLayout>(R.id.bottom_sheet_inner)
        constraintLayout.addView(subView)
//            constraintLayout.addView(spinner)

        val set1 = ConstraintSet()
        set1.clone(constraintLayout)
        set1.connect(hubView.id, ConstraintSet.TOP, constraintLayout.id, ConstraintSet.TOP, 0);
        set1.applyTo(constraintLayout);

        return view
    }

    companion object {
        fun newInstance(targetPage: HubPageSelector): HubBottomSheet {
            val bundle = Bundle()
            bundle.putSerializable(HubBottomSheetBundleKeys.TargetPage.key, targetPage)
            bundle.putInt("subview_id", R.id.hub_webview)

            val bottomSheet = HubBottomSheet()
            bottomSheet.arguments = bundle

            return bottomSheet
        }
    }

}