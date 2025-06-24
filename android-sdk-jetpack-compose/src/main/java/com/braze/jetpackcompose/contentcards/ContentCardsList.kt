package com.braze.jetpackcompose.contentcards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.braze.Braze
import com.braze.coroutine.BrazeCoroutineScope
import com.braze.enums.CardType
import com.braze.events.ContentCardsUpdatedEvent
import com.braze.events.IEventSubscriber
import com.braze.events.SdkDataWipeEvent
import com.braze.jetpackcompose.LocalContentCardListStyling
import com.braze.jetpackcompose.LocalContentCardStyling
import com.braze.jetpackcompose.contentcards.cards.ContentCard
import com.braze.jetpackcompose.contentcards.styling.ContentCardListStyling
import com.braze.jetpackcompose.contentcards.styling.ContentCardStyling
import com.braze.models.cards.Card
import com.braze.support.BrazeLogger.Priority.V
import com.braze.support.BrazeLogger.Priority.W
import com.braze.support.BrazeLogger.brazelog
import com.braze.ui.contentcards.BrazeContentCardUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Suppress("LongMethod", "ComplexMethod", "LongParameterList", "VariableNaming", "MagicNumber", "NestedBlockDepth")
@OptIn(ExperimentalMaterialApi::class)
@Composable
/**
 * BrazeContentCardsList displays a list of Content Cards.
 *
 * @param cards List of [Card] to render. If null, all content cards will be retrieved and displayed.
 * @param emptyComposable The composable to render if there are no content cards.
 * @param emptyString The string to display when there are no content cards. Will not display if [emptyComposable] is present.
 * @param cardUpdateHandler The function to filter and sort the list of cards. If null, all cards will be passed with a default sort.
 * @param onCardClicked Will be called when a card is clicked. Returns `false` if Braze should handle the click.
 * @param onCardDismissed Will be called when a card is dismissed.
 * @param customCardComposer A function that can take a card and then either compose it and return `true`,
 *                           or return `false` and have the card rendered by default.
 *                           If you use this, you are responsible for handling all aspects of card rendering (unread, pinned, etc.).
 *                           You also need to handle card clicks (See `BrazeContentCardUtils.handleCardClick`).
 * @param style The styling for the list of content cards.
 * @param cardStyle The styling for the individual content cards.
 * @param enablePullToRefresh If true, the user can pull down to refresh the list of content cards.
 */
fun ContentCardsList(
    cards: List<Card>? = null,
    emptyComposable: @Composable (() -> Unit)? = null,
    emptyString: String? = null,
    cardUpdateHandler: ((List<Card>) -> List<Card>)? = null,
    onCardClicked: ((Card) -> Boolean)? = null,
    onCardDismissed: ((Card) -> Unit)? = null,
    customCardComposer: (@Composable ((Card) -> Boolean))? = null,
    style: ContentCardListStyling = LocalContentCardListStyling.current,
    cardStyle: ContentCardStyling = LocalContentCardStyling.current,
    enablePullToRefresh: Boolean = true,
) {
    val context = LocalContext.current
    var contentCardsUpdatedSubscriber: IEventSubscriber<ContentCardsUpdatedEvent>? = null
    var sdkDataWipeEventSubscriber: IEventSubscriber<SdkDataWipeEvent>? = null

    val myCards = remember {
        mutableStateListOf<Card>()
    }

    val controlCardInference = remember {
        mutableStateListOf<Pair<String, Card>>()
    }

    var didInitialLoad by remember { mutableStateOf(false) }

    val refreshScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    var networkUnavailableJob: Job? = null

    val TAG = "BrazeContentCardList"
    brazelog(TAG) { "Doing compose of BrazeContentCardsList" }

    val NETWORK_PROBLEM_WARNING_MS = 5000L
    val AUTO_HIDE_REFRESH_INDICATOR_DELAY_MS = 2500L

    fun refresh() = refreshScope.launch {
        isRefreshing = true
        Braze.getInstance(context).requestContentCardsRefresh()
        delay(AUTO_HIDE_REFRESH_INDICATOR_DELAY_MS)
        isRefreshing = false
    }

    val refreshState = rememberPullRefreshState(isRefreshing, ::refresh)

    val impressedCards = remember {
        mutableStateListOf<String>()
    }

    fun networkUnavailable() {
        brazelog(TAG) { "Network is unavailable." }
        networkUnavailableJob = null
        isRefreshing = false
    }

    @Composable
    fun renderCard(card: Card) {
        var isCustomRendered = false
        if (customCardComposer != null) {
            isCustomRendered = customCardComposer.invoke(card)
            brazelog(TAG, V) { "Attempted to render custom card ${card.id} and received a result of $isCustomRendered" }
        }

        if (!isCustomRendered) {
            // If this is unread, then monitor when it scrolls off the screen to mark it as read
            if (!card.isIndicatorHighlighted) {
                DisposableEffect(key1 = card.id) {
                    onDispose {
                        if (impressedCards.contains(card.id)) {
                            if (!card.isIndicatorHighlighted) {
                                card.isIndicatorHighlighted = true
                            }
                            brazelog(TAG, V) { "DisposableEffect Card ${card.id} left view" }
                        } else {
                            brazelog(TAG, V) { "DisposableEffect Card left view, but was never actually seen. Not marking as viewed." }
                        }
                    }
                }
            }
            ContentCard(card = card, clickHandler = onCardClicked, style = cardStyle)
        }
    }

    fun replaceCards(newCards: List<Card>) {
        val processedCards = cardUpdateHandler?.invoke(newCards) ?: BrazeContentCardUtils.defaultCardHandling(newCards)
        val cardsToAdd = mutableListOf<Card>()
        var lastCardId = ""
        val cardIDs = mutableSetOf<String>()
        for (card in processedCards) {
            if (card.isControl) {
                val idCardPair = Pair(lastCardId, card)
                if (lastCardId.isBlank()) {
                    brazelog(TAG) { "Control card $card.id is at the front. Logging impression immediately" }
                    card.logImpression()
                } else if (!controlCardInference.contains(idCardPair)) {
                    // This control card will log impression when the last "real" card has its impression logged.
                    controlCardInference.add(idCardPair)
                }
            } else if (cardIDs.contains(card.id)) {
                brazelog(TAG, W) { "Card ID ${card.id} already exists. Skipping card $card." }
            } else {
                cardIDs.add(card.id)
                cardsToAdd.add(card)
                lastCardId = card.id
            }
        }
        myCards.clear()
        myCards.addAll(cardsToAdd)
    }

    fun handleContentCardsUpdatedEvent(event: ContentCardsUpdatedEvent) {
        brazelog(TAG) { "Handling ContentCardsUpdatedEvent" }
        networkUnavailableJob?.cancel()
        networkUnavailableJob = null
        replaceCards(event.allCards)
        isRefreshing = false
    }

    fun logCardImpression(card: Card) {
        if (impressedCards.contains(card.id)) {
            brazelog(TAG) { "Card ${card.id} already logged. Skipping." }
        } else {
            brazelog(TAG) { "Logging impression for card ${card.id}" }
            impressedCards.add(card.id)
            card.logImpression()
        }
        controlCardInference.filter { it.first == card.id }.forEach {
            val controlCard = it.second
            if (impressedCards.contains(controlCard.id)) {
                brazelog(TAG) { "Control Card ${controlCard.id} (via ${card.id}) already logged. Skipping." }
            } else {
                brazelog(TAG) { "Logging impression for control card ${controlCard.id} (via ${card.id})" }
                impressedCards.add(controlCard.id)
                controlCard.logImpression()
            }
        }
    }

    if (cards == null) {
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(LocalLifecycleOwner.current) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        brazelog { "OnPause called in BrazeContentCardList" }
                        Braze.getInstance(context).removeSingleSubscription(
                            contentCardsUpdatedSubscriber,
                            ContentCardsUpdatedEvent::class.java
                        )
                        Braze.getInstance(context)
                            .removeSingleSubscription(sdkDataWipeEventSubscriber, SdkDataWipeEvent::class.java)
                        networkUnavailableJob?.cancel()
                        networkUnavailableJob = null
                    }

                    Lifecycle.Event.ON_RESUME -> {
                        brazelog { "OnResume called in BrazeContentCardList" }
                        // Remove the previous subscriber before rebuilding a new one with our new activity.
                        Braze.getInstance(context).removeSingleSubscription(
                            contentCardsUpdatedSubscriber,
                            ContentCardsUpdatedEvent::class.java
                        )
                        if (contentCardsUpdatedSubscriber == null) {
                            contentCardsUpdatedSubscriber = IEventSubscriber {
                                handleContentCardsUpdatedEvent(it)
                            }
                        }
                        contentCardsUpdatedSubscriber?.let {
                            Braze.getInstance(context)
                                .addSingleSynchronousSubscription(it, ContentCardsUpdatedEvent::class.java)
                        }

                        if (sdkDataWipeEventSubscriber == null) {
                            // If the SDK data is wiped, then we want to clear any cached Content Cards
                            sdkDataWipeEventSubscriber = IEventSubscriber {
                                handleContentCardsUpdatedEvent(ContentCardsUpdatedEvent.emptyUpdate)
                            }
                        }
                        sdkDataWipeEventSubscriber?.let {
                            Braze.getInstance(context)
                                .addSingleSynchronousSubscription(it, SdkDataWipeEvent::class.java)
                        }
                    }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        if (!didInitialLoad) {
            Braze.getInstance(context).getCachedContentCards()?.let {
                replaceCards(it)
            }
            if (myCards.isEmpty() && Braze.getInstance(context).areCachedContentCardsStale()) {
                Braze.getInstance(context).requestContentCardsRefresh()
                if (networkUnavailableJob == null) {
                    isRefreshing = true
                    networkUnavailableJob =
                        BrazeCoroutineScope.launchDelayed(NETWORK_PROBLEM_WARNING_MS, Dispatchers.Main) {
                            networkUnavailable()
                        }
                }
            }
            didInitialLoad = true
        } else {
            brazelog(TAG) { "Doing a recomposition, so skipping loading of cards from cache" }
        }
    } else {
        if (!didInitialLoad) {
            replaceCards(cards)
            didInitialLoad = true
        } else {
            brazelog(TAG) { "Doing a recomposition, so skipping loading of cards" }
        }
    }

    val listState = rememberLazyListState()

    val fullyVisibleIndices: List<Int> by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
                emptyList()
            } else {
                val fullyVisibleItemsInfo = visibleItemsInfo.toMutableList()

                // If the size is exactly one, mark it as visible. Cards bigger than the viewport will never be
                // "fully" visible otherwise.
                if (fullyVisibleItemsInfo.size > 1) {
                    val lastItem = fullyVisibleItemsInfo.last()
                    val viewportHeight = layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset

                    if (lastItem.offset + lastItem.size > viewportHeight) {
                        fullyVisibleItemsInfo.removeAt(fullyVisibleItemsInfo.lastIndex)
                    }

                    val firstItemIfLeft = fullyVisibleItemsInfo.firstOrNull()
                    if (firstItemIfLeft != null && firstItemIfLeft.offset < layoutInfo.viewportStartOffset) {
                        fullyVisibleItemsInfo.removeAt(0)
                    }
                }

                fullyVisibleItemsInfo.map { it.index }
            }
        }
    }

    val modifier = style.modifier
        .background(style.listBackgroundColor())
        .let {
            // Don't enable pull-to-refresh if we were given a list of cards.
            if (enablePullToRefresh && cards == null) {
                it.pullRefresh(refreshState)
            } else {
                it
            }
        }

    Box(
        modifier
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.matchParentSize(),
            verticalArrangement = Arrangement.spacedBy(style.spacerSize),
            contentPadding = PaddingValues(vertical = style.listPadding)
        ) {
            if (myCards.isNotEmpty()) {
                items(items = myCards, key = { card -> card.id }) { card ->
                    val isVisible by remember(card.id) {
                        derivedStateOf {
                            fullyVisibleIndices.contains(myCards.indexOf(card))
                        }
                    }

                    if (isVisible && !impressedCards.contains(card.id)) {
                        logCardImpression(card)
                        card.viewed = true
                    }

                    if (card.isDismissibleByUser) {
                        val currentFraction = remember { mutableFloatStateOf(0f) }
                        val dismissThreshold = 0.50f

                        var hasCardBeenDismissed by remember { mutableStateOf(false) }
                        val dismissState = rememberDismissState(
                            confirmStateChange = {
                                // SwipeToDismiss handles flings very sensitively, so we do this to disable accidental dismisses
                                // https://stackoverflow.com/questions/72676541/compose-swipetodismiss-confirmstatechange-applies-only-threshold
                                var willDismiss = false
                                if (it == DismissValue.DismissedToStart || it == DismissValue.DismissedToEnd) {
                                    if (currentFraction.floatValue >= dismissThreshold && currentFraction.floatValue < 1.0f) {
                                        willDismiss = true
                                        hasCardBeenDismissed = true
                                    }
                                }
                                willDismiss
                            }
                        )

                        if (hasCardBeenDismissed) {
                            LaunchedEffect(Unit) {
                                myCards.remove(card)
                                brazelog { "Removing card ${card.id}. Total size is now ${myCards.size}" }
                                card.isDismissed = true
                                onCardDismissed?.invoke(card)
                            }
                        }

                        SwipeToDismiss(
                            modifier = Modifier.animateItem(),
                            state = dismissState,
                            dismissThresholds = {
                                @Suppress("DEPRECATION")
                                androidx.compose.material.FractionalThreshold(0.5f)
                            },
                            background = {}
                        ) {
                            currentFraction.floatValue = dismissState.progress.fraction
                            renderCard(card)
                        }
                    } else {
                        // Just display the card without any dismissing handling
                        renderCard(card)
                    }
                }
            } else {
                // Even though we don't have any items, we need to present an empty "Text" so that the screen is still
                // a scrollable item so PullRefresh works on it.
                items(count = 1) {
                    if (emptyComposable != null) {
                        emptyComposable.invoke()
                    } else {
                        // If we're trying to refresh because we're stale and empty, then don't display the error
                        // message just yet.
                        val emptyStringToUse = emptyString ?: stringResource(com.braze.ui.R.string.com_braze_feed_empty)
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillParentMaxHeight()
                                .background(color = cardStyle.cardBackgroundColor(type = CardType.DEFAULT))
                                .wrapContentSize(align = Alignment.Center),
                            text = emptyStringToUse,
                            style = style.emptyTextStyle ?: TextStyle(
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                                color = cardStyle.titleTextColor(type = CardType.DEFAULT)
                            )
                        )
                    }
                }
            }
        }
        if (enablePullToRefresh && cards == null) {
            PullRefreshIndicator(isRefreshing, refreshState, Modifier.align(Alignment.TopCenter))
        }
    }
}
