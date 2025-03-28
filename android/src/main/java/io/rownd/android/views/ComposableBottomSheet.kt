package io.rownd.android.views

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import io.rownd.android.Rownd
import io.rownd.android.util.bottom.sheet.ModalBottomSheet
import io.rownd.android.util.bottom.sheet.SheetState
import io.rownd.android.util.bottom.sheet.SheetValue
import io.rownd.android.util.convertStringToColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
abstract class ComposableBottomSheetFragment : DialogFragment() {
    open val shouldDisplayLoader = false

    @OptIn(ExperimentalMaterial3Api::class)
    var sheetState: SheetState? = null

    var isKeyboardOpen = false

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
        dialog?.window?.setBackgroundDrawable(Color.Transparent.toArgb().toDrawable())

        dialog?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )

        dialog?.window?.decorView?.let { decorView ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    requireActivity().window?.insetsController?.let { controller ->
                        decorView.setOnApplyWindowInsetsListener { _, insets ->
                            controller.show(WindowInsetsCompat.Type.systemBars())
                            insets
                        }
                    }
                } else {
                    decorView.systemUiVisibility =
                        requireActivity().window?.decorView?.systemUiVisibility ?: 0
                }
            } catch (e: Exception) {
                Log.d("ComposableBottomSheetFragment", "onCreateView: $e")
            }
        }

        dialog?.setOnShowListener { // Clear the not focusable flag from the window
            dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

            // Update the WindowManager with the new attributes (no nicer way I know of to do this)..
            val wm = activity?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            wm?.updateViewLayout(dialog?.window?.decorView, dialog?.window?.attributes)
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
    @Suppress("UnusedBoxWithConstraintsScope")
    @Composable
    private fun BottomSheet() {
        val coroutineScope = rememberCoroutineScope()

        // Declaring a Boolean value to
        // store bottom sheet collapsed state
//        var openBottomSheet by rememberSaveable { mutableStateOf(false) }
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
        val (canTouchBackgroundToDismiss, setCanTouchBackgroundToDismiss) = remember { mutableStateOf<Boolean>(true) }

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

        // Creating a Bottom Sheet
        ModalBottomSheet(
            sheetState = bottomSheetState,
            canTouchBackgroundToDismiss = canTouchBackgroundToDismiss,
            contentColor = Rownd.config.customizations.dynamicSheetBackgroundColor,
            containerColor = Rownd.config.customizations.dynamicSheetBackgroundColor,
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
                            .offset(y = (-24).dp)
                    ) {
                        Content(bottomSheetState, setIsLoading, setCanTouchBackgroundToDismiss)
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


        val context = LocalContext.current
        DisposableEffect(context) {
            val activity = context as? ComponentActivity ?: return@DisposableEffect onDispose { }
            val rootView = activity.window.decorView.rootView
            val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
                val isKeyboardOpen = ViewCompat.getRootWindowInsets(rootView)?.isVisible(WindowInsetsCompat.Type.ime()) ?: true;

                if (isKeyboardOpen != this@ComposableBottomSheetFragment.isKeyboardOpen || !this@ComposableBottomSheetFragment.isKeyboardOpen)
                {
                    coroutineScope.launch {
                        if (bottomSheetState.currentValue != SheetValue.Expanded) {
                            bottomSheetState.snapTo(SheetValue.Expanded)
                        }
                    }
                    this@ComposableBottomSheetFragment.isKeyboardOpen = isKeyboardOpen;
                }
            }

            rootView.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)

            onDispose {
                rootView.viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    abstract fun Content(
        bottomSheetState: SheetState,
        setIsLoading: (isLoading: Boolean) -> Unit,
        setCanTouchBackgroundToDismiss: (canTouchBackgroundToDismiss: Boolean) -> Unit
    )
}
