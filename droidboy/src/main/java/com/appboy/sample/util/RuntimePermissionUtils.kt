package com.appboy.sample.util

import android.app.Activity
import android.content.DialogInterface
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.appboy.sample.R

object RuntimePermissionUtils {
    /**
     * Optional rationale UI shown before launching a runtime permission request.
     * Callers pick messaging appropriate to the permission being requested.
     */
    data class Rationale(
        @StringRes val titleRes: Int,
        @StringRes val messageRes: Int,
        @DrawableRes val iconRes: Int = android.R.drawable.ic_dialog_info,
    )

    val LOCATION_RATIONALE = Rationale(
        titleRes = R.string.droidboy_required_location_prompt_title,
        messageRes = R.string.droidboy_required_location_prompt_message,
        iconRes = android.R.drawable.ic_dialog_map,
    )

    val BACKGROUND_LOCATION_RATIONALE = Rationale(
        titleRes = R.string.droidboy_required_bg_location_prompt_title,
        messageRes = R.string.droidboy_required_bg_location_prompt_message,
        iconRes = android.R.drawable.ic_dialog_map,
    )

    val LOCAL_NETWORK_RATIONALE = Rationale(
        titleRes = R.string.droidboy_required_local_network_prompt_title,
        messageRes = R.string.droidboy_required_local_network_prompt_message,
        iconRes = android.R.drawable.ic_dialog_info,
    )

    /**
     * Requests a single runtime permission, optionally showing a rationale dialog first
     * when the system indicates one is warranted.
     */
    @JvmStatic
    fun requestPermissionWithRationale(
        activity: Activity,
        permission: String,
        rationale: Rationale?,
        singlePermissionLauncher: ActivityResultLauncher<String?>
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        if (rationale != null && activity.shouldShowRequestPermissionRationale(permission)) {
            showRationaleDialog(activity, rationale) {
                singlePermissionLauncher.launch(permission)
            }
        } else {
            singlePermissionLauncher.launch(permission)
        }
    }

    /**
     * Requests a batch of runtime permissions, optionally showing a single rationale
     * dialog first when any of the permissions warrant one.
     */
    @JvmStatic
    fun requestPermissionsWithRationale(
        activity: Activity,
        permissions: Array<String>,
        rationale: Rationale?,
        multiplePermissionLauncher: ActivityResultLauncher<Array<String>>
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || permissions.isEmpty()) {
            return
        }
        val anyNeedsRationale = permissions.any { permission ->
            activity.shouldShowRequestPermissionRationale(permission)
        }
        if (rationale != null && anyNeedsRationale) {
            showRationaleDialog(activity, rationale) {
                multiplePermissionLauncher.launch(permissions)
            }
        } else {
            multiplePermissionLauncher.launch(permissions)
        }
    }

    private fun showRationaleDialog(
        activity: Activity,
        rationale: Rationale,
        onAllow: () -> Unit,
    ) {
        AlertDialog.Builder(activity)
            .setTitle(rationale.titleRes)
            .setMessage(rationale.messageRes)
            .setPositiveButton("allow") { _: DialogInterface?, _: Int -> onAllow() }
            .setNegativeButton("no", null)
            .setIcon(rationale.iconRes)
            .show()
    }
}
