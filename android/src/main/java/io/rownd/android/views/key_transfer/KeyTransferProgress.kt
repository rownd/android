package io.rownd.android.views.key_transfer

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rownd.android.R

@Composable
internal fun KeyTransferProgress(
    hostController: KeyTransferBottomSheet,
    onNavBack: () -> Unit,
    viewModel: KeyTransferViewModel
) {
    Column() {
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
                text = "Signing in with your key",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }

}