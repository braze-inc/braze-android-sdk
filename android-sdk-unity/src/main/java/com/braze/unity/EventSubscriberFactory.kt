package com.braze.unity

import com.braze.unity.configuration.UnityConfigurationProvider
import com.braze.enums.BrazePushEventType
import com.braze.events.BrazePushEvent
import com.braze.events.BrazeSdkAuthenticationErrorEvent
import com.braze.events.ContentCardsUpdatedEvent
import com.braze.events.FeatureFlagsUpdatedEvent
import com.braze.events.IEventSubscriber
import com.braze.events.InAppMessageEvent
import com.braze.support.BrazeLogger.brazelog
import com.braze.support.BrazeLogger.getBrazeLogTag
import com.braze.unity.utils.MessagingUtils.sendContentCardsUpdatedEventToUnity
import com.braze.unity.utils.MessagingUtils.sendFeatureFlagsUpdatedEventToUnity
import com.braze.unity.utils.MessagingUtils.sendInAppMessageReceivedMessage
import com.braze.unity.utils.MessagingUtils.sendPushEventToUnity
import com.braze.unity.utils.MessagingUtils.sendSdkAuthErrorEventToUnity

/**
 * Factory for creating [IEventSubscriber] instances that forward Braze SDK
 * events to the Unity player via [MessagingUtils].
 */
object EventSubscriberFactory {
    private val TAG = getBrazeLogTag(EventSubscriberFactory::class.java)

    /**
     * Creates a subscriber that forwards [InAppMessageEvent] payloads to Unity.
     *
     * @param config Unity configuration providing the target GameObject and method names.
     * @return An [IEventSubscriber] for in-app message events.
     */
    fun createInAppMessageEventSubscriber(config: UnityConfigurationProvider): IEventSubscriber<InAppMessageEvent> {
        return IEventSubscriber { inAppMessageEvent: InAppMessageEvent ->
            val isInAppMessageEventSent =
                sendInAppMessageReceivedMessage(
                    config.inAppMessageListenerGameObjectName,
                    config.inAppMessageListenerCallbackMethodName,
                    inAppMessageEvent.inAppMessage
                )
            brazelog(TAG) { "Did send in-app message event to Unity Player?: $isInAppMessageEventSent" }
        }
    }

    /**
     * Creates a subscriber that forwards [ContentCardsUpdatedEvent] payloads to Unity.
     *
     * @param config Unity configuration providing the target GameObject and method names.
     * @return An [IEventSubscriber] for Content Cards updated events.
     */
    fun createContentCardsEventSubscriber(config: UnityConfigurationProvider): IEventSubscriber<ContentCardsUpdatedEvent> {
        return IEventSubscriber { contentCardsUpdatedEvent: ContentCardsUpdatedEvent ->
            val isContentCardsEventSent =
                sendContentCardsUpdatedEventToUnity(
                    config.contentCardsUpdatedListenerGameObjectName,
                    config.contentCardsUpdatedListenerCallbackMethodName,
                    contentCardsUpdatedEvent
                )
            brazelog(TAG) { "Did send Content Cards updated event to Unity Player?: $isContentCardsEventSent" }
        }
    }

    /**
     * Creates a subscriber that forwards [FeatureFlagsUpdatedEvent] payloads to Unity.
     *
     * @param config Unity configuration providing the target GameObject and method names.
     * @return An [IEventSubscriber] for Feature Flags updated events.
     */
    fun createFeatureFlagsEventSubscriber(config: UnityConfigurationProvider): IEventSubscriber<FeatureFlagsUpdatedEvent> {
        return IEventSubscriber { featureFlagsUpdatedEvent: FeatureFlagsUpdatedEvent ->
            val isFeatureFlagUpdatedEventSent =
                sendFeatureFlagsUpdatedEventToUnity(
                    config.featureFlagsUpdatedListenerGameObjectName,
                    config.featureFlagsUpdatedListenerCallbackMethodName,
                    featureFlagsUpdatedEvent
                )
            brazelog(TAG) { "Did send Content Cards updated event to Unity Player?: $isFeatureFlagUpdatedEventSent" }
        }
    }

    /**
     * Creates a subscriber that forwards [BrazeSdkAuthenticationErrorEvent] payloads to Unity.
     *
     * @param config Unity configuration providing the target GameObject and method names.
     * @return An [IEventSubscriber] for SDK authentication failure events.
     */
    fun createSdkAuthenticationFailureSubscriber(config: UnityConfigurationProvider): IEventSubscriber<BrazeSdkAuthenticationErrorEvent> {
        return IEventSubscriber { sdkAuthErrorEvent: BrazeSdkAuthenticationErrorEvent ->
            val isSdkAuthErrorSent =
                sendSdkAuthErrorEventToUnity(
                    config.sdkAuthenticationFailureListenerGameObjectName,
                    config.sdkAuthenticationFailureListenerCallbackMethodName,
                    sdkAuthErrorEvent
                )
            brazelog { "Did send SDK Authentication failure event to Unity Player?: $isSdkAuthErrorSent" }
        }
    }

    /**
     * Creates a subscriber that forwards [BrazePushEvent] payloads to Unity,
     * routing received, deleted, and opened events to their respective callbacks.
     *
     * @param config Unity configuration providing the target GameObject and method names.
     * @return An [IEventSubscriber] for push notification events.
     */
    fun createPushEventSubscriber(config: UnityConfigurationProvider): IEventSubscriber<BrazePushEvent> {
        return IEventSubscriber { event: BrazePushEvent ->
            val (callback, gameObject) = when (event.eventType) {
                BrazePushEventType.NOTIFICATION_RECEIVED -> Pair(config.pushReceivedCallbackMethodName, config.pushReceivedGameObjectName)
                BrazePushEventType.NOTIFICATION_DELETED -> Pair(config.pushDeletedCallbackMethodName, config.pushDeletedGameObjectName)
                BrazePushEventType.NOTIFICATION_OPENED -> Pair(config.pushOpenedCallbackMethodName, config.pushOpenedGameObjectName)
            }
            val wasMessageSent = sendPushEventToUnity(gameObject, callback, event)
            brazelog { "Did send Braze Push event to Unity Player?: $wasMessageSent \nEvent: $event" }
        }
    }
}
