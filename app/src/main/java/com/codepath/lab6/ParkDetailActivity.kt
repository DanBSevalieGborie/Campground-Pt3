package com.codepath.lab6

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import com.codepath.lab6.databinding.ActivityParkDetailBinding
import com.google.android.material.chip.Chip

/** Show this many activity chips, then a "+N more" chip. */
private const val MAX_ACTIVITY_CHIPS = 12

/**
 * The park detail screen, opened by tapping a park card.
 *
 * Shows a swipeable photo gallery, the full description, entrance fee,
 * things to do, weather and directions, plus buttons for driving directions
 * in Google Maps and the park's official NPS page.
 */
class ParkDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParkDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParkDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        val park = IntentCompat.getParcelableExtra(intent, EXTRA_PARK, NationalPark::class.java)
        if (park == null) {
            finish()
            return
        }

        bindHeader(park)
        binding.gallery.bind(park.images)
        bindActions(park)
        bindFee(park)
        bindActivities(park)
        bindTextSection(binding.descriptionSection, binding.parkDescription, park.description)
        bindTextSection(binding.weatherSection, binding.parkWeather, park.weatherInfo)
        bindTextSection(binding.gettingThereSection, binding.parkGettingThere, park.directionsInfo)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun bindHeader(park: NationalPark) {
        binding.parkDesignation.text = park.designation
        binding.parkDesignation.isVisible = park.designation.isNotEmpty()

        binding.parkName.text = park.fullName

        binding.parkStates.text = park.formattedStateNames
        binding.parkStates.isVisible = park.states.isNotEmpty()

        // Once the big park name scrolls off screen, show it in the toolbar instead
        binding.scrollView.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
                val nameScrolledAway = scrollY > binding.detailContent.top + binding.parkName.bottom
                supportActionBar?.title = if (nameScrolledAway) park.fullName else ""
            }
        )
    }

    /** Open the park in Google Maps, or its official NPS page. */
    private fun bindActions(park: NationalPark) {
        binding.directionsButton.isVisible = park.hasLocation
        binding.directionsButton.setOnClickListener {
            if (park.latitude != null && park.longitude != null) {
                openUrl(directionsUrl(park.latitude, park.longitude))
            }
        }

        binding.websiteButton.isVisible = park.url.isNotEmpty()
        binding.websiteButton.setOnClickListener { openUrl(park.url) }

        binding.actionButtons.isVisible =
            binding.directionsButton.isVisible || binding.websiteButton.isVisible
    }

    private fun bindFee(park: NationalPark) {
        val fee = park.entranceFee
        binding.feeValue.text = fee?.formattedCost ?: getString(R.string.fee_none)

        binding.feeDetail.text = fee?.shortTitle.orEmpty()
        binding.feeDetail.isVisible = binding.feeDetail.text.isNotEmpty()
    }

    private fun bindActivities(park: NationalPark) {
        if (park.activities.isEmpty()) {
            binding.activitiesSection.isVisible = false
            return
        }

        park.activities.take(MAX_ACTIVITY_CHIPS).forEach {
            binding.activitiesChips.addView(makeInfoChip(this, it))
        }

        val remaining = park.activities.size - MAX_ACTIVITY_CHIPS
        if (remaining > 0) {
            binding.activitiesChips.addView(makeInfoChip(this, getString(R.string.more_activities, remaining)))
        }
    }

    companion object {
        private const val EXTRA_PARK = "extra_park"

        fun newIntent(context: Context, park: NationalPark): Intent {
            return Intent(context, ParkDetailActivity::class.java).putExtra(EXTRA_PARK, park)
        }
    }
}

/** A read-only chip for the "Things to do" and "Amenities" sections. */
fun makeInfoChip(context: Context, label: String): Chip {
    return Chip(context).apply {
        text = label
        isClickable = false
        isCheckable = false
    }
}

/** Fills a titled text section, or hides it when the API left the field empty. */
fun bindTextSection(section: View, textView: TextView, value: String) {
    textView.text = value
    section.isVisible = value.isNotEmpty()
}
