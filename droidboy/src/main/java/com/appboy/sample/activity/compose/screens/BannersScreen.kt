package com.appboy.sample.activity.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Placement + slot selection (1–5) is on the Banners tab. Example preview:",
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Banner(placementId = "sdk-test-1")
        }
    }
}
