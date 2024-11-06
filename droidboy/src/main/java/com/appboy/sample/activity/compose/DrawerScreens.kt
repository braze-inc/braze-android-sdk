package com.appboy.sample.activity.compose

sealed class DrawerScreens(val title: String, val route: String) {
    object ContentCardsScreen : DrawerScreens("Content Cards", "contentcards")
    object BannersScreen : DrawerScreens("Banners", "banners")
    object JavascriptBridgeScreen : DrawerScreens("JavascriptBridge", "javascriptbridge")
}
