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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.composables.core.DragIndication
import com.composables.core.ModalBottomSheet
import com.composables.core.Scrim
import com.composables.core.Sheet
import com.composables.core.SheetDetent
import com.composables.core.SheetDetent.Companion.FullyExpanded
import com.composables.core.SheetDetent.Companion.Hidden
import com.composables.core.rememberModalBottomSheetState
import io.rownd.android.Rownd
import io.rownd.android.util.convertStringToColor
import kotlinx.coroutines.launch

val Peek = SheetDetent(identifier = "peek") { containerHeight, sheetHeight ->
    containerHeight * 0.6f
}

abstract class ComposableBottomSheetFragment : DialogFragment() {
    open val shouldDisplayLoader = false

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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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

    @Suppress("UnusedBoxWithConstraintsScope")
    @Composable
    private fun BottomSheet() {
        val coroutineScope = rememberCoroutineScope()

        val (isLoading, setIsLoading) = remember { mutableStateOf(shouldDisplayLoader) }
        val contentAlpha: Float by animateFloatAsState(if (!isLoading) 1f else 0f)
        var loadingLottieComposition: LottieComposition? = null
        val (canTouchBackgroundToDismiss, setCanTouchBackgroundToDismiss) = remember {
            mutableStateOf<Boolean>(
                true
            )
        }

        Rownd.config.customizations.loadingAnimation?.let { loadingAnimation ->
            loadingLottieComposition =
                rememberLottieComposition(LottieCompositionSpec.RawRes(loadingAnimation)).value
        }

        Rownd.config.customizations.loadingAnimationJsonString?.let { loadingAnimationJsonString ->
            loadingLottieComposition = rememberLottieComposition(
                LottieCompositionSpec.JsonString(
                    loadingAnimationJsonString
                )
            ).value
        }

        val primaryColor: Color = convertStringToColor(
            Rownd.store.currentState.appConfig.config.hub.customizations?.primaryColor ?: "#5b13df"
        )

        val sheetState = rememberModalBottomSheetState(
            initialDetent = Peek,
            detents = listOf(Hidden, Peek, FullyExpanded),
            confirmDetentChange = {
                canTouchBackgroundToDismiss
            }
        )

        // Creating a Bottom Sheet
        ModalBottomSheet(
            state = sheetState,
            onDismiss = {
                dismiss()
            },
        ) {
            Scrim(scrimColor = Color.Black.copy(0.3f), enter = fadeIn(), exit = fadeOut())
            Box() {
                Sheet(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal).asPaddingValues())
                        .shadow(40.dp, RoundedCornerShape(Rownd.config.customizations.sheetCornerBorderRadius))
                        .clip(RoundedCornerShape(Rownd.config.customizations.sheetCornerBorderRadius))
                        .background(Rownd.config.customizations.dynamicSheetBackgroundColor)
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .imePadding(),
                ) {
                    DragIndication(
                        modifier = Modifier.padding(top = 22.dp)
                            .background(
                                if (Rownd.config.customizations.dynamicSheetBackgroundColor.luminance() > 0.5) Color.Black else Color.White,
                                RoundedCornerShape(10.dp)
                            )
                            .width(42.dp)
                            .height(4.dp)
                            .zIndex(100F)
                    )
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .alpha(contentAlpha)
                                .offset(y = (-24).dp)
                                .padding(top = 35.dp)
                                .imePadding()
                        ) {
                            Content(requestDetent = {
                                coroutineScope.launch {
                                    sheetState.animateTo(it)
                                }
                            }, setIsLoading, setCanTouchBackgroundToDismiss)
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
            }
        }

        val context = LocalContext.current
        DisposableEffect(context) {
            val activity = context as? ComponentActivity ?: return@DisposableEffect onDispose { }
            val rootView = activity.window.decorView.rootView
            val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
                val isKeyboardOpen = ViewCompat.getRootWindowInsets(rootView)
                    ?.isVisible(WindowInsetsCompat.Type.ime()) ?: true;

                if (isKeyboardOpen && sheetState.currentDetent != FullyExpanded) {
                    coroutineScope.launch {
                        sheetState.animateTo(FullyExpanded)
                    }
                }
            }

            rootView.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)

            onDispose {
                rootView.viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
            }
        }
    }

    @Composable
    abstract fun Content(
        requestDetent: (detent: SheetDetent) -> Unit,
        setIsLoading: (isLoading: Boolean) -> Unit,
        setCanTouchBackgroundToDismiss: (canTouchBackgroundToDismiss: Boolean) -> Unit
    )
}
