package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import com.example.ui.components.ActiveCallOverlay
import com.example.ui.components.IncomingCallDialog
import com.example.ui.screens.chat.ChatConversationScreen
import com.example.ui.screens.chat.ChatListScreen
import com.example.ui.screens.contacts.ContactsScreen
import com.example.ui.screens.mesh.MeshRoomScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.transfer.FileTransferScreen
import com.example.ui.theme.PrimaryCyan
import com.example.ui.viewmodel.PeerViewModel

import com.example.ui.screens.splash.OpeningLoadingScreen

@Composable
fun MainScaffold(viewModel: PeerViewModel) {
    val context = LocalContext.current
    var showSplashScreen by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val selectedPeerId by viewModel.selectedPeerId.collectAsState()
    val activeCall by viewModel.activeCall.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Request runtime permissions for Audio, Camera, Notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showSplashScreen) {
            OpeningLoadingScreen(
                viewModel = viewModel,
                onContinueToApp = { showSplashScreen = false }
            )
        } else {
            Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("main_navigation_bar"),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.ChatBubble, contentDescription = "Messenger") },
                        label = { Text("Messenger") },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = PrimaryCyan
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.SwapHoriz, contentDescription = "Transfers") },
                        label = { Text("Files") },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = PrimaryCyan
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Hub, contentDescription = "Mesh Room") },
                        label = { Text("Mesh") },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = PrimaryCyan
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.People, contentDescription = "Contacts") },
                        label = { Text("Peers") },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = PrimaryCyan
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = PrimaryCyan
                        )
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTab) {
                    0 -> com.example.ui.screens.chat.MainMessagingScreen(viewModel = viewModel)
                    1 -> FileTransferScreen(viewModel = viewModel)
                    2 -> MeshRoomScreen(viewModel = viewModel)
                    3 -> ContactsScreen(
                        viewModel = viewModel,
                        onOpenChat = { peerId ->
                            viewModel.selectPeer(peerId)
                            selectedTab = 0
                        }
                    )
                    4 -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }

        // Incoming Call Modal
        activeCall?.let { call ->
            if (call.isIncoming && !call.isConnected) {
                IncomingCallDialog(
                    call = call,
                    onAnswer = { isVideo -> viewModel.answerCall(isVideo) },
                    onDecline = { viewModel.endCall() }
                )
            }
        }

        // Active In-Call Overlay (Video or Audio Call with WebRTC stream)
        activeCall?.let { call ->
            if (call.isConnected || !call.isIncoming) {
                ActiveCallOverlay(
                    call = call,
                    webView = viewModel.engineManager.getWebView(),
                    onMuteToggle = { viewModel.toggleAudio(!it) },
                    onVideoToggle = { viewModel.toggleVideo(it) },
                    onSwitchCamera = { viewModel.switchCamera() },
                    onEndCall = { viewModel.endCall() }
                )
            }
        }
    }
}
