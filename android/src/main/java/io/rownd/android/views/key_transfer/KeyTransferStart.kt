package io.rownd.android.views.key_transfer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.rownd.android.Rownd
import io.rownd.android.models.network.SignInLinkApi
import io.rownd.android.models.repos.UserRepo
import io.rownd.android.ui.theme.RowndButton
import io.rownd.android.ui.theme.RowndTheme
import io.rownd.android.util.Encryption
import io.rownd.android.util.asBase64String
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put


internal class KeyTransferViewModel : ViewModel() {
    var key = mutableStateOf("Loading...")
    var signInLink = mutableStateOf("")
    val isReceivingKey = mutableStateOf(true)
    var operationError = mutableStateOf("")

    val qrCodeData = derivedStateOf {
        val jsonObj = buildJsonObject {
            put("data", "${signInLink.value}#${key.value}")
        }

        jsonObj.toString()
    }

    fun setupKeyTransfer() {
        val keyId = UserRepo.getKeyId(Rownd.state.value.user)
        val key = Encryption.loadKey(keyId)
        this.key.value = key?.asBase64String ?: "Error"

        // Fetch sign-in link
        val parent = this
        CoroutineScope(Dispatchers.IO).launch {
            val signInLink = SignInLinkApi.client.createSignInLink()

            if (!signInLink.isSuccessful) {
                parent.operationError.value = "Failed to fetch sign-in link"
                isReceivingKey.value = false
            } else {
                val signInLinkBody = signInLink.body()
                parent.signInLink.value = signInLinkBody?.link ?: ""
                isReceivingKey.value = false
            }
        }
    }
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
    val state = Rownd.state.collectAsState()

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
        if (state.value.auth.isAuthenticated) {
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
