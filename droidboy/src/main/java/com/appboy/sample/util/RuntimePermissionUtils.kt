package com.appboy.sample.util

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import com.appboy.sample.R

object RuntimePermissionUtils {
    /**
     * If needed, shows a "here's why we need location permissions" prompt
     * before actually requesting a single location permission.
     */
    @JvmStatic
    fun requestLocationPermission(
        activity: Activity,
        permission: String,
        singlePermissionLauncher: ActivityResultLauncher<String?>
    ) {
        if (activity.shouldShowRequestPermissionRationale(permission)) {
            AlertDialog.Builder(activity)
                .setTitle(
                    if (Manifest.permission.ACCESS_BACKGROUND_LOCATION == permission) {
                        R.string.droidboy_required_bg_location_prompt_title
                    } else {
                        R.string.droidboy_required_location_prompt_title
                    }
                )
                .setMessage(
                    if (Manifest.permission.ACCESS_BACKGROUND_LOCATION == permission) {
                        R.string.droidboy_required_bg_location_prompt_message
                    } else {
                        R.string.droidboy_required_location_prompt_message
                    }
                )
                .setPositiveButton("allow") { _: DialogInterface?, _: Int ->
                    singlePermissionLauncher.launch(
                        permission
                    )
                }
                .setNegativeButton("no", null)
                .setIcon(android.R.drawable.ic_dialog_map)
                .show()
        } else {
            singlePermissionLauncher.launch(permission)
        }
    }

    /**
     * If needed, shows a "here's why we need location permissions" prompt
     * before actually requesting the set of location permissions.
     */
    @JvmStatic
    fun requestLocationPermissions(
        activity: Activity,
        permissions: Array<String>,
        multiplePermissionLauncher: ActivityResultLauncher<Array<String>>
    ) {
        if (permissions.size <= 1) {
            return
        }
        var isExplanationNeeded = false
        for (permission in permissions) {
            if (activity.shouldShowRequestPermissionRationale(permission)) {
                isExplanationNeeded = true
                break
            }
        }
        if (isExplanationNeeded) {
            AlertDialog.Builder(activity)
                .setTitle(R.string.droidboy_required_location_prompt_title)
                .setMessage(R.string.droidboy_required_location_prompt_message)
                .setPositiveButton("allow") { _: DialogInterface?, _: Int ->
                    multiplePermissionLauncher.launch(
                        permissions
                    )
                }
                .setNegativeButton("no", null)
                .setIcon(android.R.drawable.ic_dialog_map)
                .show()
        } else {
            multiplePermissionLauncher.launch(permissions)
        }
    }
}
