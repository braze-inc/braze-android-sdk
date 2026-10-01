package com.appboy.sample.channelevents

import com.braze.events.BannersCacheSnapshot
import com.braze.events.BannersEvent
import com.braze.events.ChannelErrorReason
import com.braze.events.ContentCardsCacheSnapshot
import com.braze.events.ContentCardsEvent
import com.braze.events.ErrorReason
import com.braze.events.FeatureFlagsCacheSnapshot
import com.braze.events.FeatureFlagsEvent
import com.braze.events.RetryState

/**
 * Maps [ContentCardsEvent], [BannersEvent], and [FeatureFlagsEvent] onto [ChannelEventRow] lines.
 */
object ChannelEventRowMapper {
    fun from(
        event: ContentCardsEvent,
        timestampMillis: Long,
        userId: String? = null,
    ): ChannelEventRow =
        when (event) {
            is ContentCardsEvent.CacheReplay ->
                snapshotRow(
                    timestampMillis,
                    EventsChannel.CONTENT_CARDS,
                    "CacheReplay",
                    EventsKind.CACHE_REPLAY,
                    contentCardsSnapshot(event.cacheSnapshot, userId = userId),
                )
            is ContentCardsEvent.CacheLoad ->
                snapshotRow(
                    timestampMillis,
                    EventsChannel.CONTENT_CARDS,
                    "CacheLoad",
                    EventsKind.CACHE_LOAD,
                    contentCardsSnapshot(event.cacheSnapshot, userId = userId),
                )
            is ContentCardsEvent.DataUpdated ->
                snapshotRow(
                    timestampMillis,
                    EventsChannel.CONTENT_CARDS,
                    "DataUpdated",
                    EventsKind.DATA_UPDATED,
                    contentCardsSnapshot(event.cacheSnapshot, event.reason.name, userId),
                )
            is ContentCardsEvent.ImpressionEvent ->
                analyticsRow(
                    timestampMillis,
                    EventsChannel.CONTENT_CARDS,
                    "ImpressionEvent",
                    AnalyticsSummary(event.action.name, event.card.id),
                )
            is ContentCardsEvent.ClickEvent ->
                analyticsRow(
                    timestampMillis,
                    EventsChannel.CONTENT_CARDS,
                    "ClickEvent",
                    AnalyticsSummary(event.action.name, event.card.id),
                )
            is ContentCardsEvent.DismissEvent ->
                analyticsRow(
                    timestampMillis,
                    EventsChannel.CONTENT_CARDS,
                    "DismissEvent",
                    AnalyticsSummary(event.action.name, event.card.id),
                )
            is ContentCardsEvent.ErrorEvent ->
                errorRow(
                    timestampMillis,
                    EventsChannel.CONTENT_CARDS,
                    errorLabel((event.reason as? ChannelErrorReason.Common)?.reason),
                    event.retryState,
                    errorDetailLines((event.reason as? ChannelErrorReason.Common)?.reason),
                )
        }

    fun from(
        event: BannersEvent,
        timestampMillis: Long,
        userId: String? = null,
    ): ChannelEventRow =
        when (event) {
            is BannersEvent.CacheReplay ->
                snapshotRow(
                    timestampMillis,
                    EventsChannel.BANNERS,
                    "CacheReplay",
                    EventsKind.CACHE_REPLAY,
                    bannersSnapshot(event.cacheSnapshot, userId = userId),
                )
            is BannersEvent.CacheLoad ->
                snapshotRow(
                    timestampMillis,
                    EventsChannel.BANNERS,
                    "CacheLoad",
                    EventsKind.CACHE_LOAD,
                    bannersSnapshot(event.cacheSnapshot, userId = userId),
                )
            is BannersEvent.DataUpdated ->
                snapshotRow(
                    timestampMillis,
                    EventsChannel.BANNERS,
                    "DataUpdated",
                    EventsKind.DATA_UPDATED,
                    bannersSnapshot(event.cacheSnapshot, event.reason.name, userId),
                )
            is BannersEvent.ImpressionEvent ->
                analyticsRow(
                    timestampMillis,
                    EventsChannel.BANNERS,
                    "ImpressionEvent",
                    AnalyticsSummary(event.action.name, event.banner.placementId, usesPlacementId = true),
                )
            is BannersEvent.ClickEvent ->
                analyticsRow(
                    timestampMillis,
                    EventsChannel.BANNERS,
                    "ClickEvent",
                    AnalyticsSummary(
                        event.action.name,
                        event.banner.placementId,
                        event.buttonId,
                        usesPlacementId = true,
                    ),
                )
            is BannersEvent.DismissEvent ->
                analyticsRow(
                    timestampMillis,
                    EventsChannel.BANNERS,
                    "DismissEvent",
                    AnalyticsSummary(event.action.name, event.banner.placementId, usesPlacementId = true),
                )
            is BannersEvent.ErrorEvent ->
                errorRow(
                    timestampMillis,
                    EventsChannel.BANNERS,
                    errorLabel((event.reason as? ChannelErrorReason.Common)?.reason),
                    event.retryState,
                    errorDetailLines((event.reason as? ChannelErrorReason.Common)?.reason),
                )
        }

    fun from(
        event: FeatureFlagsEvent,
        timestampMillis: Long,
        userId: String? = null,
    ): ChannelEventRow =
        when (event) {
            is FeatureFlagsEvent.CacheReplay ->
                snapshotRow(
                    timestampMillis,
                    EventsChannel.FEATURE_FLAGS,
                    "CacheReplay",
                    EventsKind.CACHE_REPLAY,
                    featureFlagsSnapshot(event.cacheSnapshot, userId = userId),
                )
            is FeatureFlagsEvent.CacheLoad ->
                snapshotRow(
                    timestampMillis,
                    EventsChannel.FEATURE_FLAGS,
                    "CacheLoad",
                    EventsKind.CACHE_LOAD,
                    featureFlagsSnapshot(event.cacheSnapshot, userId = userId),
                )
            is FeatureFlagsEvent.DataUpdated ->
                snapshotRow(
                    timestampMillis,
                    EventsChannel.FEATURE_FLAGS,
                    "DataUpdated",
                    EventsKind.DATA_UPDATED,
                    featureFlagsSnapshot(event.cacheSnapshot, event.reason.name, userId),
                )
            is FeatureFlagsEvent.ImpressionEvent ->
                analyticsRow(
                    timestampMillis,
                    EventsChannel.FEATURE_FLAGS,
                    "ImpressionEvent",
                    AnalyticsSummary(event.action.name, event.flag.id),
                )
            is FeatureFlagsEvent.ErrorEvent ->
                errorRow(
                    timestampMillis,
                    EventsChannel.FEATURE_FLAGS,
                    errorLabel((event.reason as? ChannelErrorReason.Common)?.reason),
                    event.retryState,
                    errorDetailLines((event.reason as? ChannelErrorReason.Common)?.reason),
                )
        }

    private fun snapshotRow(
        timestampMillis: Long,
        channel: EventsChannel,
        eventType: String,
        kind: EventsKind,
        snapshot: SnapshotView,
    ): ChannelEventRow {
        val countText = countLabel(snapshot.size, snapshot.singular, snapshot.plural)
        val lines =
            mutableListOf(
                "Channel: ${channel.displayName}",
                "Event: $eventType",
            )
        if (snapshot.reasonName != null) {
            lines.add("reason: ${snapshot.reasonName}")
        }
        if (snapshot.userId != null) {
            lines.add("userId: ${snapshot.userId}")
        }
        val listedIds = snapshot.itemIds.takeIf { it.isNotEmpty() }?.joinToString(", ")
        if (listedIds != null) {
            val idLabel = if (snapshot.usesPlacementId) "placementId" else "id"
            lines.add("$idLabel: $listedIds")
        }
        lines.add("${snapshot.heading}: ${snapshot.size}")
        lines.add("lastSyncAt: ${snapshot.lastSyncAt}")
        return ChannelEventRow(
            timestampMillis,
            channel,
            eventType,
            countText,
            lines.joinToString("\n"),
            kind,
            reason = snapshot.reasonName,
            id = if (snapshot.usesPlacementId) null else listedIds,
            placementId = if (snapshot.usesPlacementId) listedIds else null,
            userId = snapshot.userId,
        )
    }

    private fun contentCardsSnapshot(
        snapshot: ContentCardsCacheSnapshot,
        reasonName: String? = null,
        userId: String? = null,
    ): SnapshotView =
        SnapshotView(
            snapshot.cards.size,
            "card",
            "cards",
            "Cards",
            snapshot.lastSyncAt,
            reasonName,
            userId,
            snapshot.cards.map { it.id },
        )

    private fun bannersSnapshot(
        snapshot: BannersCacheSnapshot,
        reasonName: String? = null,
        userId: String? = null,
    ): SnapshotView =
        SnapshotView(
            snapshot.banners.size,
            "banner",
            "banners",
            "Banners",
            snapshot.lastSyncAt,
            reasonName,
            userId,
            snapshot.banners.values.map { it.placementId },
            usesPlacementId = true,
        )

    private fun featureFlagsSnapshot(
        snapshot: FeatureFlagsCacheSnapshot,
        reasonName: String? = null,
        userId: String? = null,
    ): SnapshotView =
        SnapshotView(
            snapshot.featureFlags.size,
            "flag",
            "flags",
            "Feature Flags",
            snapshot.lastSyncAt,
            reasonName,
            userId,
            snapshot.featureFlags.map { it.id },
        )

    private fun analyticsRow(
        timestampMillis: Long,
        channel: EventsChannel,
        eventType: String,
        summary: AnalyticsSummary,
    ): ChannelEventRow {
        val buttonId = summary.buttonId?.takeIf { it.isNotBlank() }
        val lines =
            mutableListOf(
                "Channel: ${channel.displayName}",
                "Event: $eventType",
                "action: ${summary.action}",
            )
        if (summary.usesPlacementId) {
            lines.add("placementId: ${summary.id}")
        } else {
            lines.add("id: ${summary.id}")
        }
        if (buttonId != null) {
            lines.add("buttonId: $buttonId")
        }
        return ChannelEventRow(
            timestampMillis = timestampMillis,
            channel = channel,
            eventType = eventType,
            summary = "",
            detailText = lines.joinToString("\n"),
            kind = EventsKind.ANALYTICS,
            action = summary.action,
            id = if (summary.usesPlacementId) null else summary.id,
            placementId = if (summary.usesPlacementId) summary.id else null,
            buttonId = buttonId,
        )
    }

    private fun errorRow(
        timestampMillis: Long,
        channel: EventsChannel,
        reasonLabel: String,
        retryState: RetryState,
        extraDetailLines: List<String>,
    ): ChannelEventRow =
        ChannelEventRow(
            timestampMillis = timestampMillis,
            channel = channel,
            eventType = "ErrorEvent",
            summary = "",
            detailText =
                detail(
                    "Channel: ${channel.displayName}",
                    "Event: ErrorEvent",
                    "reason: $reasonLabel",
                    "retryState: ${retryState.name}",
                    *extraDetailLines.toTypedArray(),
                ),
            kind = EventsKind.ERROR,
            reason = reasonLabel,
            retryState = retryState.name,
        )

    private fun errorLabel(commonReason: ErrorReason?): String = commonReason?.let(::commonErrorLabel) ?: "FeatureDisabled"

    private fun commonErrorLabel(reason: ErrorReason): String =
        when (reason) {
            ErrorReason.ServerError -> "ServerError"
            ErrorReason.ClientError -> "ClientError"
            is ErrorReason.RateLimited -> "RateLimited"
            ErrorReason.SdkDisabled -> "SdkDisabled"
            ErrorReason.InvalidServerData -> "InvalidServerData"
        }

    private fun errorDetailLines(commonReason: ErrorReason?): List<String> =
        if (commonReason is ErrorReason.RateLimited) {
            listOf("Until: ${commonReason.until.time}")
        } else {
            emptyList()
        }

    private fun countLabel(
        count: Int,
        singular: String,
        plural: String,
    ): String = if (count == 1) "1 $singular" else "$count $plural"

    private fun detail(vararg lines: String): String = lines.joinToString("\n")
}

private data class SnapshotView(
    val size: Int,
    val singular: String,
    val plural: String,
    val heading: String,
    val lastSyncAt: Long?,
    val reasonName: String? = null,
    val userId: String? = null,
    val itemIds: List<String> = emptyList(),
    val usesPlacementId: Boolean = false,
)

private data class AnalyticsSummary(
    val action: String,
    val id: String,
    val buttonId: String? = null,
    val usesPlacementId: Boolean = false,
)
