@file:JvmName("SystemAnimationDurations")

package com.braze.ui.support

import android.content.res.Resources
import com.braze.support.BrazeLogger.Priority.W
import com.braze.support.BrazeLogger.brazelog
import com.braze.support.BrazeLogger.getBrazeLogTag

private val TAG = "SystemAnimationDurations".getBrazeLogTag()

/**
 * AOSP default for `config_shortAnimTime`.
 * See https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/res/res/values/config.xml
 */
private const val DEFAULT_SHORT_ANIMATION_DURATION_MS = 200L

/**
 * Reads `android.R.integer.config_shortAnimTime` from [resources].
 *
 * Some OEM builds don't expose this resource, so a missing value falls back to the AOSP default of 200ms
 * instead of throwing.
 *
 * @param resources The resources to read the duration from.
 * @return The short animation duration in milliseconds.
 */
internal fun getShortAnimationDurationMs(resources: Resources): Long =
    try {
        resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
    } catch (e: Resources.NotFoundException) {
        brazelog(TAG, W, e) {
            "config_shortAnimTime not found. Using default of ${DEFAULT_SHORT_ANIMATION_DURATION_MS}ms."
        }
        DEFAULT_SHORT_ANIMATION_DURATION_MS
    }
