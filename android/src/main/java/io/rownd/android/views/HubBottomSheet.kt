package io.rownd.android.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.rownd.android.R
import io.rownd.android.Rownd
import io.rownd.android.util.RowndException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
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
            (bundle?.getSerializable(HubBottomSheetBundleKeys.TargetPage.key) ?: HubPageSelector.Unknown) as HubPageSelector
        val jsFnArgsAsJson = bundle?.getString(JS_FN_OPTIONS)

        val context = context
            ?: Rownd.appHandleWrapper.app.get()?.applicationContext
            ?: throw RowndException("Unable to locate context. Did you forget to call Rownd.configure()?")

        val hubView = RowndWebView(context, null)
        hubView.targetPage = targetPage

        if (jsFnArgsAsJson != null) {
            hubView.jsFunctionArgsAsJson = jsFnArgsAsJson
        }

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
        set1.connect(hubView.id, ConstraintSet.TOP, constraintLayout.id, ConstraintSet.TOP, 0)
        set1.connect(hubView.id, ConstraintSet.BOTTOM, constraintLayout.id, ConstraintSet.BOTTOM, 0)

        set1.applyTo(constraintLayout);

        val dialog = (dialog as BottomSheetDialog)
            val behavior = dialog.behavior

            val rootView = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

            rootView?.post {
                val bottomSheetVisibleHeight = rootView.height - rootView.top
                (subView as RowndWebView).minimumHeight = bottomSheetVisibleHeight
//                constraintLayout.maxHeight = bottomSheetVisibleHeight
            }

            behavior.apply {
                addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {

                        // set the y coordinates of the bottom layout on bottom sheet slide
                        val bottomSheetVisibleHeight = bottomSheet.height - bottomSheet.top
//                        constraintLayout.y =
//                            (bottomSheetVisibleHeight - constraintLayout.height).toFloat()
                        (subView as RowndWebView).minimumHeight = bottomSheetVisibleHeight
//                        constraintLayout.maxHeight = bottomSheetVisibleHeight
                    }

                    override fun onStateChanged(bottomSheet: View, newState: Int) {
//                        val bottomSheetVisibleHeight = bottomSheet.height - bottomSheet.top
//                        constraintLayout.maxHeight = bottomSheetVisibleHeight
//                        println("bottomSheetHeight: ${bottomSheet.height}")
                    }
                })
            }

        return view
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