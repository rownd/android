package io.rownd.android.views.key_transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import io.rownd.android.R
import io.rownd.android.views.*

class KeyTransferBottomSheet : BottomSheet() {

    override var layoutId = R.layout.key_transfer_layout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val bundle = this.arguments

        val view = super.onCreateView(inflater, container, savedInstanceState) ?: throw Exception("Failed to create view")

        val composeView = view.findViewById<ComposeView>(R.id.key_transfer_start_compose_view)

        composeView.setContent {
            KeyTransferNavHost(hostController = this)
        }

        return view
    }

    companion object {
        fun newInstance(): KeyTransferBottomSheet {
            val bundle = Bundle()

            val bottomSheet = KeyTransferBottomSheet()
            bottomSheet.arguments = bundle

            return bottomSheet
        }
    }
}