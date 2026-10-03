package com.codepath.lab6

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import com.codepath.lab6.databinding.ActivityCampgroundDetailBinding

/**
 * The campground detail screen, opened by tapping a campground card.
 *
 * Built like the park detail screen: a swipeable photo gallery, the campground
 * type, name and town, Directions / Reserve buttons, cards for the nightly fee
 * and number of sites, then About, Amenities, Reservations, Hours, Weather and
 * Getting there.
 */
class CampgroundDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCampgroundDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCampgroundDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        val campground = IntentCompat.getParcelableExtra(intent, EXTRA_CAMPGROUND, Campground::class.java)
        if (campground == null) {
            finish()
            return
        }

        bindHeader(campground)
        binding.gallery.bind(campground.images)
        bindActions(campground)
        bindFee(campground)
        bindSites(campground)
        bindAmenities(campground)
        bindTextSection(binding.descriptionSection, binding.campgroundDescription, campground.description)
        bindTextSection(binding.reservationsSection, binding.campgroundReservations, campground.reservationInfo)
        bindTextSection(binding.hoursSection, binding.campgroundHours, campground.hours)
        bindTextSection(binding.weatherSection, binding.campgroundWeather, campground.weather)
        bindTextSection(binding.gettingThereSection, binding.campgroundGettingThere, campground.directions)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun bindHeader(campground: Campground) {
        binding.campgroundOverline.text = campground.classification.ifEmpty { getString(R.string.campground) }
        binding.campgroundName.text = campground.name

        binding.campgroundLocation.text = campground.longLocation
        binding.campgroundLocation.isVisible = campground.longLocation.isNotEmpty()

        // Once the big name scrolls off screen, show it in the toolbar instead
        binding.scrollView.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
                val nameScrolledAway =
                    scrollY > binding.detailContent.top + binding.campgroundName.bottom
                supportActionBar?.title = if (nameScrolledAway) campground.name else ""
            }
        )
    }

    /** Directions in Google Maps, plus Reserve (recreation.gov) or the NPS page when there's no booking link. */
    private fun bindActions(campground: Campground) {
        binding.directionsButton.isVisible = campground.hasLocation
        binding.directionsButton.setOnClickListener {
            if (campground.latitude != null && campground.longitude != null) {
                openUrl(directionsUrl(campground.latitude, campground.longitude))
            }
        }

        val canReserve = campground.reservationUrl.isNotEmpty()
        binding.reserveButton.isVisible = canReserve
        binding.reserveButton.setOnClickListener { openUrl(campground.reservationUrl) }

        binding.websiteButton.isVisible = !canReserve && campground.url.isNotEmpty()
        binding.websiteButton.setOnClickListener { openUrl(campground.url) }

        binding.actionButtons.isVisible = binding.directionsButton.isVisible ||
            binding.reserveButton.isVisible || binding.websiteButton.isVisible
    }

    private fun bindFee(campground: Campground) {
        val fee = campground.fee
        binding.feeValue.text = fee?.formattedCost ?: getString(R.string.fee_none)

        binding.feeDetail.text = fee?.title.orEmpty()
        binding.feeDetail.isVisible = binding.feeDetail.text.isNotEmpty()
    }

    private fun bindSites(campground: Campground) {
        // Some backcountry areas list 0 sites; the fee card then takes the full width
        binding.sitesCard.isVisible = campground.totalSites > 0
        binding.sitesValue.text = campground.totalSites.toString()

        val reservable = campground.reservableSites
        val firstCome = campground.firstComeSites
        binding.sitesDetail.text = when {
            reservable > 0 && firstCome > 0 -> getString(R.string.sites_mixed, reservable, firstCome)
            reservable > 0 -> getString(R.string.sites_reservable)
            firstCome > 0 -> getString(R.string.sites_first_come)
            else -> ""
        }
        binding.sitesDetail.isVisible = binding.sitesDetail.text.isNotEmpty()
    }

    private fun bindAmenities(campground: Campground) {
        if (campground.amenities.isEmpty()) {
            binding.amenitiesSection.isVisible = false
            return
        }

        campground.amenities.forEach { amenity ->
            val label = if (amenity == Amenity.RVS_ALLOWED && campground.rvMaxLength > 0) {
                getString(R.string.amenity_rvs_length, campground.rvMaxLength)
            } else {
                getString(amenity.label)
            }
            binding.amenitiesChips.addView(makeInfoChip(this, label))
        }
    }

    companion object {
        private const val EXTRA_CAMPGROUND = "extra_campground"

        fun newIntent(context: Context, campground: Campground): Intent {
            return Intent(context, CampgroundDetailActivity::class.java)
                .putExtra(EXTRA_CAMPGROUND, campground)
        }
    }
}
