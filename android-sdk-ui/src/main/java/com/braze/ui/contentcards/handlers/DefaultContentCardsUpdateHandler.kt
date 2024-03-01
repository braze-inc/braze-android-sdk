package com.braze.ui.contentcards.handlers

import android.os.Parcel
import android.os.Parcelable
import com.braze.events.ContentCardsUpdatedEvent
import com.braze.models.cards.Card
import com.braze.ui.contentcards.BrazeContentCardUtils

open class DefaultContentCardsUpdateHandler : IContentCardsUpdateHandler {
    override fun handleCardUpdate(event: ContentCardsUpdatedEvent): List<Card> =
        BrazeContentCardUtils.defaultCardHandling(event.allCards)

    // Parcelable interface method
    override fun describeContents() = 0

    // Parcelable interface method
    override fun writeToParcel(dest: Parcel, flags: Int) {
        // No state is kept in this class so the parcel is left unmodified
    }

    companion object {
        // Interface that must be implemented and provided as a public CREATOR
        // field that generates instances of your Parcelable class from a Parcel.
        @JvmField
        val CREATOR: Parcelable.Creator<DefaultContentCardsUpdateHandler> = object : Parcelable.Creator<DefaultContentCardsUpdateHandler> {
            override fun createFromParcel(source: Parcel) =
                DefaultContentCardsUpdateHandler()

            override fun newArray(size: Int): Array<DefaultContentCardsUpdateHandler?> =
                arrayOfNulls(size)
        }
    }
}
