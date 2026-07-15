package com.appboy.sample.activity.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.appboy.sample.R
import com.appboy.sample.activity.compose.TopBar
import com.appboy.sample.activity.compose.viewmodel.MainViewModel
import com.braze.jetpackcompose.banners.Banner
import kotlinx.coroutines.CoroutineScope

@Composable
@Suppress("UnusedPrivateMember")
fun JavascriptBridgeScreen(
    openDrawer: () -> Unit,
    viewModel: MainViewModel,
    scope: CoroutineScope? = null,
    snackbarHostState: SnackbarHostState? = null,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            title = " Banners - JavaScript Bridge",
            buttonIcon = ImageVector.vectorResource(R.drawable.ic_menu),
            onButtonClicked = { openDrawer() },
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            Banner(placementId = "custom_html")
        }
    }
}
