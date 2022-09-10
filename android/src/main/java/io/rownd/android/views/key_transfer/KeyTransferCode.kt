package io.rownd.android.views.key_transfer

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import io.rownd.android.R
import io.rownd.android.Rownd
import io.rownd.android.databinding.HubViewLayoutBinding
import io.rownd.android.ui.theme.IconCopy
import io.rownd.android.ui.theme.IconFilledCircleCheck
import io.rownd.android.ui.theme.RowndButton
import io.rownd.android.views.HubPageSelector

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [KeyTransferCode.newInstance] factory method to
 * create an instance of this fragment.
 */
//class KeyTransferCode : Fragment() {
//    // TODO: Rename and change types of parameters
//    private var param1: String? = null
//    private var param2: String? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        arguments?.let {
//            param1 = it.getString(ARG_PARAM1)
//            param2 = it.getString(ARG_PARAM2)
//        }
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_key_transfer_code, container, false)
//    }
//
//    companion object {
//        /**
//         * Use this factory method to create a new instance of
//         * this fragment using the provided parameters.
//         *
//         * @param param1 Parameter 1.
//         * @param param2 Parameter 2.
//         * @return A new instance of fragment KeyTransferCode.
//         */
//        // TODO: Rename and change types and number of parameters
//        @JvmStatic
//        fun newInstance(param1: String, param2: String) =
//            KeyTransferCode().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
//                }
//            }
//    }
//}

@Composable
internal fun KeyTransferCode(
    onNavBack: () -> Unit,
    viewModel: KeyTransferViewModel
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val didCopyToClipboard = remember { mutableStateOf(false) }
    val didInit = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!didInit.value) {
            viewModel.setupKeyTransfer()
            didInit.value = true
        }
    }

    Column() {
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                lineHeight = 24.sp,
                text = "To sign in on another device, scan the QR code below with the new device."
            )

            // QRCodeWebView
            AndroidViewBinding(HubViewLayoutBinding::inflate) {
                val url = Rownd.config.hubLoaderUrl()
                this.hubWebview.layoutParams.height = 700
                this.hubWebview.progressBar = this.hubProgressBar
                this.hubWebview.targetPage = HubPageSelector.QrCode
                this.hubWebview.jsFunctionArgsAsJson = viewModel.qrCodeData.value
                this.hubWebview.loadUrl(url)
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