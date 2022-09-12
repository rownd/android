package io.rownd.android.ui.theme

import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import io.rownd.android.R

@Composable
fun IconCopy() {
    Icon(
        painter = painterResource(R.drawable.ic_baseline_content_copy),
        contentDescription = "copy"
    )
}

@Composable
fun IconFilledCircleCheck() {
    Icon(
        painter = painterResource(R.drawable.ic_baseline_check_circle_24),
        contentDescription = "check"
    )
}