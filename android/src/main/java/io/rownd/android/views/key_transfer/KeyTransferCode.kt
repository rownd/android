package io.rownd.android.views.key_transfer

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.fragment.app.Fragment
import io.rownd.android.Rownd
import io.rownd.android.databinding.HubViewLayoutBinding
import io.rownd.android.ui.theme.IconCopy
import io.rownd.android.ui.theme.IconFilledCircleCheck
import io.rownd.android.ui.theme.RowndButton
import io.rownd.android.views.HubPageSelector
import kotlinx.coroutines.launch

@Composable
internal fun KeyTransferCode(
    onNavBack: () -> Unit,
    viewModel: KeyTransferViewModel
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val didCopyToClipboard = remember { mutableStateOf(false) }
    val didInit = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (!didInit.value) {
            viewModel.setupKeyTransfer()
            didInit.value = true
        }
    }

    Column(modifier = Modifier.padding(bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onNavBack,

                ) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    contentDescription = "Back to initiate key transfer",
                )
            }
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = "Show encryption key",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Column(
            modifier = Modifier
                .padding(horizontal = Dp(10F)),
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top)
        ) {
            Text(
                lineHeight = 24.sp,
                text = "To sign in on another device, scan the QR code below with the new device."
            )

            // QRCodeWebView
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                AndroidViewBinding(HubViewLayoutBinding::inflate) {
                    this.hubWebview.progressBar = this.hubProgressBar
                    this.hubWebview.targetPage = HubPageSelector.QrCode
                    this.hubWebview.jsFunctionArgsAsJson = viewModel.qrCodeData.value

                    val parentScope = this
                    coroutineScope.launch {
                        val url = Rownd.config.hubLoaderUrl()
                        parentScope.hubWebview.loadUrl(url)
                    }
                }
            }

            RowndButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(viewModel.key.value))
                    didCopyToClipboard.value = true
                },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            ) {
                Text(
                    text = if (didCopyToClipboard.value) "Copied!" else "Copy to clipboard",
                    modifier = Modifier.padding(end = 10.dp)
                )
                if (didCopyToClipboard.value) IconFilledCircleCheck() else IconCopy()
            }

            Text(
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 24.sp,
                text = "You can also copy your account's secret encryption key in case you need to recover it later. Be sure to store it in a safe, secure location."
            )
        }
    }
}