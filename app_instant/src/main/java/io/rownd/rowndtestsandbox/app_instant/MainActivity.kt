package io.rownd.rowndtestsandbox.app_instant

//import io.rownd.android.Rownd
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.rownd.android.Rownd
import io.rownd.rowndtestsandbox.app_instant.ui.theme.RowndTestSandboxTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
//        Debug.waitForDebugger()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state = Rownd.state.collectAsState()
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

            RowndTestSandboxTheme {
                Scaffold(
                    topBar = {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF6A11CB), Color(0xFF2575FC))
                                    )
                                )
                        ) {
                            LargeTopAppBar(
                                title = { Text("Awesome App", color = Color.White) },
                                scrollBehavior = scrollBehavior,
                                colors = TopAppBarDefaults.largeTopAppBarColors(
                                    containerColor = Color.Transparent,
                                    titleContentColor = Color.White
                                )
                            )
                        }
                    },
                    containerColor = Color(0xFFF0F0F3)
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
//                                .fillMaxSize()
//                                .padding(padding)
//                                .padding(horizontal = 16.dp, vertical = 24.dp)
                                .align(Alignment.BottomCenter)
                        ) {
                            if (!state.value.auth.isAuthenticated) {
                                SignInButton(
                                    text = "Sign in",
                                    icon = Icons.Default.AccountCircle,
                                    onClick = {
                                        Rownd.requestSignIn()
                                    }
                                )
                            } else {
                                SignInButton(
                                    text = "Sign out",
                                    icon = Icons.Default.AccountCircle,
                                    onClick = {
                                        Rownd.signOut()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            }
        }
    }

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RowndTestSandboxTheme {
        Greeting("Android")
    }
}

@Composable
fun SignInButton(
    text: String = "Sign In",
    icon: ImageVector = Icons.Default.Person,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF6A11CB), Color(0xFF2575FC))
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
