package com.braze.jetpackcompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.braze.jetpackcompose.contentcards.styling.ContentCardListStyling
import com.braze.jetpackcompose.contentcards.styling.ContentCardStyling

/** [CompositionLocal][compositionLocalOf] providing the current [ContentCardListStyling] for Content Card list rendering. */
val LocalContentCardListStyling = compositionLocalOf { ContentCardListStyling() }

/** [CompositionLocal][compositionLocalOf] providing the current [ContentCardStyling] for individual Content Card rendering. */
val LocalContentCardStyling = compositionLocalOf { ContentCardStyling() }

/**
 * Provides Braze styling values to the composition tree via [CompositionLocalProvider].
 * Wrap your Braze composables in [BrazeStyle] to apply custom styling.
 *
 * @param contentCardListStyle Styling for the Content Card list container.
 * @param contentCardStyle Styling for individual Content Cards.
 * @param content The composable content that will receive the provided styles.
 */
@Suppress("LongParameterList")
@Composable
fun BrazeStyle(
    contentCardListStyle: ContentCardListStyling = ContentCardListStyling(),
    contentCardStyle: ContentCardStyling = ContentCardStyling(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalContentCardListStyling provides contentCardListStyle,
        LocalContentCardStyling provides contentCardStyle,
        content = content,
    )
}
