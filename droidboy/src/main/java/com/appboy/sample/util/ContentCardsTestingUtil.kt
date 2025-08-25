package com.appboy.sample.util

import com.braze.enums.CardKey
import com.braze.enums.CardType
import com.braze.models.cards.Card
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("UnsafeCallOnNullableType")
class ContentCardsTestingUtil private constructor() {
    companion object {
        /**
         * https://effigis.com/en/solutions/satellite-images/satellite-image-samples/
         */
        private val SUPER_HIGH_RESOLUTION_IMAGES = listOf(
            "https://images.unsplash.com/photo-1543314444-26a64fa5efe1"
        )
        private val random = Random()

        fun createRandomCards(numCardsOfEachType: Int): List<Card> {
            val cards = mutableListOf<Card>()

            for (cardType in CardType.entries) {
                if (cardType == CardType.DEFAULT) {
                    continue
                }
                repeat((0..numCardsOfEachType).count()) {
                    createRandomCard(cardType).let { card -> cards.add(card) }
                }
            }

            cards.shuffle()
            return cards
        }

        fun getRemovedCardJson(id: String): JSONObject {
            return JSONObject(
                mapOf(
                    CardKey.ID.key to id,
                    CardKey.REMOVED.key to true,
                )
            )
        }

        fun createCaptionedImageCardJson(
            id: String,
            title: String,
            description: String,
            imageUrl: String,
            altImageText: String
        ): JSONObject {
            // Get the default fields
            val defaultMapping = getDefaultCardFields(CardType.CAPTIONED_IMAGE)

            defaultMapping.mergeWith(
                mapOf(
                    CardKey.ID.key to id,
                    CardKey.CAPTIONED_IMAGE_IMAGE.key to imageUrl,
                    CardKey.CAPTIONED_IMAGE_ALT_IMAGE.key to altImageText,
                    CardKey.CAPTIONED_IMAGE_ASPECT_RATIO.key to 1.0,
                    CardKey.CAPTIONED_IMAGE_TITLE.key to title,
                    CardKey.CAPTIONED_IMAGE_DESCRIPTION.key to description,
                    CardKey.PINNED.key to true,
                    CardKey.DISMISSIBLE.key to false,
                    CardKey.CREATED.key to System.currentTimeMillis()
                )
            )
            return JSONObject(defaultMapping.toMap())
        }

        private fun getDefaultCardFields(cardType: CardType): MutableMap<String, Any> = mutableMapOf(
            CardKey.ID.key to getRandomString(),
            CardKey.TYPE.key to CardKey.getServerKeyFromCardType(cardType)!!,
            CardKey.VIEWED.key to getRandomBoolean(),
            CardKey.CREATED.key to getNow(),
            CardKey.EXPIRES_AT.key to getNowPlusDelta(TimeUnit.DAYS, 30),
            CardKey.OPEN_URI_IN_WEBVIEW.key to getRandomBoolean(),
            CardKey.DISMISSED.key to false,
            CardKey.REMOVED.key to false,
            CardKey.PINNED.key to getRandomBoolean(),
            CardKey.DISMISSIBLE.key to getRandomBoolean(),
            CardKey.IS_TEST.key to true
        )

        private fun createRandomCard(cardType: CardType): Card {
            // Set the default fields
            val defaultMapping = getDefaultCardFields(cardType)

            // Based on the card type, add new fields
            val title = "Title"
            val description = "Description -> cardType $cardType"
            val randomImage = getRandomImageUrl()
            val altImageText = getRandomString()

            when (cardType) {
                CardType.IMAGE -> {
                    defaultMapping.mergeWith(
                        mapOf(
                            CardKey.IMAGE_ONLY_IMAGE.key to randomImage.first,
                            CardKey.IMAGE_ONLY_ALT_IMAGE.key to altImageText,
                            CardKey.IMAGE_ONLY_ASPECT_RATIO.key to randomImage.second,
                            CardKey.IMAGE_ONLY_URL.key to randomImage.first
                        )
                    )
                }

                CardType.CAPTIONED_IMAGE -> {
                    defaultMapping.mergeWith(
                        mapOf(
                            CardKey.CAPTIONED_IMAGE_IMAGE.key to randomImage.first,
                            CardKey.CAPTIONED_IMAGE_ALT_IMAGE.key to altImageText,
                            CardKey.CAPTIONED_IMAGE_ASPECT_RATIO.key to randomImage.second,
                            CardKey.CAPTIONED_IMAGE_TITLE.key to title,
                            CardKey.CAPTIONED_IMAGE_DESCRIPTION.key to description,
                            CardKey.CAPTIONED_IMAGE_URL.key to randomImage.first
                        )
                    )
                }

                CardType.SHORT_NEWS -> {
                    defaultMapping.mergeWith(
                        mapOf(
                            CardKey.SHORT_NEWS_IMAGE.key to randomImage.first,
                            CardKey.SHORT_NEWS_ALT_IMAGE.key to altImageText,
                            CardKey.SHORT_NEWS_TITLE.key to title,
                            CardKey.SHORT_NEWS_DESCRIPTION.key to description,
                            CardKey.SHORT_NEWS_URL.key to randomImage.first
                        )
                    )
                }

                CardType.TEXT_ANNOUNCEMENT -> {
                    defaultMapping.mergeWith(
                        mapOf(
                            CardKey.TEXT_ANNOUNCEMENT_DESCRIPTION.key to description,
                            CardKey.TEXT_ANNOUNCEMENT_URL.key to randomImage.first,
                            CardKey.TEXT_ANNOUNCEMENT_TITLE.key to title,
                            CardKey.TEXT_ANNOUNCEMENT_URL.key to randomImage.first
                        )
                    )
                }

                else -> {
                    // Do nothing!
                }
            }

            val json = JSONObject(defaultMapping.toMap())
            return Card(json)
        }

        private fun getRandomString(): String = UUID.randomUUID().toString()

        private fun getRandomBoolean(): Boolean = random.nextBoolean()

        // Get now plus some random delta a minute into the future
        private fun getNow(): Long = getNowPlusDelta(TimeUnit.MILLISECONDS, random.nextInt(60000).toLong())

        private fun getNowPlusDelta(deltaUnits: TimeUnit, delta: Long): Long = System.currentTimeMillis() + deltaUnits.toMillis(delta)

        /**
         * @return Pair of url to aspect ratio
         */
        private fun getRandomImageUrl(): Pair<String, Double> {
            return if (random.nextInt(100) < 40) {
                // Return a SUPER high resolution image
                val url = "${SUPER_HIGH_RESOLUTION_IMAGES.shuffled(random).first()}?q=${System.nanoTime()}"
                Pair(url, 1.0)
            } else {
                val height = random.nextInt(500) + 200
                val width = random.nextInt(500) + 200
                Pair("https://picsum.photos/seed/${System.nanoTime()}/$width/$height", width.toDouble() / height.toDouble())
            }
        }

        /**
         * Merges the content of a target map with another map
         */
        private fun MutableMap<String, Any>.mergeWith(another: Map<String, Any>) {
            for ((key, value) in another) {
                this[key] = value
            }
        }
    }
}
