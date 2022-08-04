package io.rownd.android.views

import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.rownd.android.R

open class BottomSheet() : BottomSheetDialogFragment() {

    protected var subView: View? = null

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

            val subView = subView ?: return this

            if (subView.id == -1) {
                subView.id = View.generateViewId()
            }

            // Put reference to dialog
            if (subView is DialogChild) {
                subView.dialog = parent
            }

            val constraintLayout = findViewById<ConstraintLayout>(R.id.bottom_sheet_inner)
            val set = ConstraintSet()
            constraintLayout.addView(subView)

            set.clone(constraintLayout)
            set.connect(subView.id, ConstraintSet.TOP, constraintLayout.id, ConstraintSet.TOP, 0);
            set.applyTo(constraintLayout);
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) : Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setContentView(View(context))

        val root = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

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
