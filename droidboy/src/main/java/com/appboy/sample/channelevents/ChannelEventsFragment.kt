package com.appboy.sample.channelevents

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.appboy.sample.DroidboyApplication
import com.appboy.sample.R
import com.braze.Braze
import com.braze.bannersEventsFlow
import com.braze.changeUserEventsFlow
import com.braze.contentCardsEventsFlow
import com.braze.featureFlagsEventsFlow
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

/**
 * Observer tab for the Content Cards, Banners, and Feature Flags event streams.
 * Product UI on the other tabs remains on the legacy APIs and still produces events here.
 *
 * Collects the Kotlin Flow APIs (for example [contentCardsEventsFlow]) in the view lifecycle scope. Cancelling a
 * collection removes its subscription, so there are no manual `removeSingleSubscription` calls.
 */
class ChannelEventsFragment : Fragment() {
    private lateinit var adapter: ChannelEventsAdapter
    private lateinit var refreshSpinner: Spinner
    private lateinit var resubscribeSpinner: Spinner

    @Volatile
    private var activeUserId: String? = null

    /**
     * [Braze] instance these collections were started on.
     * [Braze.wipeData] replaces that instance. Settings cannot see this fragment, so [onStart]
     * compares this reference and collects again.
     */
    private var subscribedBraze: Braze? = null

    private val channelCollections = mutableMapOf<EventsChannel, Job>()
    private var userChangeCollection: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.channel_events_fragment, container, false)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ChannelEventsAdapter(requireContext(), onRowClick = ::showRowDetail)
        val recycler = view.findViewById<RecyclerView>(R.id.channel_events_recycler)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        bindChannelChips(view.findViewById(R.id.channel_events_channel_chips))
        refreshSpinner = bindChannelSpinner(view.findViewById(R.id.channel_events_refresh_spinner))
        resubscribeSpinner = bindChannelSpinner(view.findViewById(R.id.channel_events_resubscribe_spinner))
        bindHttpResponseSpinner(view.findViewById(R.id.channel_events_http_response_spinner))
        view.findViewById<Button>(R.id.channel_events_refresh_button).setOnClickListener {
            refresh(selectedChannel(refreshSpinner))
        }
        view.findViewById<Button>(R.id.channel_events_resubscribe_button).setOnClickListener {
            resubscribe(selectedChannel(resubscribeSpinner))
        }
        view.findViewById<Button>(R.id.channel_events_clear_button).setOnClickListener {
            adapter.clearRows()
        }
        view.findViewById<Button>(R.id.channel_events_local_fixtures_button).setOnClickListener {
            LocalChannelFixtures.insert(requireContext())
            Toast
                .makeText(
                    requireContext(),
                    getString(
                        R.string.channel_events_local_fixtures_added,
                        LocalChannelFixtures.CARD_ID,
                    ),
                    Toast.LENGTH_LONG,
                ).show()
        }
        activeUserId = Braze.getInstance(requireContext()).currentUser?.userId
        subscribeAll()
    }

    override fun onStart() {
        super.onStart()
        val current = Braze.getInstance(requireContext())
        if (subscribedBraze != null && subscribedBraze !== current) {
            resubscribeAfterInstanceReset()
        }
    }

    /**
     * The HTTP dropdown and Push Unregister QA share one process-wide connector slot.
     * That run replaces the slot and then clears it. This page stays alive in the pager, so a canned
     * dropdown selection is installed again when this page is shown.
     * [ChannelSyncHttpResponse.REAL] is left alone so opening this tab does not remove a Push Unregister
     * connector that is still retrying.
     */
    override fun onResume() {
        super.onResume()
        val spinner = view?.findViewById<Spinner>(R.id.channel_events_http_response_spinner) ?: return
        val selection = ChannelSyncHttpResponse.entries[spinner.selectedItemPosition]
        if (selection == ChannelSyncHttpResponse.REAL) return
        ChannelSyncHttpConnector.install(selection)
    }

    /**
     * Collects this page's Flows from the [Braze] instance created after [Braze.wipeData].
     * The previous instance's messenger is stopped, so every collection is restarted on the new instance.
     */
    internal fun resubscribeAfterInstanceReset() {
        if (!isAdded || view == null) return
        val braze = Braze.getInstance(requireContext())
        activeUserId = braze.currentUser?.userId
        subscribeAll()
    }

    override fun onDestroyView() {
        ChannelSyncHttpConnector.install(ChannelSyncHttpResponse.REAL)
        super.onDestroyView()
    }

    private fun bindChannelChips(chipGroup: ChipGroup) {
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.channel_events_chip_all
            adapter.channelFilter =
                when (checkedId) {
                    R.id.channel_events_chip_content_cards -> EventsChannel.CONTENT_CARDS
                    R.id.channel_events_chip_banners -> EventsChannel.BANNERS
                    R.id.channel_events_chip_feature_flags -> EventsChannel.FEATURE_FLAGS
                    else -> null
                }
        }
    }

    private fun bindChannelSpinner(spinner: Spinner): Spinner {
        val labels = EventsChannel.entries.map { it.displayName }
        val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, labels)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter
        return spinner
    }

    private fun bindHttpResponseSpinner(spinner: Spinner) {
        val labels = ChannelSyncHttpResponse.entries.map { getString(it.label) }
        val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, labels)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter
        spinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    ChannelSyncHttpConnector.install(ChannelSyncHttpResponse.entries[position])
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
    }

    private fun selectedChannel(spinner: Spinner): EventsChannel = EventsChannel.entries[spinner.selectedItemPosition]

    private fun refresh(channel: EventsChannel) {
        val braze = Braze.getInstance(requireContext())
        when (channel) {
            EventsChannel.CONTENT_CARDS -> braze.requestContentCardsRefresh()
            EventsChannel.BANNERS -> braze.requestBannersRefresh(DroidboyApplication.BANNER_PLACEMENT_IDS)
            EventsChannel.FEATURE_FLAGS -> braze.refreshFeatureFlags()
        }
    }

    /** Restarts one channel's collection so its subscribe handshake is delivered again. */
    private fun resubscribe(channel: EventsChannel) {
        collectChannel(Braze.getInstance(requireContext()), channel)
    }

    private fun subscribeAll() {
        val braze = Braze.getInstance(requireContext())
        subscribedBraze = braze
        EventsChannel.entries.forEach { collectChannel(braze, it) }
        val previousUserChangeCollection = userChangeCollection
        userChangeCollection =
            viewLifecycleOwner.lifecycleScope.launch {
                previousUserChangeCollection?.cancelAndJoin()
                braze.changeUserEventsFlow().collect { activeUserId = it.currentUserId }
            }
    }

    /**
     * Replaces [channel]'s collection. The previous one is cancelled, which removes its subscription, before the new
     * one subscribes. Collections run in the view lifecycle scope, so they also stop in [onDestroyView].
     */
    private fun collectChannel(
        braze: Braze,
        channel: EventsChannel,
    ) {
        val previousCollection = channelCollections[channel]
        channelCollections[channel] =
            viewLifecycleOwner.lifecycleScope.launch {
                previousCollection?.cancelAndJoin()
                when (channel) {
                    EventsChannel.CONTENT_CARDS ->
                        braze.contentCardsEventsFlow().collect {
                            appendRow(ChannelEventRowMapper.from(it, System.currentTimeMillis(), activeUserId))
                        }
                    EventsChannel.BANNERS ->
                        braze.bannersEventsFlow().collect {
                            appendRow(ChannelEventRowMapper.from(it, System.currentTimeMillis(), activeUserId))
                        }
                    EventsChannel.FEATURE_FLAGS ->
                        braze.featureFlagsEventsFlow().collect {
                            appendRow(ChannelEventRowMapper.from(it, System.currentTimeMillis(), activeUserId))
                        }
                }
            }
    }

    private fun appendRow(row: ChannelEventRow) {
        val recycler = view?.findViewById<RecyclerView>(R.id.channel_events_recycler) ?: return
        recycler.post {
            if (!isAdded) return@post
            val layoutManager = recycler.layoutManager as? LinearLayoutManager
            val firstVisible = layoutManager?.findFirstVisibleItemPosition() ?: RecyclerView.NO_POSITION
            val wasFollowing = isNewestFirstLiveEdge(firstVisible)
            val newRowVisible = adapter.prepend(row)
            if (newRowVisible && wasFollowing) {
                recycler.scrollToPosition(0)
            }
        }
    }

    private fun showRowDetail(row: ChannelEventRow) {
        AlertDialog
            .Builder(requireContext())
            .setTitle("${row.channel.displayName} · ${row.eventType}")
            .setMessage(row.detailText)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
