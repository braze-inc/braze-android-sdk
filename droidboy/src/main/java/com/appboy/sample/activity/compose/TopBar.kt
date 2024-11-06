package com.appboy.sample.activity.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.appboy.sample.DroidboyApplication
import com.braze.enums.BrazeDateFormat
import com.braze.support.formatDate
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String = "",
    buttonIcon: ImageVector,
    onButtonClicked: () -> Unit
) {
    var shouldShowOverflow by remember { mutableStateOf(false) }
    val application = LocalContext.current.applicationContext as DroidboyApplication

    TopAppBar(
        title = {
            Text(
                text = title
            )
        },
        navigationIcon = {
            IconButton(onClick = { onButtonClicked() }) {
                Icon(buttonIcon, contentDescription = "")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        actions = {
            Box {
                IconButton(onClick = { shouldShowOverflow = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = shouldShowOverflow,
                    onDismissRequest = { shouldShowOverflow = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Change to random user ID") },
                        onClick = {
                            val newUserId = "droidboy-${Date().formatDate(BrazeDateFormat.SHORT)}-${(1000..9999).random()}"
                            application.changeUserWithNewSdkAuthToken(newUserId)
                        }
                    )
                }
            }
        }
    )
}
