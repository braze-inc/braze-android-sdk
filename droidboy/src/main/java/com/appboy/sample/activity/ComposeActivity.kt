package com.appboy.sample.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.appboy.sample.activity.compose.DrawerScreens
import com.appboy.sample.activity.compose.screens.BannersScreen
import com.appboy.sample.activity.compose.screens.ContentCardsScreen
import com.appboy.sample.activity.compose.screens.JavascriptBridgeScreen
import com.appboy.sample.activity.compose.viewmodel.MainViewModel
import com.braze.enums.CardType
import com.braze.jetpackcompose.BrazeStyle
import com.braze.jetpackcompose.contentcards.styling.BrazeTextAnnouncementContentCardStyling
import com.braze.jetpackcompose.contentcards.styling.ContentCardStyling
import com.braze.models.cards.Card
import com.braze.models.cards.TextAnnouncementCard
import kotlinx.coroutines.launch

class ComposeActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val screens = listOf(
        DrawerScreens.ContentCardsScreen,
        DrawerScreens.BannersScreen,
        DrawerScreens.JavascriptBridgeScreen
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Surface(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars), color = MaterialTheme.colorScheme.background) {
                val navController = rememberNavController()

                val snackbarHostState = remember { SnackbarHostState() }

                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val isDrawerOpen by viewModel.drawerShouldBeOpened
                    .collectAsStateWithLifecycle()

                if (isDrawerOpen) {
                    // Open drawer and reset state in VM.
                    LaunchedEffect(Unit) {
                        // wrap in try-finally to handle interruption whiles opening drawer
                        try {
                            viewModel.openDrawer()
                        } finally {
                            viewModel.resetOpenDrawerAction()
                        }
                    }
                }

                // Intercepts back navigation when the drawer is open
                val scope = rememberCoroutineScope()
                if (drawerState.isOpen) {
                    BackHandler {
                        scope.launch {
                            drawerState.close()
                        }
                    }
                }

                Scaffold(snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState)
                }) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        ModalNavigationDrawer(
                            drawerState = drawerState,
                            drawerContent = {
                                ModalDrawerSheet {
                                    Text(
                                        "Droidboy",
                                        fontSize = 24.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.primary)
                                            .padding(16.dp)
                                    )

                                    screens.forEach { screen ->
                                        HorizontalDivider()
                                        NavigationDrawerItem(
                                            label = { Text(text = screen.title) },
                                            shape = MaterialTheme.shapes.extraSmall,
                                            selected = navController.currentDestination?.route == screen.route,
                                            onClick = {
                                                scope.launch {
                                                    drawerState.close()
                                                }
                                                navController.navigate(screen.route) {
                                                    // Pop up to the start destination of the graph to
                                                    // avoid building up a large stack of destinations
                                                    // on the back stack as users select items
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    // Avoid multiple copies of the same destination when
                                                    // reselecting the same item
                                                    launchSingleTop = true
                                                    // Restore state when reselecting a previously selected item
                                                    restoreState = true
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = DrawerScreens.ContentCardsScreen.route
                            ) {
                                composable(DrawerScreens.ContentCardsScreen.route) {
                                    ContentCardsScreen(
                                        openDrawer = {
                                            scope.launch {
                                                drawerState.open()
                                            }
                                        },
                                        viewModel,
                                        scope,
                                        snackbarHostState
                                    )
                                }
                                composable(DrawerScreens.BannersScreen.route) {
                                    BannersScreen(
                                        openDrawer = {
                                            scope.launch {
                                                drawerState.open()
                                            }
                                        },
                                        viewModel,
                                        scope,
                                        snackbarHostState
                                    )
                                }
                                composable(DrawerScreens.JavascriptBridgeScreen.route) {
                                    JavascriptBridgeScreen(
                                        openDrawer = {
                                            scope.launch {
                                                drawerState.open()
                                            }
                                        },
                                        viewModel,
                                        scope,
                                        snackbarHostState
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

val myCustomCardRenderer: @Composable ((Card) -> Boolean) = { card ->
    if (card.cardType == CardType.TEXT_ANNOUNCEMENT) {
        val textCard = card as TextAnnouncementCard
        Box(
            Modifier
                .padding(10.dp)
                .fillMaxWidth()
                .background(color = Color.Red)
        ) {
            Text(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .basicMarquee(iterations = Int.MAX_VALUE),
                fontSize = 35.sp,
                text = textCard.description
            )
        }
        true
    } else {
        false
    }
}

@Composable
@Suppress("UnusedPrivateMember")
private fun GetMyBrazeStyle(content: @Composable () -> Unit) {
    BrazeStyle(
        contentCardStyle = ContentCardStyling(
            cardBackgroundColor = Color.Blue,
            textAnnouncementContentCardStyle = BrazeTextAnnouncementContentCardStyling(
                cardBackgroundColor = Color.Red
            ),
        ),
        content = content
    )
}

@Composable
@Suppress("UnusedPrivateMember")
private fun GetMyOtherBrazeStyle(content: @Composable () -> Unit) {
    BrazeStyle(
        contentCardStyle = ContentCardStyling(
            pinnedComposable = {
                Box(Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(50.dp)
                            .basicMarquee(iterations = Int.MAX_VALUE),
                        text = "This message is not read. Please read it."
                    )
                }
            },
            cardBackgroundColor = Color.Red
        ),
        content = content
    )
}
