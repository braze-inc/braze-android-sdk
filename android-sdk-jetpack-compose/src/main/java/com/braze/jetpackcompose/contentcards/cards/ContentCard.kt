package com.braze.jetpackcompose.contentcards.cards

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.braze.configuration.BrazeConfigurationProvider
import com.braze.enums.CardType
import com.braze.jetpackcompose.LocalContentCardStyling
import com.braze.jetpackcompose.contentcards.styling.ContentCardStyling
import com.braze.models.cards.CaptionedImageCard
import com.braze.models.cards.Card
import com.braze.models.cards.ImageOnlyCard
import com.braze.models.cards.ShortNewsCard
import com.braze.models.cards.TextAnnouncementCard
import com.braze.support.BrazeLogger.brazelog
import com.braze.ui.contentcards.BrazeContentCardUtils

@Composable
@Suppress("LongMethod", "ComplexMethod")
fun ContentCard(
    card: Card,
    clickHandler: ((Card) -> Boolean)? = null,
    style: ContentCardStyling = LocalContentCardStyling.current
) {
    val context = LocalContext.current

    @Suppress("VariableNaming")
    val TAG = "ContentCardComposable"
    val decoderFactory =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) ImageDecoderDecoder.Factory() else GifDecoder.Factory()

    // Track the unread with this so we can dynamically change it if needed
    var isUnread by remember { mutableStateOf(!card.isIndicatorHighlighted) }

    if (card.isControl) {
        return
    }

    if (isUnread) {
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(LocalLifecycleOwner.current) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        brazelog(TAG) { "OnPause called in ContentCardComposable" }
                        if (!card.isIndicatorHighlighted) {
                            card.isIndicatorHighlighted = true
                            isUnread = false
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
    }

    BoxWithConstraints(
        Modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .padding(
                PaddingValues(
                    start = style.listPadding,
                    end = style.listPadding
                )
            )
    ) {
        val configurationProvider = remember { BrazeConfigurationProvider(context) }

        val parentWidth = maxWidth
        var extraPadding = 0.dp
        val maxWidth = style.maxCardWidth()
        if (parentWidth > maxWidth) {
            extraPadding = (parentWidth - maxWidth) / 2
        }

        Box(
            modifier = style
                .cardModifier(type = card.cardType, extraPadding)
                .clickable {
                    if (!card.isIndicatorHighlighted) {
                        card.isIndicatorHighlighted = true
                        isUnread = false
                    }
                    BrazeContentCardUtils.handleCardClick(context, card, clickHandler)
                }
        ) {
            if (card.cardType == CardType.TEXT_ANNOUNCEMENT) {
                val textAnnouncementCard = card as TextAnnouncementCard
                Column(
                    modifier = Modifier.padding(
                        PaddingValues(
                            top = style.textAnnouncementContentCardStyle.textColumnPaddingTop,
                            bottom = style.textAnnouncementContentCardStyle.textColumnPaddingBottom,
                            start = style.textAnnouncementContentCardStyle.textColumnPaddingStart,
                            end = style.textAnnouncementContentCardStyle.textColumnPaddingEnd
                        )
                    )
                ) {
                    textAnnouncementCard.title?.let {
                        Text(
                            text = it,
                            style = style.titleTextStyle(type = card.cardType),
                            modifier = Modifier.padding(PaddingValues(bottom = style.textAnnouncementContentCardStyle.titlePaddingBottom))
                        )
                    }
                    Text(
                        text = textAnnouncementCard.description,
                        style = style.descriptionTextStyle(type = card.cardType),
                    )
                    if (textAnnouncementCard.url != null) {
                        val hintActionText =
                            if (textAnnouncementCard.domain.isNullOrBlank()) card.url else textAnnouncementCard.domain
                        if (hintActionText != null) {
                            Text(
                                text = hintActionText,
                                style = style.hintActionTextStyle(type = card.cardType),
                                modifier = Modifier.padding(PaddingValues(top = style.textAnnouncementContentCardStyle.actionHintPaddingTop))
                            )
                        }
                    }
                }
            }

            if (card.cardType == CardType.IMAGE) {
                val imageOnlyCard = card as ImageOnlyCard
                val customComposable = style.imageComposable(card.cardType)
                if (customComposable != null) {
                    customComposable.invoke(card)
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageOnlyCard.imageUrl)
                            .decoderFactory(decoderFactory)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .run {
                                if (imageOnlyCard.aspectRatio > 0) {
                                    aspectRatio(imageOnlyCard.aspectRatio)
                                } else {
                                    this
                                }
                            }
                    )
                }
            }

            if (card.cardType == CardType.CAPTIONED_IMAGE) {
                val captionedImageCard = card as CaptionedImageCard
                Column {
                    val customComposable = style.imageComposable(card.cardType)
                    if (customComposable != null) {
                        customComposable.invoke(card)
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(captionedImageCard.imageUrl)
                                .decoderFactory(decoderFactory)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .run {
                                    if (captionedImageCard.aspectRatio > 0) {
                                        aspectRatio(captionedImageCard.aspectRatio)
                                    } else {
                                        this
                                    }
                                }
                        )
                    }
                    Column(
                        modifier = Modifier.padding(
                            PaddingValues(
                                top = style.captionedImageContentCardStyle.textColumnPaddingTop,
                                bottom = style.captionedImageContentCardStyle.textColumnPaddingBottom,
                                start = style.captionedImageContentCardStyle.textColumnPaddingStart,
                                end = style.captionedImageContentCardStyle.textColumnPaddingEnd
                            )
                        )
                    ) {
                        Text(
                            text = captionedImageCard.title,
                            style = style.titleTextStyle(type = card.cardType)
                        )
                        Text(
                            text = captionedImageCard.description,
                            style = style.descriptionTextStyle(type = card.cardType),
                            modifier = Modifier.padding(
                                PaddingValues(
                                    top = style.captionedImageContentCardStyle.descriptionPaddingTop
                                )

                            )
                        )
                        if (captionedImageCard.url != null) {
                            val hintActionText =
                                if (captionedImageCard.domain.isNullOrBlank()) card.url else captionedImageCard.domain
                            if (hintActionText != null) {
                                Text(
                                    text = hintActionText,
                                    style = style.hintActionTextStyle(type = card.cardType),
                                    modifier = Modifier.padding(PaddingValues(top = style.captionedImageContentCardStyle.actionHintPaddingTop))
                                )
                            }
                        }
                    }
                }
            }

            if (card.cardType == CardType.SHORT_NEWS) {
                val shortNewsCard = card as ShortNewsCard
                Row {
                    val customComposable = style.imageComposable(card.cardType)
                    if (customComposable != null) {
                        customComposable.invoke(card)
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(shortNewsCard.imageUrl)
                                .decoderFactory(decoderFactory)
                                .build(),
                            contentScale = ContentScale.Crop,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(
                                    PaddingValues(
                                        top = style.shortNewsContentCardStyle.imagePaddingTop,
                                        start = style.shortNewsContentCardStyle.imagePaddingStart,
                                        bottom = style.shortNewsContentCardStyle.imagePaddingBottom
                                    )
                                )
                                .width(style.shortNewsContentCardStyle.imageWidth)
                                .height(style.shortNewsContentCardStyle.imageHeight)
                                .clip(RoundedCornerShape(3.0.dp))
                        )
                    }
                    Column(
                        modifier = Modifier.padding(
                            PaddingValues(
                                top = style.shortNewsContentCardStyle.textColumnPaddingTop,
                                bottom = style.shortNewsContentCardStyle.textColumnPaddingBottom,
                                start = style.shortNewsContentCardStyle.textColumnPaddingStart,
                                end = style.shortNewsContentCardStyle.textColumnPaddingEnd
                            )
                        )
                    ) {
                        shortNewsCard.title?.let {
                            Text(
                                text = it,
                                style = style.titleTextStyle(type = card.cardType),
                                maxLines = 1,
                            )
                        }
                        Text(
                            text = shortNewsCard.description,
                            style = style.descriptionTextStyle(type = card.cardType),
                            modifier = Modifier.padding(
                                PaddingValues(
                                    top = style.shortNewsContentCardStyle.descriptionPaddingTop
                                )
                            )
                        )
                        if (shortNewsCard.url != null) {
                            val hintActionText =
                                if (shortNewsCard.domain.isNullOrBlank()) card.url else shortNewsCard.domain
                            if (hintActionText != null) {
                                Text(
                                    text = hintActionText,
                                    style = style.hintActionTextStyle(type = card.cardType),
                                    modifier = Modifier.padding(PaddingValues(top = style.shortNewsContentCardStyle.actionHintPaddingTop))
                                )
                            }
                        }
                    }
                }
            }

            if (
                configurationProvider.isContentCardsUnreadVisualIndicatorEnabled
                && isUnread
            ) {
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    color = style.unreadIndicatorColor(card.cardType),
                    thickness = 4.dp
                )
            }

            if (card.isPinned) {
                val customPin = style.pinnedComposable(card.cardType)
                if (customPin != null) {
                    customPin.invoke()
                } else {
                    Image(
                        painter = painterResource(id = style.pinnedResourceId(card.cardType)),
                        contentDescription = null,
                        modifier = Modifier
                            .align(style.pinnedAlignment(card))
                            .padding(horizontal = extraPadding)
                    )
                }
            }
        }
    }
}
