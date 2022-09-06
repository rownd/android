package io.rownd.android.views.key_transfer

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import io.rownd.android.R
import io.rownd.android.ui.theme.RowndButton
import io.rownd.android.ui.theme.RowndTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class KeyTransferState(
    val key: String = "Loading...",
    val signInLink: String = "",
    val isReceivingKey: Boolean = false,
    val operationError: String? = null
) {
    internal fun qrCodeData(): String {
        val jsonObj = buildJsonObject {
            put("data", "${signInLink}#${key}")
        }

        return jsonObj.toString()
    }
}

internal class KeyTransferViewModel : ViewModel() {
    internal var keyState by mutableStateOf(KeyTransferState())
        private set
}

@Composable
internal fun KeyTransferNavHost(
    navController: NavHostController = rememberNavController(),
    navStartPage: String = "key_transfer_start",
    viewModel: KeyTransferViewModel = KeyTransferViewModel()
) {
    RowndTheme {
        Surface {
            NavHost(
                navController = navController,
                startDestination = navStartPage,
            ) {
                val backFn = { navController.popBackStack() }
                composable("key_transfer_start") {
                    KeyTransferStartContent(
                        onNavToShowCode = { navController.navigate("key_transfer_code")},
                        onNavToShowScanner = { navController.navigate("key_transfer_scanner") }
                    )
                }

                composable("key_transfer_scanner") {
                    KeyTransferScanner(
                        onNavBack = { backFn() }
                    )
                }

                composable("key_transfer_code") {
                    KeyTransferCode(
                        onNavBack = { backFn() },
                        viewModel = viewModel
                    )
                }

                composable("key_transfer_progress") {
                    KeyTransferProgress(
                        onNavBack = { backFn() }
                    )
                }
            }
        }
    }

    BackHandler {
        navController.popBackStack()
    }
}


@Composable
fun KeyTransferStartContent(
    onNavToShowCode: () -> Unit,
    onNavToShowScanner: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 10.dp)
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = "Encryption key",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
        Text(
            lineHeight = 24.sp,
            text = "To view your key or transfer your encryption key to another device, tap below."
        )
        RowndButton(
            onClick = {
                onNavToShowCode()
            },
            modifier = Modifier
                .padding(horizontal = 0.dp, vertical = 10.dp)
                .fillMaxWidth()
        ) {
            Text(text = "Show encryption key")
        }

        Text(
            modifier = Modifier.padding(top = 10.dp),
            lineHeight = 24.sp,
            text = "To sign in to your account using another device, scan the encryption key QR code that's displayed on the other device."
        )
        RowndButton(
            onClick = {
                onNavToShowScanner()
            },
            modifier = Modifier
                .padding(horizontal = 0.dp, vertical = 10.dp)
                .fillMaxWidth()
        ) {
            Text(text = "Scan QR code")
        }
    }
}
