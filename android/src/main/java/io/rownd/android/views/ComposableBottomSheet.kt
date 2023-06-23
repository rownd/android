package io.rownd.android.views

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import io.rownd.android.util.convertStringToColor
import io.rownd.android.util.bottom.sheet.*
import io.rownd.android.util.bottom.sheet.SheetState
import io.rownd.android.util.bottom.sheet.SheetValue


abstract class ComposableBottomSheetFragment : DialogFragment() {
    open val shouldDisplayLoader = false

    @OptIn(ExperimentalMaterial3Api::class)
    var sheetState: SheetState? = null

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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun BottomSheet() {
        val coroutineScope = rememberCoroutineScope()

        // Declaring a Boolean value to
        // store bottom sheet collapsed state
        var openBottomSheet by rememberSaveable { mutableStateOf(false) }
        var skipPartiallyExpanded by remember { mutableStateOf(false) }
        val bottomSheetState = io.rownd.android.util.bottom.sheet.rememberModalBottomSheetState(
            skipPartiallyExpanded = skipPartiallyExpanded,

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
        val (dynamicOffset, setDynamicOffset) = remember { mutableStateOf<Float?>(null) }

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

        val primaryColor: Color = convertStringToColor(Rownd.store.currentState.appConfig.config.hub.customizations?.primaryColor ?: "#5b13df")

        val configuration = LocalConfiguration.current
        val fullScreenHeight = configuration.screenHeightDp.dp


        // Creating a Bottom Sheet
        ModalBottomSheet(
            sheetState = bottomSheetState,
            dynamicOffset = dynamicOffset,
            contentColor = Rownd.config.customizations.dynamicSheetBackgroundColor,
            shape = RoundedCornerShape(Rownd.config.customizations.sheetCornerBorderRadius),
            onDismissRequest = { dismiss() },
            content = {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .alpha(contentAlpha)
                    ) {
                        Content(bottomSheetState, setIsLoading, setDynamicOffset)
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
                                    modifier = Modifier.align(Alignment.Center),
                                    color = primaryColor
                                )
                            }
                        }
                    }
                }
            }
        )


        val view = LocalView.current
        DisposableEffect(view) {
            val listener = ViewTreeObserver.OnGlobalLayoutListener {
                val isKeyboardOpen = ViewCompat.getRootWindowInsets(view)
                    ?.isVisible(WindowInsetsCompat.Type.ime()) ?: true
                if (isKeyboardOpen) {
                    coroutineScope.launch {
                        bottomSheetState.animateTo(SheetValue.Expanded)
                    }
                } else {
                    coroutineScope.launch {
                        bottomSheetState.animateTo(SheetValue.PartiallyExpanded)
                    }
                }
            }

            view.viewTreeObserver.addOnGlobalLayoutListener(listener)
            onDispose {
                view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    abstract fun Content(
        bottomSheetState: SheetState,
        setIsLoading: (isLoading: Boolean) -> Unit,
        setDynamicOffset: (dynamicOffset: Float) -> Unit
    )
}
