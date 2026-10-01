package com.appboy.sample.channelevents

/**
 * One Events-tab line for a [com.braze.events.ContentCardsEvent], [com.braze.events.BannersEvent],
 * or [com.braze.events.FeatureFlagsEvent].
 *
 * @property timestampMillis Event arrival time on the sample-app clock.
 * @property channel Content Cards, Banners, or Feature Flags.
 * @property eventType SDK sealed-subtype name, such as `DataUpdated` or `ImpressionEvent`.
 * @property summary Short count text under the labeled lines.
 * @property detailText Full text for the tap dialog.
 * @property kind Used to color the event type.
 * @property reason [com.braze.events.ChannelUpdateReason] or error reason name.
 * @property retryState [com.braze.events.RetryState] name.
 * @property action [com.braze.events.AnalyticsAction] name.
 * @property id Card id, Feature Flag id, or the comma-separated ids from a cache snapshot.
 * @property placementId Banner placement id, or the comma-separated placement ids from a cache snapshot.
 * @property buttonId Banner click button id.
 * @property userId Active user id stamped on cache snapshots.
 */
data class ChannelEventRow(
    val timestampMillis: Long,
    val channel: EventsChannel,
    val eventType: String,
    val summary: String,
    val detailText: String,
    val kind: EventsKind,
    val reason: String? = null,
    val retryState: String? = null,
    val action: String? = null,
    val id: String? = null,
    val placementId: String? = null,
    val buttonId: String? = null,
    val userId: String? = null,
)

enum class EventsChannel(
    val displayName: String,
) {
    CONTENT_CARDS("Content Cards"),
    BANNERS("Banners"),
    FEATURE_FLAGS("Feature Flags"),
}

enum class EventsKind {
    CACHE_REPLAY,
    CACHE_LOAD,
    DATA_UPDATED,
    ANALYTICS,
    ERROR,
}
