package com.braze.jetpackcompose.contentcards.styling

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.braze.ui.R

@Suppress("LongParameterList")
open class ContentCardListStyling(
    val modifier: Modifier = Modifier.fillMaxSize(),
    val spacerSize: Dp = 32.dp,
    val listPadding: Dp = 32.dp,
    val listBackgroundColor: Color = Color.Unspecified,
    val emptyTextStyle: TextStyle? = null
) {
    @Composable
    fun listBackgroundColor(): Color {
        val color = listBackgroundColor
        if (color == Color.Unspecified) {
            return colorResource(id = R.color.com_braze_content_cards_display_background_color)
        }
        return color
    }
}
