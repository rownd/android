package io.rownd.android.views

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.rownd.android.R
import io.rownd.android.Rownd
import kotlinx.serialization.json.Json

class RowndBottomSheetActivity : ComponentActivity() {
    private var bottomSheetHolder: HubComposableBottomSheet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        setTheme(R.style.Theme_Rownd_Transparent)
        super.onCreate(savedInstanceState)

        Rownd.signInWithGoogle.registerIntentLauncher(this)

        val targetPage = intent.getSerializableExtra(EXTRA_TARGET_PAGE) as? HubPageSelector ?: HubPageSelector.Unknown
        val jsFnOptions = intent.getStringExtra(EXTRA_JS_FN_OPTIONS)

        bottomSheetHolder = HubComposableBottomSheet(
            this,
            onDismiss = { dismiss() },
            targetPage = targetPage,
            jsFnArgsAsJson = jsFnOptions
        )

        setContent {
            bottomSheetHolder?.BottomSheet()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Rownd.signInWithGoogle.deRegisterIntentLauncher(this.localClassName)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSheetRequest(intent)
    }

    private fun handleSheetRequest(intent: Intent) {
        val targetPage = intent.getSerializableExtra(EXTRA_TARGET_PAGE) as? HubPageSelector ?: HubPageSelector.Unknown
        val jsFnOptions = intent.getStringExtra(EXTRA_JS_FN_OPTIONS)

        bottomSheetHolder

        bottomSheetHolder?.existingWebView?.loadNewPage(targetPage, jsFnOptions)
    }

    private fun dismiss() {
        finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        private const val EXTRA_TARGET_PAGE = "extra_target_page"
        private const val EXTRA_JS_FN_OPTIONS = "extra_js_fn_options"

        val json = Json { encodeDefaults = true }

        fun launch(context: Context, targetPage: HubPageSelector, jsFnOptions: String? = null) {
            val intent = Intent(context, RowndBottomSheetActivity::class.java).apply {
                putExtra(EXTRA_TARGET_PAGE, targetPage) // Must be Serializable or Parcelable
                jsFnOptions?.let { putExtra(EXTRA_JS_FN_OPTIONS, it) }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Required if launching from non-activity context
            }

            context.startActivity(intent)

            if (context is Activity) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    context.overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
                    context.overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
                } else {
                    @Suppress("DEPRECATION")
                    context.overridePendingTransition(0, 0)
                }
            }
        }
    }
}