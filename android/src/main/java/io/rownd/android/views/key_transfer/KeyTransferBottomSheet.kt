package io.rownd.android.views.key_transfer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import io.rownd.android.R
import io.rownd.android.views.*

class KeyTransferBottomSheet : BottomSheet() {

    override var layoutId = R.layout.key_transfer_layout
    private var permissionRequestCallback: ((isGranted: Boolean) -> Unit)? = null
    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionRequestCallback?.invoke(isGranted)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
//        val bundle = this.arguments

        val view = super.onCreateView(inflater, container, savedInstanceState)
            ?: throw Exception("Failed to create view")

        val composeView = view.findViewById<ComposeView>(R.id.key_transfer_start_compose_view)

        composeView.setContent {
            KeyTransferNavHost(hostController = this)
        }

        return view
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
        fun newInstance(): KeyTransferBottomSheet {
            val bundle = Bundle()

            val bottomSheet = KeyTransferBottomSheet()
            bottomSheet.arguments = bundle

            return bottomSheet
        }
    }
}