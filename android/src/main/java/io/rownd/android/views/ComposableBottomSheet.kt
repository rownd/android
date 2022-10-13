package io.rownd.android.views

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import io.rownd.android.Rownd
import kotlinx.coroutines.launch


abstract class ComposableBottomSheetFragment : DialogFragment() {

    override fun onStart() {
        super.onStart()
        val dialog: Dialog? = dialog
        if (dialog != null) {
            dialog.window?.setDimAmount(0f)
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window?.setLayout(width, height)
        }
    }

//    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog? {
//        val dialog: BottomSheetDialog = super.onCreateDialog(savedInstanceState)
//        val window: Window? = dialog.window
//        window.setFlags(
//            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
//            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
//        )
//        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
//        return dialog
//    }

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.Transparent.toArgb()))

        dialog?.window?.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            )
        dialog?.window?.decorView?.systemUiVisibility = requireActivity().window!!.decorView.systemUiVisibility

        dialog?.setOnShowListener { // Clear the not focusable flag from the window
            dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

            // Update the WindowManager with the new attributes (no nicer way I know of to do this)..
            val wm = activity?.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.updateViewLayout(dialog?.window?.decorView, dialog?.window?.attributes)
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                BottomSheet()
            }
        }
    }

    override fun dismiss() {
        super.dismiss()
    }

    @ExperimentalMaterialApi
    @Composable
    private fun BottomSheet() {
        val coroutineScope = rememberCoroutineScope()

        // Declaring a Boolean value to
        // store bottom sheet collapsed state
        val bottomSheetState = rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.HalfExpanded
        )

        val configuration = LocalConfiguration.current
        val fullScreenHeight = configuration.screenHeightDp.dp

        // State change callback
        LaunchedEffect(bottomSheetState) {
            snapshotFlow { bottomSheetState.isVisible }.collect { isVisible ->
                if (isVisible) {
                    // Sheet is visible
                } else {
                    dismiss()
                }
            }
        }

        // Creating a Bottom Sheet
        ModalBottomSheetLayout(
            sheetState = bottomSheetState,
            sheetShape = RoundedCornerShape(Rownd.config.customizations.sheetCornerBorderRadius),
            sheetContent =  {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .background(Rownd.config.customizations.sheetBackgroundColor)
                ) {
                    Column(
                        modifier = Modifier
                            .height(fullScreenHeight)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Content()
                    }
                }
            },
        ) {
            Text("")
        }

        val view = LocalView.current
        DisposableEffect(view) {
            val listener = ViewTreeObserver.OnGlobalLayoutListener {
                val isKeyboardOpen = ViewCompat.getRootWindowInsets(view)
                    ?.isVisible(WindowInsetsCompat.Type.ime()) ?: true
                if (isKeyboardOpen) {
                    coroutineScope.launch {
                        bottomSheetState.animateTo(ModalBottomSheetValue.Expanded)
                    }
                }
            }

            view.viewTreeObserver.addOnGlobalLayoutListener(listener)
            onDispose {
                view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
            }
        }
    }

    @Composable
    abstract fun Content()
}