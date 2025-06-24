package com.braze.ui.contentcards.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.braze.models.cards.Card
import com.braze.models.cards.ImageOnlyCard
import com.braze.ui.R

open class ImageOnlyContentCardView(context: Context) : BaseContentCardView<ImageOnlyCard>(
    context
) {
    private inner class ViewHolder(view: View) :
        ContentCardViewHolder(view, isUnreadIndicatorEnabled) {
        val imageView: ImageView? = view.findViewById(R.id.com_braze_content_cards_image_only_card_image)
    }

    override fun createViewHolder(viewGroup: ViewGroup): ContentCardViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.com_braze_image_only_content_card, viewGroup, false)
        setViewBackground(view)
        return ViewHolder(view)
    }

    override fun bindViewHolder(viewHolder: ContentCardViewHolder, card: Card) {
        if (card is ImageOnlyCard) {
            super.bindViewHolder(viewHolder, card)
            val imageOnlyViewHolder = viewHolder as ViewHolder
            setOptionalCardImage(
                imageOnlyViewHolder.imageView,
                card.aspectRatio,
                card.imageUrl,
                card.altImageText,
                card
            )
        }
    }
}
