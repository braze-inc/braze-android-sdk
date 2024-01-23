package com.appboy.sample.featureflag.controller

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.appboy.sample.R
import com.braze.Braze
import com.braze.models.FeatureFlag
import com.braze.support.getPrettyPrintedString

class FeatureFlagAdapter(
    private val featureFlags: MutableList<FeatureFlag>
) : RecyclerView.Adapter<FeatureFlagAdapter.ViewHolder>() {
    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var areDetailsVisible = false
        val tvIdentifier: TextView
        val tvInfo: TextView
        val tvTracking: TextView
        val tvPropertyDetails: TextView

        init {
            // Define click listener for the ViewHolder's View
            tvIdentifier = view.findViewById(R.id.tvFlagId)
            tvInfo = view.findViewById(R.id.tvFlagInfo)
            tvTracking = view.findViewById(R.id.tvFlagTrackingInfo)
            tvPropertyDetails = view.findViewById(R.id.tvFlagProperties)

            itemView.setOnClickListener {
                areDetailsVisible = !areDetailsVisible
                tvTracking.visibility = if (areDetailsVisible) View.VISIBLE else View.GONE
                tvPropertyDetails.visibility = if (areDetailsVisible) View.VISIBLE else View.GONE
                Braze.getInstance(itemView.context).logFeatureFlagImpression(tvIdentifier.text.toString())
            }
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.feature_flag_overview_item, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        val featureFlag = featureFlags[position]
        viewHolder.run {
            tvIdentifier.text = featureFlag.id
            var trackingId = featureFlag.forJsonPut().optString("fts")
            var infoText = ""
            if (trackingId.isNotEmpty()) {
                infoText += "[T] "
            } else {
                trackingId = "None"
            }
            infoText += if (featureFlag.enabled) "ON" else "OFF"
            infoText += " ${featureFlag.properties.length()}"

            tvInfo.text = infoText
            tvTracking.text = "Tracking ID: $trackingId"
            tvPropertyDetails.text = featureFlag.properties.getPrettyPrintedString()
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = featureFlags.size

    fun replaceFeatureFlags(newFlagData: List<FeatureFlag>) {
        val diffCallback = DiffCallback(featureFlags, newFlagData)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        featureFlags.clear()
        featureFlags.addAll(newFlagData)
        diffResult.dispatchUpdatesTo(this)
    }

    private class DiffCallback(
        private val oldFlags: List<FeatureFlag>,
        private val newFlags: List<FeatureFlag>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() =
            oldFlags.size

        override fun getNewListSize() =
            newFlags.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            doItemsShareIds(oldItemPosition, newItemPosition)

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldFlags[oldItemPosition].forJsonPut() == newFlags[newItemPosition].forJsonPut()

        private fun doItemsShareIds(oldItemPosition: Int, newItemPosition: Int) =
            oldFlags[oldItemPosition].id == newFlags[newItemPosition].id
    }
}
