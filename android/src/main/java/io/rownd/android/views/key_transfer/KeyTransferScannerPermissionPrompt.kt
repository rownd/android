package io.rownd.android.views.key_transfer

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import io.rownd.android.R
import io.rownd.android.ui.theme.RowndButton

@Composable
fun KeyTransferScannerPermissionPrompt(
    hostController: KeyTransferBottomSheet,
    onNavBack: () -> Unit,
    onNavToShowScanner: () -> Unit
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
                text = "Camera permission required",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Column(modifier = Modifier.padding(horizontal = Dp(10F))) {
            Text(
                lineHeight = 24.sp,
                text = "The easiest way to sync your data to this device is by using your device's camera."
            )
            Text(
                lineHeight = 24.sp,
                text = "If you don't want to allow access to your camera, select \"Enter key manually\" instead."
            )

            RowndButton(
                onClick = {
                    hostController.requestCameraPermissions(isReRequestingPermission = true) {
                        onNavToShowScanner()
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 0.dp, vertical = 10.dp)
                    .fillMaxWidth()
            ) {
                Text(text = "Use camera")
            }

            TextButton(
                onClick = {
                    // TODO: Handle manual mode
                },
                modifier = Modifier
                    .padding(horizontal = 0.dp, vertical = 10.dp)
                    .fillMaxWidth()
            ) {
                Text("Enter code manually")
            }
        }
    }
}