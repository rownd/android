package io.rownd.android.views

import android.app.Dialog
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.ui.unit.Dp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.rownd.android.R

open class BottomSheet() : BottomSheetDialogFragment() {

    protected open var layoutId: Int = R.layout.modal_bottom_sheet_content
    protected var subView: View? = null
    protected var spinner: ProgressBar? = null

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        val contextThemeWrapper: Context = ContextThemeWrapper(requireContext(), R.style.Theme_Rownd)
        return inflater.cloneInContext(contextThemeWrapper)
    }

    override fun onStart() {
        super.onStart()
        val containerID = com.google.android.material.R.id.design_bottom_sheet
        val bottomSheet: FrameLayout? = dialog?.findViewById(containerID)
        bottomSheet?.let {
            BottomSheetBehavior.from<FrameLayout?>(it).state =
                BottomSheetBehavior.STATE_HALF_EXPANDED
            bottomSheet.layoutParams.height = FrameLayout.LayoutParams.MATCH_PARENT
        }

        view?.post {
            val params = (view?.parent as View).layoutParams as (CoordinatorLayout.LayoutParams)
            val behavior = params.behavior
            val bottomSheetBehavior = behavior as (BottomSheetBehavior)
            bottomSheetBehavior.peekHeight = view?.measuredHeight ?: 0
            (bottomSheet?.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
//        dialog?.setOnShowListener { dialog ->
//            val d = dialog as BottomSheetDialog
//            val bottomSheetInternal =
//                d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
//            bottomSheetInternal?.minimumHeight=
//                Resources.getSystem().displayMetrics.heightPixels
//        }

//        dialog?.setOnShowListener {
//            val dialog = it as BottomSheetDialog
//            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
//            bottomSheet?.let { sheet ->
//                dialog.behavior.peekHeight = sheet.height
//                sheet.parent.parent.requestLayout()
//            }
//        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val parent = this

        return inflater.inflate(layoutId, container, false).apply {
            setStyle(STYLE_NORMAL, R.style.RowndBottomSheetDialog)

//            val spinner = ProgressBar(context)
//            parent.spinner = spinner
//            spinner.id = View.generateViewId()
//            spinner.layoutParams = ViewGroup.LayoutParams(, ViewGroup.LayoutParams.WRAP_CONTENT)
//            spinner.foregroundGravity = Gravity.CENTER

            val subView = subView ?: return this

            if (subView.id == -1) {
                subView.id = arguments?.getInt("subview_id") ?: View.generateViewId()
            }

            // Put reference to dialog
            if (subView is DialogChild) {
                subView.dialog = parent
            }

            // Center the spinner
//            val set2 = ConstraintSet()
//            set2.connect(spinner.id, ConstraintSet.LEFT, constraintLayout.id, ConstraintSet.LEFT, 0)
//            set2.connect(spinner.id, ConstraintSet.RIGHT, constraintLayout.id, ConstraintSet.RIGHT, 0)
//            set2.connect(spinner.id, ConstraintSet.TOP, constraintLayout.id, ConstraintSet.TOP, 0)
//            set2.connect(spinner.id, ConstraintSet.BOTTOM, constraintLayout.id, ConstraintSet.BOTTOM, 0)
//            set2.applyTo(constraintLayout)

//            val dialog = (dialog as BottomSheetDialog)
//            val behavior = dialog.behavior

//            val rootView = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
//
//            rootView?.post {
//                val bottomSheetVisibleHeight = rootView.height - rootView.top
////                constraintLayout.minHeight = bottomSheetVisibleHeight
////                constraintLayout.maxHeight = bottomSheetVisibleHeight
//            }
//
//            behavior.apply {
//                addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
//                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
//
//                        // set the y coordinates of the bottom layout on bottom sheet slide
////                        val bottomSheetVisibleHeight = bottomSheet.height - bottomSheet.top
////                        constraintLayout.y =
////                            (bottomSheetVisibleHeight - constraintLayout.height).toFloat()
////                        constraintLayout.minHeight = bottomSheetVisibleHeight
////                        constraintLayout.maxHeight = bottomSheetVisibleHeight
//                    }
//
//                    override fun onStateChanged(bottomSheet: View, newState: Int) {
////                        val bottomSheetVisibleHeight = bottomSheet.height - bottomSheet.top
////                        constraintLayout.maxHeight = bottomSheetVisibleHeight
////                        println("bottomSheetHeight: ${bottomSheet.height}")
//                    }
//                })
//            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) : Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setContentView(View(context))

//        val root = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

        val behavior = (dialog as BottomSheetDialog).behavior

//        val behavior = BottomSheetBehavior.from<View>(root)

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
