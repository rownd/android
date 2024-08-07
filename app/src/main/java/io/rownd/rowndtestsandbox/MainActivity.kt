package io.rownd.rowndtestsandbox

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.primarySurface
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.databinding.DataBindingUtil
import io.rownd.android.Rownd
import io.rownd.android.RowndConnectAuthenticatorHint
import io.rownd.android.RowndSignInHint
import io.rownd.android.RowndSignInOptions
import io.rownd.rowndtestsandbox.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val _encKey = MutableStateFlow("")

    private fun refreshTokenTest() {
        CoroutineScope(Dispatchers.Default).launch {
            val newToken = Rownd._refreshToken()
            Log.d("Token 1:", newToken ?: "null")
        }

        CoroutineScope(Dispatchers.Default).launch {
            val newToken = Rownd._refreshToken()
            Log.d("Token 2:", newToken ?: "null")
        }

        CoroutineScope(Dispatchers.Default).launch {
            val newToken = Rownd._refreshToken()
            Log.d("Token 3:", newToken ?: "null")
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.lifecycleOwner = this

        binding.rownd = Rownd

        val parentActivity = this

        val composeView = findViewById<ComposeView>(R.id.compose_view)
        composeView.setContent {
            val state = Rownd.state.collectAsState()
            val signInButtonText = if (state.value.auth.isAuthenticated) "Sign out" else "Sign in"

            val modalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
            val scope = rememberCoroutineScope()

            val encKeyState = _encKey.collectAsState(initial = "")
            ModalBottomSheetLayout(
                sheetContent = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dp(10F))
                    ) {
                        Text(
                            fontSize = 20.sp,
                                text = "Set encryption key"
                        )
                        TextField(
                            modifier = Modifier.padding(vertical = Dp(5F)).fillMaxWidth(),
                            value = encKeyState.value,
                            onValueChange = { _encKey.value = it },
                            label = { Text(text = "Enter key") }
                        )
                        Button(
                            modifier = Modifier.fillMaxWidth()
                                .padding(vertical = Dp(5F)),
                            onClick = {
//                                val keyId = Rownd.userRepo.getKeyId(state.value.user)
//                                Encryption.deleteKey(keyId)
//                                Encryption.storeKey(encKeyState.value, keyId)
                            }
                        ) {
                            Text("Save key")
                        }
                    }
                },
                sheetState = modalBottomSheetState,
            ) {
                ConstraintLayout(modifier = Modifier.fillMaxHeight()) {
                    val (column) = createRefs()

                    Surface(
                        color = MaterialTheme.colors.primarySurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .constrainAs(column) {
                                bottom.linkTo(parent.bottom)
                            }
                            .padding(Dp(5F))
                    ) {
                        Column {
                            Text("Initialized: ${state.value.isInitialized}")
                            Text("Valid access token: ${state.value.auth.isAccessTokenValid}")

                            if (state.value.auth.isAuthenticated) {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text((state.value.user.data["super_secret_data"] ?: "") as String)
                                }
//                Row(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Button(
                                        modifier = Modifier.padding(horizontal = Dp(5F)),
                                        onClick = {
                                            scope.launch {
                                                modalBottomSheetState.show()
                                            }
                                        }
                                    ) {
                                        Text("Set encryption key")
                                    }

                                    Button(
                                        modifier = Modifier.padding(horizontal = Dp(5F)),
                                        onClick = {
                                            Rownd.manageAccount()
                                        }
                                    ) {
                                        Text("Edit profile")
                                    }

                                    Button(
                                        modifier = Modifier.padding(horizontal = 5.dp),
                                        onClick = {
                                            refreshTokenTest()
                                        }
                                    ) {
                                        Text("Refresh token")
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Button(
                                        modifier = Modifier.padding(horizontal = 5.dp),
                                        onClick = {
                                            Rownd.connectAuthenticator(RowndConnectAuthenticatorHint.Passkey)
                                        }
                                    ) {
                                        Text("Register passkey")
                                    }
                                }
//                }
                            }
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (!state.value.auth.isAuthenticated && state.value.appConfig.config.hub.auth.signInMethods.google.enabled) {
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            Rownd.requestSignIn(RowndSignInHint.OneTap)
                                        }
                                    ) {
                                        Text("Show One Tap")
                                    }
                                }
                                if (!state.value.auth.isAuthenticated && state.value.appConfig.config.hub.auth.signInMethods.anonymous.enabled) {
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            Rownd.requestSignIn(RowndSignInHint.Guest)
                                        }
                                    ) {
                                        Text("Sign in as a guest")
                                    }
                                }
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        if (state.value.auth.isAuthenticated) Rownd.signOut()
                                        else Rownd.requestSignIn(RowndSignInOptions(
//                                            postSignInRedirect = "rowndtestapp://test"
                                        ))
                                    }
                                ) {
                                    Text(signInButtonText)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}