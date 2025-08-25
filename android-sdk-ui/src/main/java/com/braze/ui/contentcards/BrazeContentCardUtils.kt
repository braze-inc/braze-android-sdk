package com.braze.ui.contentcards

import android.content.Context
import android.os.Bundle
import com.braze.enums.Channel
import com.braze.models.cards.Card
import com.braze.support.BrazeLogger.Priority.V
import com.braze.support.BrazeLogger.brazelog
import com.braze.ui.BrazeDeeplinkHandler
import com.braze.ui.actions.UriAction
import com.braze.ui.actions.brazeactions.containsInvalidBrazeAction

object BrazeContentCardUtils {
    /**
     * Sort cards by pinned, then newer, then card ID. Will also removed
     * cards with invalid BrazeActions
     *
     * @param cards Cards to sort
     * @return List of cards sorted and filtered
     */
    fun defaultCardHandling(cards: List<Card>): List<Card> {
        // Sort by pinned, then by the 'updated' timestamp descending
        // Pinned before non-pinned
        val cardComparator = Comparator { cardA: Card, cardB: Card ->
            when {
                // A displays above B since A is pinned and B isn't
                cardA.isPinned && !cardB.isPinned -> -1
                // B displays above A since B is pinned and A isn't
                !cardA.isPinned && cardB.isPinned -> 1
                // At this point, both A & B are pinned or both A & B are non-pinned
                // A displays above B if A is newer
                cardA.created > cardB.created -> -1
                // B displays above A if B is newer
                cardA.created < cardB.created -> 1
                // Last chance with the card IDs
                cardA.id > cardB.id -> -1
                cardA.id < cardB.id -> 1
                // They're considered equal at this point (although ID's should never match)
                else -> 0
            }
        }
        return cards.filter { card -> !card.containsInvalidBrazeAction() }
            .sortedWith(cardComparator)
    }

    fun getUriActionForCard(card: Card): UriAction? {
        val url = card.url
        if (url == null) {
            brazelog(V) { "Card URL is null, returning null for getUriActionForCard" }
            return null
        }
        val extras = Bundle()
        for (key in card.extras.keys) {
            extras.putString(key, card.extras[key])
        }
        return BrazeDeeplinkHandler.getInstance().createUriActionFromUrlString(
            url,
            extras,
            card.openUriInWebView,
            Channel.CONTENT_CARD
        )
    }

    /**
     * This is used by Braze code to handle clicks. Clients should not call this directly.
     */
    fun handleCardClick(context: Context, card: Card, clickHandler: ((Card) -> Boolean)?) {
        brazelog(V) { "Handling card click for card: $card" }
        card.isIndicatorHighlighted = true

        var isCustomHandled = false
        if (clickHandler != null) {
            brazelog { "Calling custom card click handler" }
            isCustomHandled = clickHandler.invoke(card)
        }

        if (!isCustomHandled) {
            val cardAction = getUriActionForCard(card)
            if (cardAction != null) {
                card.logClick()
                brazelog(V) { "Card action is non-null. Attempting to perform action on card: ${card.id}" }
                BrazeDeeplinkHandler.getInstance().gotoUri(context, cardAction)
            } else {
                brazelog(V) { "Card action is null. Not performing any click action on card: ${card.id}" }
            }
        } else {
            brazelog { "Card click was handled by custom listener on card: ${card.id}" }
            card.logClick()
        }
    }
}
