package com.braze.jetpackcompose.contentcards.styling

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braze.enums.CardType
import com.braze.models.cards.Card
import com.braze.ui.R

@Suppress("MagicNumber")
val UndefinedAlignment: Alignment = BiasAlignment(-2.0f, -2.0f)

/**
 * This specifies the various styles to use for rendering Content Cards.
 *
 * Values will be pulled from the specific card type ([imageOnlyContentCardStyle], [textAnnouncementContentCardStyle],
 * [shortNewsContentCardStyle], and [captionedImageContentCardStyle]). If the value isn't present there, then it will
 * used the more general value for all card types.
 *
 * If the [modifier] is specified, it will be used directly with very little modification.
 *
 * Note: Due to how Jetpack Compose works, listPadding is passed in here so that additional padding can be added to the
 * cards. This is needed to allowed swiped cards to be swiped completely out of view. You will also need to add this to
 * your modifier's padding if you specify your own.
 *
 * @property modifier to use for the card. If specified, it will be used directly, bypassing many of the other values here.
 * @property pinnedResourceId The drawable resource ID of the image used for pinned cards.
 * @property pinnedImageAlignment The alignment of the pinned image.
 * @property unreadIndicatorColor The color of the unread indicator.
 * @property pinnedComposable A Composable function that will render on pinned cards. If this is specified, then the other pinned paramaters are not used.
 * @property imageComposable A Composable function that accepts a [Card] and will render the image. If not specified, default image library is used (Coil)
 * @property borderColor Border color. If not specified, it will fallback to R.color.com_braze_content_card_background_border
 * @property borderSize Size of the border. This is used to handle all sides at once. If a specific size is specified (e.g.
 * topBorderSize), that will be used instead.
 * @property topBorderSize Size of the top border. If not specified, it will fallback to borderSize, and then
 * R.dimen.com_braze_content_card_background_border_top
 * @property startBorderSize Size of the border on the start side. If not specified, it will fallback to borderSize, and
 * then R.dimen.com_braze_content_card_background_border_left
 * @property endBorderSize Size of the border on the end side. If not specified, it will fallback to borderSize, and then
 * R.dimen.com_braze_content_card_background_border_right
 * @property bottomBorderSize Size of the top border. If not specified, it will fallback to borderSize, and then
 * R.dimen.com_braze_content_card_background_border_bottom
 * @property borderRadius Size of the border radius. If not specified, it will fallback to R.dimen.com_braze_content_card_background_corner_radius
 * @property shadowColor Color of the card shadow. If not specified, it will fallback to R.color.com_braze_content_card_background_shadow
 * @property shadowSize Size of the card shadow. If not specified, it will fallback to R.dimen.com_braze_content_card_background_shadow_bottom
 * @property shadowRadius Size of the card shadow radius. If not specified, it will fallback to R.dimen.com_braze_content_card_background_shadow_radius
 * @property maxCardWidth Maximum width of the card. If not specified, it will fallback to R.dimen.com_braze_content_cards_max_width
 * @property listPadding Size of the list padding. See note above.
 * @property cardBackgroundColor Color of the card background. If not specified, it will fallback to R.color.com_braze_content_card_background
 * @property titleTextStyle [TextStyle] used for the title. If this is specified, other title text customization will be ignored.
 * @property descriptionTextStyle [TextStyle] used for the title. If this is specified, other title text customization will be ignored.
 * @property actionHintTextStyle [TextStyle] used for the title. If this is specified, other title text customization will be ignored.
 * @property titleTextColor Color of the card title text. If not specified, it will fallback to R.color.com_braze_content_cards_title
 * @property descriptionTextColor Color of the card description text. If not specified, it will fallback to R.color.com_braze_content_cards_description
 * @property actionHintTextColor Color of the card description text. If not specified, it will fallback to
 * R.color.com_braze_content_cards_action_hint_text_color
 * @property imageOnlyContentCardStyle Style to use specifically for Image Only Content Cards. See note above.
 * @property textAnnouncementContentCardStyle Style to use specifically for Text Announcement Content Cards. See note above.
 * @property shortNewsContentCardStyle Style to use specifically for Short News Content Cards. See note above.
 * @property captionedImageContentCardStyle Style to use specifically for Captioned Image Content Cards. See note above.
 */
@Suppress("LongParameterList", "TooManyFunctions", "BooleanPropertyNaming")
open class ContentCardStyling(
    val modifier: Modifier? = null,
    val pinnedResourceId: Int = R.drawable.com_braze_content_card_icon_pinned,
    val pinnedImageAlignment: Alignment = Alignment.TopEnd,
    val unreadIndicatorColor: Color = Color.Unspecified,
    val pinnedComposable: @Composable (() -> Unit)? = null,
    val imageComposable: @Composable ((Card) -> Unit)? = null,
    val borderColor: Color = Color.Unspecified,
    val borderSize: Dp = Dp.Unspecified,
    val topBorderSize: Dp = Dp.Unspecified,
    val startBorderSize: Dp = Dp.Unspecified,
    val endBorderSize: Dp = Dp.Unspecified,
    val bottomBorderSize: Dp = Dp.Unspecified,
    val borderRadius: Dp = Dp.Unspecified,
    val shadowColor: Color = Color.Unspecified,
    val shadowSize: Dp = Dp.Unspecified,
    val shadowRadius: Dp = Dp.Unspecified,
    val maxCardWidth: Dp = Dp.Unspecified,
    val listPadding: Dp = 32.dp,
    val cardBackgroundColor: Color = Color.Unspecified,
    val titleTextStyle: TextStyle? = null,
    val descriptionTextStyle: TextStyle? = null,
    val actionHintTextStyle: TextStyle? = null,
    val titleTextColor: Color = Color.Unspecified,
    val descriptionTextColor: Color = Color.Unspecified,
    val actionHintTextColor: Color = Color.Unspecified,
    val imageOnlyContentCardStyle: BrazeImageOnlyContentCardStyling = BrazeImageOnlyContentCardStyling(),
    val textAnnouncementContentCardStyle: BrazeTextAnnouncementContentCardStyling =
        BrazeTextAnnouncementContentCardStyling(),
    val shortNewsContentCardStyle: BrazeShortNewsContentCardStyling = BrazeShortNewsContentCardStyling(),
    val captionedImageContentCardStyle: BrazeCaptionedImageContentCardStyling = BrazeCaptionedImageContentCardStyling(),
) {

    @Composable
    fun imageComposable(type: CardType): @Composable ((Card) -> Unit)? {
        val composable = when (type) {
            CardType.IMAGE -> imageOnlyContentCardStyle.imageComposable
            CardType.SHORT_NEWS -> shortNewsContentCardStyle.imageComposable
            CardType.CAPTIONED_IMAGE -> captionedImageContentCardStyle.imageComposable
            else -> null
        }
        if (composable != null) {
            return composable
        }
        return imageComposable
    }

    @Composable
    fun pinnedComposable(type: CardType): @Composable (() -> Unit)? {
        val pinned = when (type) {
            CardType.IMAGE -> imageOnlyContentCardStyle.pinnedComposable
            CardType.TEXT_ANNOUNCEMENT -> textAnnouncementContentCardStyle.pinnedComposable
            CardType.SHORT_NEWS -> shortNewsContentCardStyle.pinnedComposable
            CardType.CAPTIONED_IMAGE -> captionedImageContentCardStyle.pinnedComposable
            else -> null
        }
        if (pinned != null) {
            return pinned
        }
        return pinnedComposable
    }

    @Composable
    fun pinnedResourceId(type: CardType): Int {
        val resourceId = when (type) {
            CardType.IMAGE -> imageOnlyContentCardStyle.pinnedResourceId
            CardType.TEXT_ANNOUNCEMENT -> textAnnouncementContentCardStyle.pinnedResourceId
            CardType.SHORT_NEWS -> shortNewsContentCardStyle.pinnedResourceId
            CardType.CAPTIONED_IMAGE -> captionedImageContentCardStyle.pinnedResourceId
            else -> 0
        }
        if (resourceId != 0) {
            return resourceId
        }
        return pinnedResourceId
    }

    @Composable
    fun unreadIndicatorColor(type: CardType): Color {
        val color = when (type) {
            CardType.IMAGE -> imageOnlyContentCardStyle.unreadIndicatorColor
            CardType.TEXT_ANNOUNCEMENT -> textAnnouncementContentCardStyle.unreadIndicatorColor
            CardType.SHORT_NEWS -> shortNewsContentCardStyle.unreadIndicatorColor
            CardType.CAPTIONED_IMAGE -> captionedImageContentCardStyle.unreadIndicatorColor
            else -> Color.Unspecified
        }

        return if (color != Color.Unspecified) {
            color
        } else if (unreadIndicatorColor != Color.Unspecified) {
            unreadIndicatorColor
        } else {
            colorResource(id = R.color.com_braze_content_cards_unread_bar_color)
        }
    }

    @Composable
    fun cardBackgroundColor(type: CardType): Color {
        val color = when (type) {
            CardType.IMAGE -> imageOnlyContentCardStyle.cardBackgroundColor
            CardType.TEXT_ANNOUNCEMENT -> textAnnouncementContentCardStyle.cardBackgroundColor
            CardType.SHORT_NEWS -> shortNewsContentCardStyle.cardBackgroundColor
            CardType.CAPTIONED_IMAGE -> captionedImageContentCardStyle.cardBackgroundColor
            else -> Color.Unspecified
        }
        return if (color != Color.Unspecified) {
            color
        } else if (cardBackgroundColor != Color.Unspecified) {
            cardBackgroundColor
        } else {
            colorResource(id = R.color.com_braze_content_card_background)
        }
    }

    @Composable
    fun titleTextColor(type: CardType): Color {
        val color = when (type) {
            CardType.TEXT_ANNOUNCEMENT -> textAnnouncementContentCardStyle.titleTextColor
            CardType.SHORT_NEWS -> shortNewsContentCardStyle.titleTextColor
            CardType.CAPTIONED_IMAGE -> captionedImageContentCardStyle.titleTextColor
            else -> Color.Unspecified
        }
        return if (color != Color.Unspecified) {
            color
        } else if (titleTextColor != Color.Unspecified) {
            titleTextColor
        } else {
            colorResource(id = R.color.com_braze_content_cards_title)
        }
    }

    @Composable
    fun descriptionTextColor(type: CardType): Color {
        val color = when (type) {
            CardType.TEXT_ANNOUNCEMENT -> textAnnouncementContentCardStyle.descriptionTextColor
            CardType.SHORT_NEWS -> shortNewsContentCardStyle.descriptionTextColor
            CardType.CAPTIONED_IMAGE -> captionedImageContentCardStyle.descriptionTextColor
            else -> Color.Unspecified
        }
        return if (color != Color.Unspecified) {
            color
        } else if (descriptionTextColor != Color.Unspecified) {
            descriptionTextColor
        } else {
            colorResource(id = R.color.com_braze_content_cards_description)
        }
    }

    @Composable
    fun actionHintTextColor(type: CardType): Color {
        val color = when (type) {
            CardType.TEXT_ANNOUNCEMENT -> textAnnouncementContentCardStyle.actionHintTextColor
            CardType.SHORT_NEWS -> shortNewsContentCardStyle.actionHintTextColor
            CardType.CAPTIONED_IMAGE -> captionedImageContentCardStyle.actionHintTextColor
            else -> Color.Unspecified
        }
        return if (color != Color.Unspecified) {
            color
        } else if (actionHintTextColor != Color.Unspecified) {
            actionHintTextColor
        } else {
            colorResource(id = R.color.com_braze_content_cards_action_hint_text_color)
        }
    }

    fun pinnedAlignment(card: Card): Alignment {
        val alignment = when (card.cardType) {
            CardType.IMAGE -> imageOnlyContentCardStyle.pinnedImageAlignment
            CardType.TEXT_ANNOUNCEMENT -> textAnnouncementContentCardStyle.pinnedImageAlignment
            CardType.SHORT_NEWS -> shortNewsContentCardStyle.pinnedImageAlignment
            CardType.CAPTIONED_IMAGE -> captionedImageContentCardStyle.pinnedImageAlignment
            else -> UndefinedAlignment
        }
        if (alignment != UndefinedAlignment) {
            return alignment
        }
        return pinnedImageAlignment
    }

    @Composable
    fun borderColor(type: CardType): Color {
        val color = when (type) {
            CardType.IMAGE -> captionedImageContentCardStyle.borderColor
            CardType.TEXT_ANNOUNCEMENT -> textAnnouncementContentCardStyle.borderColor
            CardType.SHORT_NEWS -> shortNewsContentCardStyle.borderColor
            CardType.CAPTIONED_IMAGE -> captionedImageContentCardStyle.borderColor
            else -> Color.Unspecified
        }
        return if (color != Color.Unspecified) {
            color
        } else if (borderColor != Color.Unspecified) {
            borderColor
        } else {
            colorResource(id = R.color.com_braze_content_card_background_border)
        }
    }

    fun borderSize(type: CardType): Dp {
        val size = when (type) {
            CardType.IMAGE -> imageOnlyContentCardStyle.borderSize
            CardType.TEXT_ANNOUNCEMENT -> textAnnouncementContentCardStyle.borderSize
            CardType.SHORT_NEWS -> shortNewsContentCardStyle.borderSize
            CardType.CAPTIONED_IMAGE -> captionedImageContentCardStyle.borderSize
            else -> Dp.Unspecified
        }

        if (size != Dp.Unspecified) {
            return size
        }

        // Return the generic border size here. This may be Dp.Unspecified, but that's OK, because
        // this function is only used to get a generic border size if a specific side doesn't exist.
        // Those functions will fallback to XML.
        return borderSize
    }

    @Composable
    fun topBorderSize(type: CardType): Dp {
        var size = when (type) {
            CardType.IMAGE -> imageOnlyContentCardStyle.topBorderSize
            CardType.TEXT_ANNOUNCEMENT -> textAnnouncementContentCardStyle.topBorderSize
            CardType.SHORT_NEWS -> shortNewsContentCardStyle.topBorderSize
            CardType.CAPTIONED_IMAGE -> captionedImageContentCardStyle.topBorderSize
            else -> Dp.Unspecified
        }

        if (size != Dp.Unspecified) {
            return size
        }

        size = topBorderSize
        if (size != Dp.Unspecified) {
            return size
        }

        size = borderSize(type)
        if (size != Dp.Unspecified) {
            return size
        }

        return dimensionResource(id = R.dimen.com_braze_content_card_background_border_top)
    }

    @Composable
    fun startBorderSize(type: CardType): Dp {
        var size = when (type) {
            CardType.IMAGE -> imageOnlyContentCardStyle.startBorderSize
            CardType.TEXT_ANNOUNCEMENT -> textAnnouncementContentCardStyle.startBorderSize
            CardType.SHORT_NEWS -> shortNewsContentCardStyle.startBorderSize
            CardType.CAPTIONED_IMAGE -> captionedImageContentCardStyle.startBorderSize
            else -> Dp.Unspecified
        }

        if (size != Dp.Unspecified) {
            return size
        }

        size = startBorderSize
        if (size != Dp.Unspecified) {
            return size
        }

        size = borderSize(type)
        if (size != Dp.Unspecified) {
            return size
        }

        return dimensionResource(id = R.dimen.com_braze_content_card_background_border_left)
    }

    @Composable
    fun endBorderSize(type: CardType): Dp {
        var size = when (type) {
            CardType.IMAGE -> imageOnlyContentCardStyle.endBorderSize
            CardType.TEXT_ANNOUNCEMENT -> textAnnouncementContentCardStyle.endBorderSize
            CardType.SHORT_NEWS -> shortNewsContentCardStyle.endBorderSize
            CardType.CAPTIONED_IMAGE -> captionedImageContentCardStyle.endBorderSize
            else -> Dp.Unspecified
        }

        if (size != Dp.Unspecified) {
            return size
        }

        size = endBorderSize
        if (size != Dp.Unspecified) {
            return size
        }

        size = borderSize(type)
        if (size != Dp.Unspecified) {
            return size
        }

        return dimensionResource(id = R.dimen.com_braze_content_card_background_border_right)
    }

    @Composable
    fun bottomBorderSize(type: CardType): Dp {
        var size = when (type) {
            CardType.IMAGE -> imageOnlyContentCardStyle.bottomBorderSize
            CardType.TEXT_ANNOUNCEMENT -> textAnnouncementContentCardStyle.bottomBorderSize
            CardType.SHORT_NEWS -> shortNewsContentCardStyle.bottomBorderSize
            CardType.CAPTIONED_IMAGE -> captionedImageContentCardStyle.bottomBorderSize
            else -> Dp.Unspecified
        }

        if (size != Dp.Unspecified) {
            return size
        }

        size = bottomBorderSize
        if (size != Dp.Unspecified) {
            return size
        }

        size = borderSize(type)
        if (size != Dp.Unspecified) {
            return size
        }

        return dimensionResource(id = R.dimen.com_braze_content_card_background_border_bottom)
    }

    @Composable
    fun borderRadius(type: CardType): Dp {
        var size = when (type) {
            CardType.IMAGE -> imageOnlyContentCardStyle.borderRadius
            CardType.TEXT_ANNOUNCEMENT -> textAnnouncementContentCardStyle.borderRadius
            CardType.SHORT_NEWS -> shortNewsContentCardStyle.borderRadius
            CardType.CAPTIONED_IMAGE -> captionedImageContentCardStyle.borderRadius
            else -> Dp.Unspecified
        }

        if (size != Dp.Unspecified) {
            return size
        }

        size = borderRadius
        if (size != Dp.Unspecified) {
            return size
        }

        return dimensionResource(id = R.dimen.com_braze_content_card_background_corner_radius)
    }

    @Composable
    fun shadowColor(type: CardType): Color {
        val color = when (type) {
            CardType.IMAGE -> captionedImageContentCardStyle.shadowColor
            CardType.TEXT_ANNOUNCEMENT -> textAnnouncementContentCardStyle.shadowColor
            CardType.SHORT_NEWS -> shortNewsContentCardStyle.shadowColor
            CardType.CAPTIONED_IMAGE -> captionedImageContentCardStyle.shadowColor
            else -> Color.Unspecified
        }
        return if (color != Color.Unspecified) {
            color
        } else if (shadowColor != Color.Unspecified) {
            shadowColor
        } else {
            colorResource(id = R.color.com_braze_content_card_background_shadow)
        }
    }

    @Composable
    fun shadowSize(type: CardType): Dp {
        val size = when (type) {
            CardType.IMAGE -> imageOnlyContentCardStyle.shadowSize
            CardType.TEXT_ANNOUNCEMENT -> textAnnouncementContentCardStyle.shadowSize
            CardType.SHORT_NEWS -> shortNewsContentCardStyle.shadowSize
            CardType.CAPTIONED_IMAGE -> captionedImageContentCardStyle.shadowSize
            else -> Dp.Unspecified
        }

        return if (size != Dp.Unspecified) {
            size
        } else if (shadowSize != Dp.Unspecified) {
            shadowSize
        } else {
            dimensionResource(id = R.dimen.com_braze_content_card_background_shadow_bottom)
        }
    }

    @Composable
    fun shadowRadius(type: CardType): Dp {
        val size = when (type) {
            CardType.IMAGE -> imageOnlyContentCardStyle.shadowRadius
            CardType.TEXT_ANNOUNCEMENT -> textAnnouncementContentCardStyle.shadowRadius
            CardType.SHORT_NEWS -> shortNewsContentCardStyle.shadowRadius
            CardType.CAPTIONED_IMAGE -> captionedImageContentCardStyle.shadowRadius
            else -> Dp.Unspecified
        }

        return if (size != Dp.Unspecified) {
            size
        } else if (shadowRadius != Dp.Unspecified) {
            shadowRadius
        } else {
            dimensionResource(id = R.dimen.com_braze_content_card_background_shadow_radius)
        }
    }

    @Composable
    fun titleTextStyle(type: CardType): TextStyle {
        val textStyle = when (type) {
            CardType.IMAGE -> imageOnlyContentCardStyle.titleTextStyle
            CardType.TEXT_ANNOUNCEMENT -> textAnnouncementContentCardStyle.titleTextStyle
            CardType.SHORT_NEWS -> shortNewsContentCardStyle.titleTextStyle
            CardType.CAPTIONED_IMAGE -> captionedImageContentCardStyle.titleTextStyle
            else -> null
        }

        return textStyle
            ?: titleTextStyle
            ?: when (type) {
                CardType.TEXT_ANNOUNCEMENT ->
                    TextStyle(
                        color = titleTextColor(type = type),
                        fontWeight = textAnnouncementContentCardStyle.titleFontWeight,
                        fontSize = textAnnouncementContentCardStyle.titleTextSize,
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = textAnnouncementContentCardStyle.titleIncludeFontPadding
                        )
                    )
                CardType.SHORT_NEWS ->
                    TextStyle(
                        color = titleTextColor(type = type),
                        fontWeight = shortNewsContentCardStyle.titleFontWeight,
                        fontSize = shortNewsContentCardStyle.titleTextSize,
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = shortNewsContentCardStyle.titleIncludeFontPadding
                        )
                    )
                CardType.CAPTIONED_IMAGE ->
                    TextStyle(
                        color = titleTextColor(type = type),
                        fontWeight = captionedImageContentCardStyle.titleFontWeight,
                        fontSize = captionedImageContentCardStyle.titleTextSize,
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = captionedImageContentCardStyle.titleIncludeFontPadding
                        )
                    )
                else ->
                    // We shouldn't actually ever get here
                    TextStyle(
                        color = titleTextColor(type = type)
                    )
            }
    }

    @Composable
    fun descriptionTextStyle(type: CardType): TextStyle {
        val textStyle = when (type) {
            CardType.IMAGE -> imageOnlyContentCardStyle.descriptionTextStyle
            CardType.TEXT_ANNOUNCEMENT -> textAnnouncementContentCardStyle.descriptionTextStyle
            CardType.SHORT_NEWS -> shortNewsContentCardStyle.descriptionTextStyle
            CardType.CAPTIONED_IMAGE -> captionedImageContentCardStyle.descriptionTextStyle
            else -> null
        }

        return textStyle
            ?: descriptionTextStyle
            ?: when (type) {
                CardType.TEXT_ANNOUNCEMENT ->
                    TextStyle(
                        color = descriptionTextColor(type = type),
                        fontSize = textAnnouncementContentCardStyle.descriptionTextSize,
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = textAnnouncementContentCardStyle.descriptionIncludeFontPadding
                        )
                    )
                CardType.SHORT_NEWS ->
                    TextStyle(
                        color = descriptionTextColor(type = type),
                        fontSize = shortNewsContentCardStyle.descriptionTextSize,
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = shortNewsContentCardStyle.descriptionIncludeFontPadding
                        )
                    )
                CardType.CAPTIONED_IMAGE ->
                    TextStyle(
                        color = descriptionTextColor(type = type),
                        fontSize = captionedImageContentCardStyle.descriptionTextSize,
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = captionedImageContentCardStyle.descriptionIncludeFontPadding
                        )
                    )
                else ->
                    // We shouldn't actually ever get here
                    TextStyle(
                        color = descriptionTextColor(type = type)
                    )
            }
    }

    @Composable
    fun hintActionTextStyle(type: CardType): TextStyle {
        val textStyle = when (type) {
            CardType.IMAGE -> imageOnlyContentCardStyle.actionHintTextStyle
            CardType.TEXT_ANNOUNCEMENT -> textAnnouncementContentCardStyle.actionHintTextStyle
            CardType.SHORT_NEWS -> shortNewsContentCardStyle.actionHintTextStyle
            CardType.CAPTIONED_IMAGE -> captionedImageContentCardStyle.actionHintTextStyle
            else -> null
        }

        return textStyle
            ?: actionHintTextStyle
            ?: when (type) {
                CardType.TEXT_ANNOUNCEMENT ->
                    TextStyle(
                        color = actionHintTextColor(type = type),
                        fontSize = textAnnouncementContentCardStyle.actionHintTextSize,
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = textAnnouncementContentCardStyle.actionHintIncludeFontPadding
                        )
                    )
                CardType.SHORT_NEWS ->
                    TextStyle(
                        color = actionHintTextColor(type = type),
                        fontSize = shortNewsContentCardStyle.actionHintTextSize,
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = shortNewsContentCardStyle.actionHintIncludeFontPadding
                        )
                    )
                CardType.CAPTIONED_IMAGE ->
                    TextStyle(
                        color = actionHintTextColor(type = type),
                        fontSize = captionedImageContentCardStyle.actionHintTextSize,
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = captionedImageContentCardStyle.actionHintIncludeFontPadding
                        )
                    )
                else ->
                    // We shouldn't actually ever get here
                    TextStyle(
                        color = actionHintTextColor(type = type)
                    )
            }
    }

    @Composable
    fun maxCardWidth(): Dp {
        return if (maxCardWidth != Dp.Unspecified) {
            maxCardWidth
        } else {
            dimensionResource(id = R.dimen.com_braze_content_cards_max_width)
        }
    }

    /**
     * Return a modifier to be used for a specific card type.
     * If a modifier has been specified for the specific card type, it will be used.
     * If no modifier has been specified for the specific card type, then the general modifier will be used.
     *
     * If neither a specific or generic modifier, a default will be created using the background color.
     *
     * NOTE: If modifier is used, then it should have a background color specified in it.
     *
     * @param type
     * @param extraPadding
     * @return
     */
    @SuppressLint("ModifierFactoryExtensionFunction")
    @Composable
    fun cardModifier(type: CardType, extraPadding: Dp): Modifier {
        var cardModifier: Modifier?
        when (type) {
            CardType.IMAGE -> {
                cardModifier = imageOnlyContentCardStyle.modifier
            }

            CardType.TEXT_ANNOUNCEMENT -> {
                cardModifier = textAnnouncementContentCardStyle.modifier
            }

            CardType.SHORT_NEWS -> {
                cardModifier = shortNewsContentCardStyle.modifier
            }

            CardType.CAPTIONED_IMAGE -> {
                cardModifier = captionedImageContentCardStyle.modifier
            }

            else -> {
                cardModifier = modifier
            }
        }

        // If we don't have a modifier, then try to use the general card modifier
        if (cardModifier == null) {
            cardModifier = modifier
        }

        val cardBorderRadius = borderRadius(type)
        val cardShadowRadius = shadowRadius(type)

        // If we still don't have a modifier, then construct a modifier with the background color
        if (cardModifier == null) {
            cardModifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(horizontal = extraPadding)
                .clip(RoundedCornerShape(cardShadowRadius))
                .background(shadowColor(type))
                .padding(bottom = shadowSize(type))
                .clip(RoundedCornerShape(cardShadowRadius))
                .background(borderColor(type))
                .padding(
                    start = startBorderSize(type),
                    end = endBorderSize(type),
                    top = topBorderSize(type),
                    bottom = bottomBorderSize(type)
                )
                .clip(RoundedCornerShape(cardBorderRadius))
                .background(cardBackgroundColor(type))
        }

        return cardModifier
    }
}

@Suppress("LongParameterList")
open class BrazeContentCardBaseStyling(
    val modifier: Modifier? = null,
    val pinnedResourceId: Int = R.drawable.com_braze_content_card_icon_pinned,
    val pinnedImageAlignment: Alignment = Alignment.TopEnd,
    val unreadIndicatorColor: Color = Color.Unspecified,
    val pinnedComposable: @Composable (() -> Unit)? = null,
    val imageComposable: @Composable ((Card) -> Unit)? = null,
    val cardBackgroundColor: Color = Color.Unspecified,
    val titleTextStyle: TextStyle? = null,
    val descriptionTextStyle: TextStyle? = null,
    val actionHintTextStyle: TextStyle? = null,
    val titleTextColor: Color = Color.Unspecified,
    val descriptionTextColor: Color = Color.Unspecified,
    val actionHintTextColor: Color = Color.Unspecified,
    val borderColor: Color = Color.Unspecified,
    val borderSize: Dp = Dp.Unspecified,
    val topBorderSize: Dp = Dp.Unspecified,
    val startBorderSize: Dp = Dp.Unspecified,
    val endBorderSize: Dp = Dp.Unspecified,
    val bottomBorderSize: Dp = Dp.Unspecified,
    val borderRadius: Dp = Dp.Unspecified,
    val shadowColor: Color = Color.Unspecified,
    val shadowSize: Dp = Dp.Unspecified,
    val shadowRadius: Dp = Dp.Unspecified
)

@Suppress("LongParameterList")
class BrazeImageOnlyContentCardStyling(
    modifier: Modifier? = null,
    pinnedResourceId: Int = 0,
    pinnedImageAlignment: Alignment = UndefinedAlignment,
    unreadIndicatorColor: Color = Color.Unspecified,
    cardBackgroundColor: Color = Color.Unspecified,
    pinnedComposable: @Composable (() -> Unit)? = null,
    borderColor: Color = Color.Unspecified,
    borderSize: Dp = Dp.Unspecified,
    topBorderSize: Dp = Dp.Unspecified,
    startBorderSize: Dp = Dp.Unspecified,
    endBorderSize: Dp = Dp.Unspecified,
    bottomBorderSize: Dp = Dp.Unspecified,
    borderRadius: Dp = Dp.Unspecified,
    shadowColor: Color = Color.Unspecified,
    shadowSize: Dp = Dp.Unspecified,
    shadowRadius: Dp = Dp.Unspecified,
    imageComposable: @Composable ((Card) -> Unit)? = null,
    val imageHeight: Dp = 250.dp
) : BrazeContentCardBaseStyling(
    modifier = modifier,
    pinnedResourceId = pinnedResourceId,
    pinnedImageAlignment = pinnedImageAlignment,
    unreadIndicatorColor = unreadIndicatorColor,
    cardBackgroundColor = cardBackgroundColor,
    pinnedComposable = pinnedComposable,
    imageComposable = imageComposable,
    borderColor = borderColor,
    borderSize = borderSize,
    topBorderSize = topBorderSize,
    startBorderSize = startBorderSize,
    endBorderSize = endBorderSize,
    bottomBorderSize = bottomBorderSize,
    borderRadius = borderRadius,
    shadowColor = shadowColor,
    shadowSize = shadowSize,
    shadowRadius = shadowRadius
)

@Suppress("LongParameterList", "BooleanPropertyNaming")
class BrazeTextAnnouncementContentCardStyling(
    modifier: Modifier? = null,
    pinnedResourceId: Int = 0,
    pinnedImageAlignment: Alignment = UndefinedAlignment,
    unreadIndicatorColor: Color = Color.Unspecified,
    cardBackgroundColor: Color = Color.Unspecified,
    pinnedComposable: @Composable (() -> Unit)? = null,
    borderColor: Color = Color.Unspecified,
    borderSize: Dp = Dp.Unspecified,
    topBorderSize: Dp = Dp.Unspecified,
    startBorderSize: Dp = Dp.Unspecified,
    endBorderSize: Dp = Dp.Unspecified,
    bottomBorderSize: Dp = Dp.Unspecified,
    borderRadius: Dp = Dp.Unspecified,
    shadowColor: Color = Color.Unspecified,
    shadowSize: Dp = Dp.Unspecified,
    shadowRadius: Dp = Dp.Unspecified,
    val textColumnPaddingTop: Dp = 20.dp,
    val textColumnPaddingBottom: Dp = 25.dp,
    val textColumnPaddingStart: Dp = 25.dp,
    val textColumnPaddingEnd: Dp = 25.dp,
    titleTextStyle: TextStyle? = null,
    descriptionTextStyle: TextStyle? = null,
    actionHintTextStyle: TextStyle? = null,
    titleTextColor: Color = Color.Unspecified,
    val titleTextSize: TextUnit = 16.sp,
    val titleFontWeight: FontWeight = FontWeight.Bold,
    val titleIncludeFontPadding: Boolean = false,
    val titlePaddingBottom: Dp = 10.dp,
    descriptionTextColor: Color = Color.Unspecified,
    val descriptionTextSize: TextUnit = 13.sp,
    val descriptionIncludeFontPadding: Boolean = false,
    actionHintTextColor: Color = Color.Unspecified,
    val actionHintTextSize: TextUnit = 13.sp,
    val actionHintIncludeFontPadding: Boolean = false,
    val actionHintPaddingTop: Dp = 10.dp
) : BrazeContentCardBaseStyling(
    modifier = modifier,
    pinnedResourceId = pinnedResourceId,
    pinnedImageAlignment = pinnedImageAlignment,
    unreadIndicatorColor = unreadIndicatorColor,
    cardBackgroundColor = cardBackgroundColor,
    pinnedComposable = pinnedComposable,
    borderColor = borderColor,
    borderSize = borderSize,
    topBorderSize = topBorderSize,
    startBorderSize = startBorderSize,
    endBorderSize = endBorderSize,
    bottomBorderSize = bottomBorderSize,
    borderRadius = borderRadius,
    shadowColor = shadowColor,
    shadowSize = shadowSize,
    shadowRadius = shadowRadius,
    titleTextStyle = titleTextStyle,
    descriptionTextStyle = descriptionTextStyle,
    actionHintTextStyle = actionHintTextStyle,
    titleTextColor = titleTextColor,
    descriptionTextColor = descriptionTextColor,
    actionHintTextColor = actionHintTextColor
)

@Suppress("LongParameterList", "BooleanPropertyNaming")
class BrazeShortNewsContentCardStyling(
    modifier: Modifier? = null,
    pinnedResourceId: Int = 0,
    pinnedImageAlignment: Alignment = UndefinedAlignment,
    unreadIndicatorColor: Color = Color.Unspecified,
    cardBackgroundColor: Color = Color.Unspecified,
    pinnedComposable: @Composable (() -> Unit)? = null,
    borderColor: Color = Color.Unspecified,
    borderSize: Dp = Dp.Unspecified,
    topBorderSize: Dp = Dp.Unspecified,
    startBorderSize: Dp = Dp.Unspecified,
    endBorderSize: Dp = Dp.Unspecified,
    bottomBorderSize: Dp = Dp.Unspecified,
    borderRadius: Dp = Dp.Unspecified,
    shadowColor: Color = Color.Unspecified,
    shadowSize: Dp = Dp.Unspecified,
    shadowRadius: Dp = Dp.Unspecified,
    val textColumnPaddingTop: Dp = 20.dp,
    val textColumnPaddingBottom: Dp = 25.dp,
    val textColumnPaddingStart: Dp = 12.dp,
    val textColumnPaddingEnd: Dp = 25.dp,
    imageComposable: @Composable ((Card) -> Unit)? = null,
    val imageHeight: Dp = 57.5.dp,
    val imageWidth: Dp = 57.5.dp,
    val imagePaddingTop: Dp = 20.dp,
    val imagePaddingStart: Dp = 25.dp,
    val imagePaddingBottom: Dp = 25.dp,
    titleTextStyle: TextStyle? = null,
    descriptionTextStyle: TextStyle? = null,
    actionHintTextStyle: TextStyle? = null,
    titleTextColor: Color = Color.Unspecified,
    val titleTextSize: TextUnit = 16.sp,
    val titleFontWeight: FontWeight = FontWeight.Bold,
    val titleIncludeFontPadding: Boolean = false,
    descriptionTextColor: Color = Color.Unspecified,
    val descriptionTextSize: TextUnit = 13.sp,
    val descriptionIncludeFontPadding: Boolean = false,
    val descriptionPaddingTop: Dp = 10.dp,
    actionHintTextColor: Color = Color.Unspecified,
    val actionHintTextSize: TextUnit = 13.sp,
    val actionHintIncludeFontPadding: Boolean = false,
    val actionHintPaddingTop: Dp = 12.dp
) : BrazeContentCardBaseStyling(
    modifier = modifier,
    pinnedResourceId = pinnedResourceId,
    pinnedImageAlignment = pinnedImageAlignment,
    unreadIndicatorColor = unreadIndicatorColor,
    cardBackgroundColor = cardBackgroundColor,
    pinnedComposable = pinnedComposable,
    imageComposable = imageComposable,
    borderColor = borderColor,
    borderSize = borderSize,
    topBorderSize = topBorderSize,
    startBorderSize = startBorderSize,
    endBorderSize = endBorderSize,
    bottomBorderSize = bottomBorderSize,
    borderRadius = borderRadius,
    shadowColor = shadowColor,
    shadowSize = shadowSize,
    shadowRadius = shadowRadius,
    titleTextStyle = titleTextStyle,
    descriptionTextStyle = descriptionTextStyle,
    actionHintTextStyle = actionHintTextStyle,
    titleTextColor = titleTextColor,
    descriptionTextColor = descriptionTextColor,
    actionHintTextColor = actionHintTextColor
)

@Suppress("LongParameterList", "BooleanPropertyNaming")
class BrazeCaptionedImageContentCardStyling(
    modifier: Modifier? = null,
    pinnedResourceId: Int = 0,
    pinnedImageAlignment: Alignment = UndefinedAlignment,
    unreadIndicatorColor: Color = Color.Unspecified,
    cardBackgroundColor: Color = Color.Unspecified,
    pinnedComposable: @Composable (() -> Unit)? = null,
    borderColor: Color = Color.Unspecified,
    borderSize: Dp = Dp.Unspecified,
    topBorderSize: Dp = Dp.Unspecified,
    startBorderSize: Dp = Dp.Unspecified,
    endBorderSize: Dp = Dp.Unspecified,
    bottomBorderSize: Dp = Dp.Unspecified,
    borderRadius: Dp = Dp.Unspecified,
    shadowColor: Color = Color.Unspecified,
    shadowSize: Dp = Dp.Unspecified,
    shadowRadius: Dp = Dp.Unspecified,
    imageComposable: @Composable ((Card) -> Unit)? = null,
    val textColumnPaddingTop: Dp = 20.dp,
    val textColumnPaddingBottom: Dp = 25.dp,
    val textColumnPaddingStart: Dp = 25.dp,
    val textColumnPaddingEnd: Dp = 25.dp,
    titleTextStyle: TextStyle? = null,
    descriptionTextStyle: TextStyle? = null,
    actionHintTextStyle: TextStyle? = null,
    titleTextColor: Color = Color.Unspecified,
    val titleTextSize: TextUnit = 16.sp,
    val titleFontWeight: FontWeight = FontWeight.Bold,
    val titleIncludeFontPadding: Boolean = false,
    descriptionTextColor: Color = Color.Unspecified,
    val descriptionTextSize: TextUnit = 13.sp,
    val descriptionIncludeFontPadding: Boolean = false,
    val descriptionPaddingTop: Dp = 10.dp,
    val actionHintTextSize: TextUnit = 13.sp,
    actionHintTextColor: Color = Color.Unspecified,
    val actionHintIncludeFontPadding: Boolean = false,
    val actionHintPaddingTop: Dp = 12.dp
) : BrazeContentCardBaseStyling(
    modifier = modifier,
    pinnedResourceId = pinnedResourceId,
    pinnedImageAlignment = pinnedImageAlignment,
    unreadIndicatorColor = unreadIndicatorColor,
    cardBackgroundColor = cardBackgroundColor,
    pinnedComposable = pinnedComposable,
    imageComposable = imageComposable,
    borderColor = borderColor,
    borderSize = borderSize,
    topBorderSize = topBorderSize,
    startBorderSize = startBorderSize,
    endBorderSize = endBorderSize,
    bottomBorderSize = bottomBorderSize,
    borderRadius = borderRadius,
    shadowColor = shadowColor,
    shadowSize = shadowSize,
    shadowRadius = shadowRadius,
    titleTextStyle = titleTextStyle,
    descriptionTextStyle = descriptionTextStyle,
    actionHintTextStyle = actionHintTextStyle,
    titleTextColor = titleTextColor,
    descriptionTextColor = descriptionTextColor,
    actionHintTextColor = actionHintTextColor
)
