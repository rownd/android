package io.rownd.android.views

import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.material.*
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.rownd.android.R


class BottomSheet(child: View) : BottomSheetDialogFragment() {
//    private var viewDelegate: BottomSheetViewDelegate
//    private lateinit var rootView: View
    private lateinit var subView: View

    init {
//        this.viewDelegate = viewDelegate
        this.subView = child

        if (subView.id == -1) {
            subView.id = View.generateViewId()
        }

        // Put reference to dialog
        if (child is DialogChild) {
            child.dialog = this
        }

//        var constraintLayout = rootView?.findViewById<ConstraintLayout>(R.id.bottom_sheet_inner)
//        if (constraintLayout != null) {
//            val set = ConstraintSet()
//            constraintLayout.addView(subView)
//
//            set.clone(constraintLayout)
//            set.connect(subView.id, ConstraintSet.TOP, constraintLayout.id, ConstraintSet.TOP, 0);
//            set.applyTo(constraintLayout);
//        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        dialog?.setOnShowListener { dialog ->
            val d = dialog as BottomSheetDialog
            val bottomSheetInternal =
                d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheetInternal?.minimumHeight=
                Resources.getSystem().displayMetrics.heightPixels
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val parent = this
        return inflater.inflate(R.layout.modal_bottom_sheet_content, container, false).apply {
            setStyle(STYLE_NORMAL, R.style.RowndBottomSheetDialog)
//            parent.rootView = this
//            findViewById<ComposeView>(R.id.compose_view).setContent {
//                Surface {
//                    Text("Hello from compose")
//
//                }
//            }

            var constraintLayout = findViewById<ConstraintLayout>(R.id.bottom_sheet_inner)
            var set = ConstraintSet()
            constraintLayout.addView(subView)

            set.clone(constraintLayout)
            set.connect(subView.id, ConstraintSet.TOP, constraintLayout.id, ConstraintSet.TOP, 0);
            set.applyTo(constraintLayout);

//            var frameLayout = findViewById<FrameLayout>(R.id.standard_bottom_sheet)
//            var view: View = inflater.inflate(viewDelegate.layoutId, frameLayout, false)
//            frameLayout.addView(subView)

//            var webView = findViewById<RowndWebView>(R.id.web_view)
//            webView.loadUrl(url)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) : Dialog {
        var dialog = super.onCreateDialog(savedInstanceState)
        dialog.setContentView(View(context))

        var root = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

        val behavior = BottomSheetBehavior.from<View>(root)

        behavior.setPeekHeight(320, true)
        behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        behavior.isDraggable = true
        behavior.isFitToContents = false

        behavior.saveFlags = BottomSheetBehavior.SAVE_ALL

        return dialog
    }

    companion object {
        const val TAG = "ModalBottomSheet"
    }
}
