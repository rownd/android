package io.rownd.android.views.key_transfer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import io.rownd.android.views.ComposableBottomSheetFragment
import io.rownd.android.util.bottom.sheet.*

class KeyTransferBottomSheet : ComposableBottomSheetFragment() {

    private var permissionRequestCallback: ((isGranted: Boolean) -> Unit)? = null
    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionRequestCallback?.invoke(isGranted)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(bottomSheetState: SheetState, setIsLoading: (isLoading: Boolean) -> Unit, setDynamicHeight: (dynamicHeight: Float) -> Unit, setCanTouchBackgroundToDismiss: (canTouchBackgroundToDismiss: Boolean) -> Unit) {
        KeyTransferNavHost(hostController = this)
    }

    internal fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this.requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    internal fun requestCameraPermissions(
        rationaleCallback: (() -> Unit)? = null,
        isReRequestingPermission: Boolean = false,
        callback: (isGranted: Boolean) -> Unit
    ) {
        permissionRequestCallback = callback
        when {
            ContextCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
                callback.invoke(true)
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) && !isReRequestingPermission -> {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected. In this UI,
                // include a "cancel" or "no thanks" button that allows the user to
                // continue using your app without granting the permission.
//            showInContextUI(...)
                rationaleCallback?.invoke()
            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }

    companion object {
        const val TAG = "KeyTransferComposableBottomSheet"

        fun newInstance(): KeyTransferBottomSheet {
            val bundle = Bundle()

            val bottomSheet = KeyTransferBottomSheet()
            bottomSheet.arguments = bundle

            return bottomSheet
        }
    }
}