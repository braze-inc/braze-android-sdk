package com.appboy.sample.activity

import android.content.Context
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import com.appboy.sample.R
import kotlin.math.max

internal object EdgeToEdgeHtmlIamInsets {
    fun resolveIamWindowInsets(
        context: Context,
        windowInsets: WindowInsetsCompat,
    ): WindowInsetsCompat {
        if (EdgeToEdgeHtmlIamDemoSettings.forceZeroIamInsets) {
            return zeroSystemBarInsets(windowInsets)
        }
        if (!EdgeToEdgeHtmlIamDemoSettings.useDemoHorizontalInsets) {
            return windowInsets
        }
        return augmentWindowInsetsForDemo(context, windowInsets)
    }

    fun getResolvedSystemBarInsets(
        context: Context,
        windowInsets: WindowInsetsCompat,
    ): Insets =
        resolveIamWindowInsets(context, windowInsets)
            .getInsets(WindowInsetsCompat.Type.systemBars())

    private fun zeroSystemBarInsets(windowInsets: WindowInsetsCompat): WindowInsetsCompat =
        WindowInsetsCompat
            .Builder(windowInsets)
            .setInsets(
                WindowInsetsCompat.Type.systemBars(),
                Insets.NONE,
            ).build()

    private fun augmentWindowInsetsForDemo(
        context: Context,
        windowInsets: WindowInsetsCompat,
    ): WindowInsetsCompat {
        val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        val demoHorizontalInset =
            context.resources.getDimensionPixelSize(R.dimen.edge_to_edge_host_side_indicator_width)
        return WindowInsetsCompat
            .Builder(windowInsets)
            .setInsets(
                WindowInsetsCompat.Type.systemBars(),
                Insets.of(
                    max(systemBars.left, demoHorizontalInset),
                    systemBars.top,
                    max(systemBars.right, demoHorizontalInset),
                    systemBars.bottom,
                ),
            ).build()
    }
}
