package io.rownd.rowndtestsandbox

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ProgressBar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Arrangement.SpaceAround
import androidx.compose.ui.platform.ComposeView
import io.rownd.android.Rownd
import androidx.compose.material.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.databinding.DataBindingUtil
import io.rownd.android.RowndSignInOptions
import io.rownd.android.models.repos.UserRepo
import io.rownd.android.util.Encryption
import io.rownd.rowndtestsandbox.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val _encKey = MutableStateFlow("")

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.lifecycleOwner = this

        binding.rownd = Rownd

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
                                val keyId = UserRepo.getKeyId(state.value.user)
                                Encryption.deleteKey(keyId)
                                Encryption.storeKey(encKeyState.value, keyId)
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
                            Button(
                                modifier = Modifier.padding(horizontal = Dp(5F)),
                                onClick = {
                                    Rownd.transferEncryptionKey()
                                }
                            ) {
                                Text("Transfer key")
                            }

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

                                        }
                                    ) {
                                        Text("Edit profile")
                                    }
                                }
//                }
                            }
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        if (state.value.auth.isAuthenticated) Rownd.signOut()
                                        else Rownd.requestSignIn(RowndSignInOptions(
                                            postSignInRedirect = "rowndtestapp://test"
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