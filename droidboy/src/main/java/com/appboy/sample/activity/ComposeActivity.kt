package com.appboy.sample.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braze.enums.CardType
import com.braze.jetpackcompose.BrazeStyle
import com.braze.jetpackcompose.contentcards.ContentCardsList
import com.braze.jetpackcompose.contentcards.styling.ContentCardStyling
import com.braze.jetpackcompose.contentcards.styling.BrazeTextAnnouncementContentCardStyling
import com.braze.models.cards.Card
import com.braze.models.cards.TextAnnouncementCard
import com.braze.support.BrazeLogger.brazelog

class ComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ContentCardsList(
                onCardDismissed = { card ->
                    brazelog { "Card ${card.id} was dismissed." }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
val myCustomCardRenderer: @Composable ((Card) -> Boolean) = { card ->
    if (card.cardType == CardType.TEXT_ANNOUNCEMENT) {
        val textCard = card as TextAnnouncementCard
        Box(
            Modifier
                .padding(10.dp)
                .fillMaxWidth()
                .background(color = Color.Red)
        ) {
            Text(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .basicMarquee(iterations = Int.MAX_VALUE),
                fontSize = 35.sp,
                text = textCard.description
            )
        }
        true
    } else {
        false
    }
}

@Composable
@Suppress("UnusedPrivateMember")
private fun GetMyBrazeStyle(content: @Composable () -> Unit) {
    BrazeStyle(
        contentCardStyle = ContentCardStyling(
            cardBackgroundColor = Color.Blue,
            textAnnouncementContentCardStyle = BrazeTextAnnouncementContentCardStyling(
                cardBackgroundColor = Color.Red
            ),
        ),
        content = content
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
@Suppress("UnusedPrivateMember")
private fun GetMyOtherBrazeStyle(content: @Composable () -> Unit) {
    BrazeStyle(
        contentCardStyle = ContentCardStyling(
            pinnedComposable = {
                Box(Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(50.dp)
                            .basicMarquee(iterations = Int.MAX_VALUE),
                        text = "This message is not read. Please read it."
                    )
                }
            },
            cardBackgroundColor = Color.Red
        ),
        content = content
    )
}
