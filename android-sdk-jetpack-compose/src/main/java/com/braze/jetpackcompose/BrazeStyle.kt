package com.braze.jetpackcompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.braze.jetpackcompose.contentcards.styling.ContentCardListStyling
import com.braze.jetpackcompose.contentcards.styling.ContentCardStyling

val LocalContentCardListStyling = compositionLocalOf { ContentCardListStyling() }
val LocalContentCardStyling = compositionLocalOf { ContentCardStyling() }

@Suppress("LongParameterList")
@Composable
fun BrazeStyle(
    contentCardListStyle: ContentCardListStyling = ContentCardListStyling(),
    contentCardStyle: ContentCardStyling = ContentCardStyling(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalContentCardListStyling provides contentCardListStyle,
        LocalContentCardStyling provides contentCardStyle,
        content = content
    )
}
