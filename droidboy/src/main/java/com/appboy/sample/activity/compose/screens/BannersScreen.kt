package com.appboy.sample.activity.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.appboy.sample.activity.compose.TopBar
import com.appboy.sample.activity.compose.viewmodel.MainViewModel
import com.braze.jetpackcompose.banners.Banner
import kotlinx.coroutines.CoroutineScope

@Composable
@Suppress("UnusedPrivateMember")
fun BannersScreen(
    openDrawer: () -> Unit,
    viewModel: MainViewModel,
    scope: CoroutineScope? = null,
    snackbarHostState: SnackbarHostState? = null
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            title = "Banners",
            buttonIcon = Icons.Filled.Menu,
            onButtonClicked = { openDrawer() },
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Banner(placementId = "sdk-test-1")
        }
    }
}
