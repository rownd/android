package io.rownd.rowndtestsandbox.app_instant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.rownd.android.Rownd
import io.rownd.android.models.domain.AuthState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AuthUI(
                authState = Rownd.state.collectAsState().value.auth,
                onSignInOut = {
                    if (Rownd.state.value.auth.isAuthenticated) {
                        Rownd.signOut()
                    } else {
                        Rownd.requestSignIn()
                    }
                },
                onManageAccount = {
                    Rownd.manageAccount()
                }
            )
        }
    }
}

@Composable
fun AuthUI(
    authState: AuthState,
    onSignInOut: () -> Unit,
    onManageAccount: () -> Unit
) {
    val state = Rownd.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (state.value.auth.isAuthenticated) {
            Text(text = "Welcome, ${state.value.user.data.get("email") ?: "User"}!")
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Text(text = "Please sign in.")
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(onClick = onSignInOut) {
            Text(text = if (authState.isAuthenticated) "Sign Out" else "Sign In")
        }

        if (state.value.auth.isAuthenticated) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onManageAccount) {
                Text(text = "Manage Account")
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    Rownd._refreshToken()
                }
            }) {
                Text(text = "Trigger token refresh")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AuthUIPreview() {
    val authState = remember { AuthState() }
    AuthUI(
        authState = authState,
        onSignInOut = { /* TODO: Implement sign in/out logic */ },
        onManageAccount = { /* TODO: Implement manage account navigation */ }
    )
}