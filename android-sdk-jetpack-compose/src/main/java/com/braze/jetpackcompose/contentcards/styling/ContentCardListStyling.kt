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

/**
 * Styling configuration for the Content Card list container.
 *
 * @property modifier The [Modifier] applied to the list container.
 * @property spacerSize Vertical spacing between Content Cards.
 * @property listPadding Vertical padding at the top and bottom of the list.
 * @property listBackgroundColor Background color of the list. Falls back to the Braze default if [Color.Unspecified].
 * @property emptyTextStyle [TextStyle] for the empty state message, or null to use the default.
 */
@Suppress("LongParameterList")
open class ContentCardListStyling(
    val modifier: Modifier = Modifier.fillMaxSize(),
    val spacerSize: Dp = 32.dp,
    val listPadding: Dp = 32.dp,
    val listBackgroundColor: Color = Color.Unspecified,
    val emptyTextStyle: TextStyle? = null,
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
