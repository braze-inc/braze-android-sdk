package com.appboy.sample.activity

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.appboy.sample.R

class GeofencesInstructionDialogFragment : DialogFragment() {

    private val instructions: String by lazy {
        getString(R.string.geofence_instructions)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.geofence_dialog_title)
            .setMessage(instructions)
            .setPositiveButton(R.string.geofence_dialog_positive_button) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }
}
