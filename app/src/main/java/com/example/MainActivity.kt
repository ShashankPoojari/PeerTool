package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.screens.MainScaffold
import com.example.ui.theme.PeerFlowTheme
import com.example.ui.viewmodel.PeerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PeerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PeerFlowTheme(darkTheme = true) {
                MainScaffold(viewModel = viewModel)
            }
        }
    }
}
