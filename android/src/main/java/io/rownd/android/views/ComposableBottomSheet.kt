package io.rownd.android.views

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import io.rownd.android.Rownd
import kotlinx.coroutines.launch


abstract class ComposableBottomSheetFragment : DialogFragment() {
    open val shouldDisplayLoader = false

    @OptIn(ExperimentalMaterialApi::class)
    var sheetState: ModalBottomSheetState? = null

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

    // Might be needed for better display over device "safe areas"
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
        dialog?.window?.decorView?.systemUiVisibility =
            requireActivity().window!!.decorView.systemUiVisibility

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

    @ExperimentalMaterialApi
    @Composable
    private fun BottomSheet() {
        val coroutineScope = rememberCoroutineScope()

        // Declaring a Boolean value to
        // store bottom sheet collapsed state
        val bottomSheetState = rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.HalfExpanded,

            // TODO: Perhaps we should support blocking the bottom sheet from closing
            //  when certain operations are in progress.
            // confirmStateChange = { false }
        )



        SideEffect {
            sheetState = bottomSheetState
        }

        val (isLoading, setIsLoading) = remember { mutableStateOf(shouldDisplayLoader) }
        val contentAlpha: Float by animateFloatAsState(if (!isLoading) 1f else 0f)
        var loadingLottieComposition: LottieComposition? = null

        Rownd.config.customizations.loadingAnimation?.let { loadingAnimation ->
            loadingLottieComposition =
                rememberLottieComposition(LottieCompositionSpec.RawRes(loadingAnimation)).value
        }

        Rownd.config.customizations.loadingAnimationJsonString?.let { loadingAnimationJsonString ->
            loadingLottieComposition =
                rememberLottieComposition(
                    LottieCompositionSpec.JsonString(
                        loadingAnimationJsonString
                    )
                ).value
        }


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
            sheetBackgroundColor = Rownd.config.customizations.dynamicSheetBackgroundColor,
            sheetShape = RoundedCornerShape(
                topStart = Rownd.config.customizations.sheetCornerBorderRadius,
                topEnd = Rownd.config.customizations.sheetCornerBorderRadius
            ),
            sheetContent = {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .alpha(contentAlpha)
                    ) {
                        Content(bottomSheetState, setIsLoading)
                    }

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .offset(y = 100.dp)
                        ) {
                            if (loadingLottieComposition != null) {
                                LottieAnimation(
                                    composition = loadingLottieComposition,
                                    iterations = LottieConstants.IterateForever
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            })
        {
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

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    abstract fun Content(
        bottomSheetState: ModalBottomSheetState,
        setIsLoading: (isLoading: Boolean) -> Unit
    )
}
