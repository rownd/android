package io.rownd.android.views.key_transfer

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import io.rownd.android.R
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
internal fun KeyTransferScanner(
    hostController: KeyTransferBottomSheet,
    viewModel: KeyTransferViewModel,
    onNavBack: () -> Unit,
    onNavToShowProgress: () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onNavBack,
                modifier = Modifier.padding(start = 0.dp)
            ) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    contentDescription = "Back to initiate key transfer",
                )
            }
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = "Scan QR code",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = Dp(10F)),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                lineHeight = 24.sp,
                text = "If you have an account attached to another device, you can securely transfer that account data to this device."
            )

            if (hostController.isCameraPermissionGranted()) {
                Text(
                    lineHeight = 24.sp,
                    text = "Scan the QR code that is displayed on your other device."
                )

                CameraView(
                    onValueCaptured = {
                        Log.d("Rownd.enc_key_transfer_scanner", it)
                        viewModel.receiveKeyTransfer(it)
                        onNavToShowProgress()
                    },
                    onError = {
                        Log.e("Rownd.enc_key_transfer_scanner", it.message.orEmpty())
                    }
                )
            }

            TextButton(
                onClick = {
                    // TODO: Handle manual mode
                },
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .fillMaxWidth()
            ) {
                Text("Enter code manually")
            }
        }
    }
}