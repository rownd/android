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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.fragment.app.Fragment
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import io.rownd.android.R
import io.rownd.android.ui.theme.RowndButton
import io.rownd.android.ui.theme.RowndTheme

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [KeyTransferStart.newInstance] factory method to
 * create an instance of this fragment.
 */
//class KeyTransferStart : Fragment() {
//    // TODO: Rename and change types of parameters
//    private var param1: String? = null
//    private var param2: String? = null
//    private var navController: NavHostController? = null
//
//    override fun onAttach(context: Context) {
//        super.onAttach(context)
//        val callback: OnBackPressedCallback = object : OnBackPressedCallback(
//            true // default to enabled
//        ) {
//            override fun handleOnBackPressed() {
////                navController?.setOnBackPressedDispatcher()
//                // TODO: This doesn't work currently. Need to capture event prior to dismiss
//                navController?.enableOnBackPressed(true)
//            }
//        }
//
//        requireActivity().onBackPressedDispatcher.addCallback(
//            this,
//            callback
//        )
//    }
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
//
//        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_key_transfer_start, container, false).apply {
//            val composeView = findViewById<ComposeView>(R.id.key_transfer_start_compose_view)
//
//            composeView.setContent {
//                KeyTransferNavHost()
//            }
//        }
//    }
//
//    companion object {
//        /**
//         * Use this factory method to create a new instance of
//         * this fragment using the provided parameters.
//         *
//         * @param param1 Parameter 1.
//         * @param param2 Parameter 2.
//         * @return A new instance of fragment KeyTransferStart.
//         */
//        // TODO: Rename and change types and number of parameters
//        @JvmStatic
//        fun newInstance(param1: String, param2: String) =
//            KeyTransferStart().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
//                }
//            }
//    }
//}

@Composable
fun KeyTransferNavHost(
    navController: NavHostController = rememberNavController(),
    navStartPage: String = "key_transfer_start"
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
                        onNavBack = { backFn() }
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
