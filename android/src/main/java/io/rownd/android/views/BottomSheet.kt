package io.rownd.android.views

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.rownd.android.R

open class BottomSheet : BottomSheetDialogFragment() {

    protected open var layoutId: Int = R.layout.modal_bottom_sheet_content
    protected var subView: View? = null
    lateinit var sheetBehavior: BottomSheetBehavior<FrameLayout>

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        val contextThemeWrapper: Context = ContextThemeWrapper(requireContext(), R.style.Theme_Rownd)
        return inflater.cloneInContext(contextThemeWrapper)
    }

    override fun onStart() {
        super.onStart()
        val containerID = com.google.android.material.R.id.design_bottom_sheet
        val bottomSheet: FrameLayout? = dialog?.findViewById(containerID)
        sheetBehavior = (dialog as BottomSheetDialog).behavior
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val parent = this

        return inflater.inflate(layoutId, container, false).apply {

            val subView = subView ?: return this

            if (subView.id == -1) {
                subView.id = arguments?.getInt("subview_id") ?: View.generateViewId()
            }

            // Put reference to dialog
            if (subView is DialogChild) {
                subView.dialog = parent
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) : Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setContentView(View(context))

        val behavior = (dialog as BottomSheetDialog).behavior

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
