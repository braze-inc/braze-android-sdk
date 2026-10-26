package com.braze.jetpackcompose.contentcards

import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Fraction of the card width the user must swipe before the card is dismissed. */
private const val DISMISS_THRESHOLD_FRACTION = 0.5f

/**
 * Wraps [content] so the user can dismiss it by swiping horizontally in either direction.
 *
 * The card is dismissed once the swipe passes [DISMISS_THRESHOLD_FRACTION] of the card width.
 *
 * @param onDismissed Invoked once the card has settled in a dismissed position.
 * @param modifier The [Modifier] applied to the swipeable container.
 * @param content The card to render inside the swipeable container.
 */
@Composable
internal fun DismissibleContentCard(
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dismissState =
        rememberSwipeToDismissBoxState(
            positionalThreshold = { totalDistance -> totalDistance * DISMISS_THRESHOLD_FRACTION },
        )
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {},
        onDismiss = { onDismissed() },
    ) {
        content()
    }
}
