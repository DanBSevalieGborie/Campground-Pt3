package com.codepath.lab6

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject

/**
 * The model representing a single campground, parsed from
 * https://developer.nps.gov/api/v1/campgrounds
 *
 * The old version only kept name / description / latLong / one image. This one
 * keeps everything the redesigned card and detail screen show, the same way
 * [NationalPark] does for parks: every photo, the nightly fee, how many sites
 * there are and whether they can be reserved, amenities, hours, weather,
 * directions, the town and state, and the reservation / website links.
 */
@Parcelize
data class Campground(
    val id: String,
    val name: String,
    val parkCode: String,
    val description: String,
    /** "Developed Campground", "Primitive Camping Areas"... (can be empty) */
    val classification: String,
    val city: String,
    val stateCode: String,
    val latitude: Double?,
    val longitude: Double?,
    val url: String,
    val reservationUrl: String,
    val reservationInfo: String,
    val hours: String,
    val weather: String,
    val directions: String,
    val totalSites: Int,
    val reservableSites: Int,
    val firstComeSites: Int,
    val fee: CampgroundFee?,
    val amenities: List<Amenity>,
    val rvMaxLength: Int,
    val images: List<NpsImage>
) : Parcelable {

    /** The first photo, shown on the list card. */
    val imageUrl: String?
        get() = images.firstOrNull()?.url

    val hasLocation: Boolean
        get() = latitude != null && longitude != null

    /** "Corpus Christi, TX" for the list card ("TX" or "" when parts are missing). */
    val shortLocation: String
        get() = listOf(city, stateCode).filter { it.isNotEmpty() }.joinToString(", ")

    /** "Corpus Christi, Texas" for the detail screen. */
    val longLocation: String
        get() = listOf(city, StateNames.nameFor(stateCode) ?: stateCode)
            .filter { it.isNotEmpty() }
            .joinToString(", ")

    companion object {

        /** Turns the "data" array from the API response into a list of campgrounds. */
        fun fromJsonArray(array: JSONArray): List<Campground> {
            return (0 until array.length())
                .mapNotNull { array.optJSONObject(it) }
                .map { fromJson(it) }
        }

        fun fromJson(json: JSONObject): Campground {
            val campsites = json.optJSONObject("campsites") ?: JSONObject()
            val accessibility = json.optJSONObject("accessibility") ?: JSONObject()
            val address = pickAddress(json.optJSONArray("addresses"))

            return Campground(
                id = json.cleanString("id"),
                name = json.cleanString("name"),
                parkCode = json.cleanString("parkCode"),
                description = json.cleanString("description"),
                classification = accessibility.cleanStringList("classifications").firstOrNull().orEmpty(),
                city = address?.cleanString("city").orEmpty(),
                stateCode = address?.cleanString("stateCode").orEmpty(),
                latitude = json.cleanString("latitude").toDoubleOrNull(),
                longitude = json.cleanString("longitude").toDoubleOrNull(),
                url = withScheme(json.cleanString("url")),
                reservationUrl = withScheme(json.cleanString("reservationUrl")),
                reservationInfo = json.cleanString("reservationInfo"),
                hours = firstDescription(json.optJSONArray("operatingHours")),
                weather = json.cleanString("weatherOverview"),
                directions = json.cleanString("directionsOverview"),
                totalSites = campsites.cleanInt("totalSites"),
                reservableSites = json.cleanInt("numberOfSitesReservable"),
                firstComeSites = json.cleanInt("numberOfSitesFirstComeFirstServe"),
                fee = CampgroundFee.pickFrom(json.optJSONArray("fees")),
                amenities = Amenity.parse(
                    json.optJSONObject("amenities") ?: JSONObject(),
                    rvAllowed = accessibility.cleanString("rvAllowed") == "1",
                    electricSites = campsites.cleanInt("electricalHookups")
                ),
                rvMaxLength = accessibility.cleanInt("rvMaxLength"),
                images = NpsImage.listFrom(json.optJSONArray("images"))
            )
        }

        /** The physical address has the campground's own town; the mailing one is often the park HQ. */
        private fun pickAddress(addresses: JSONArray?): JSONObject? {
            if (addresses == null) return null
            val all = (0 until addresses.length()).mapNotNull { addresses.optJSONObject(it) }
            return all.firstOrNull {
                it.cleanString("type").equals("Physical", ignoreCase = true) &&
                    it.cleanString("stateCode").isNotEmpty()
            } ?: all.firstOrNull { it.cleanString("stateCode").isNotEmpty() }
        }

        private fun firstDescription(array: JSONArray?): String {
            if (array == null) return ""
            return (0 until array.length())
                .mapNotNull { array.optJSONObject(it)?.cleanString("description") }
                .firstOrNull { it.isNotEmpty() }
                .orEmpty()
        }

        /** A few NPS links come without "https://", which Android can't open. */
        private fun withScheme(url: String): String = when {
            url.isEmpty() -> ""
            url.contains("://") -> url
            else -> "https://$url"
        }
    }
}

/**
 * The fee shown on the campground card and detail screen.
 *
 * Campgrounds often list a discounted Senior/Access Pass rate or a group site
 * before (or after) the standard site, so [pickFrom] skips those first.
 */
@Parcelize
data class CampgroundFee(
    val title: String,
    val cost: String
) : Parcelable {

    /** "8.00" -> "$8.00", "0.00" -> "Free" */
    val formattedCost: String
        get() = formatCost(cost)

    /** "8.00" -> "$8", "17.50" -> "$17.50", "0.00" -> "Free" — for the small price tag on the card. */
    val compactCost: String
        get() {
            val amount = cost.toDoubleOrNull() ?: return cost
            return when {
                amount == 0.0 -> "Free"
                amount % 1.0 == 0.0 -> "$" + amount.toLong()
                else -> formatCost(cost)
            }
        }

    val isFree: Boolean
        get() = cost.toDoubleOrNull() == 0.0

    companion object {
        private val SPECIAL_RATES = listOf("senior", "access pass", "group", "horse", "equestrian")

        fun pickFrom(fees: JSONArray?): CampgroundFee? {
            if (fees == null || fees.length() == 0) return null

            val all = (0 until fees.length())
                .mapNotNull { fees.optJSONObject(it) }
                .map { CampgroundFee(it.cleanString("title"), it.cleanString("cost")) }
                .filter { it.cost.isNotEmpty() }

            return all.firstOrNull { fee -> SPECIAL_RATES.none { fee.title.contains(it, ignoreCase = true) } }
                ?: all.firstOrNull()
        }
    }
}

/**
 * The amenities shown as chips on the detail screen. The API answers most of
 * these with text like "Yes - seasonal" or "No", or lists like ["Flush Toilets - year round"].
 */
enum class Amenity(@StringRes val label: Int) {
    DRINKING_WATER(R.string.amenity_water),
    FLUSH_TOILETS(R.string.amenity_flush_toilets),
    VAULT_TOILETS(R.string.amenity_vault_toilets),
    OTHER_TOILETS(R.string.amenity_toilets),
    SHOWERS(R.string.amenity_showers),
    ELECTRIC_HOOKUPS(R.string.amenity_electric),
    RVS_ALLOWED(R.string.amenity_rvs),
    DUMP_STATION(R.string.amenity_dump_station),
    CAMP_STORE(R.string.amenity_camp_store),
    FIREWOOD(R.string.amenity_firewood),
    ICE(R.string.amenity_ice),
    LAUNDRY(R.string.amenity_laundry),
    FOOD_LOCKERS(R.string.amenity_food_lockers),
    TRASH(R.string.amenity_trash),
    CELL_SERVICE(R.string.amenity_cell),
    INTERNET(R.string.amenity_internet),
    CAMP_HOST(R.string.amenity_camp_host),
    AMPHITHEATER(R.string.amenity_amphitheater);

    companion object {

        fun parse(amenities: JSONObject, rvAllowed: Boolean, electricSites: Int): List<Amenity> {
            val found = mutableListOf<Amenity>()

            if (amenities.cleanStringList("potableWater").any { it.isYes() }) found += DRINKING_WATER

            val toilets = amenities.cleanStringList("toilets").map { it.lowercase() }
            when {
                toilets.any { "flush" in it } -> found += FLUSH_TOILETS
                toilets.any { "vault" in it } -> found += VAULT_TOILETS
                toilets.any { "toilet" in it && !it.startsWith("no ") } -> found += OTHER_TOILETS
            }

            if (amenities.cleanStringList("showers").any { !it.equals("None", ignoreCase = true) }) {
                found += SHOWERS
            }
            if (electricSites > 0) found += ELECTRIC_HOOKUPS
            if (rvAllowed) found += RVS_ALLOWED

            val yesNoFields = listOf(
                "dumpStation" to DUMP_STATION,
                "campStore" to CAMP_STORE,
                "firewoodForSale" to FIREWOOD,
                "iceAvailableForSale" to ICE,
                "laundry" to LAUNDRY,
                "foodStorageLockers" to FOOD_LOCKERS,
                "trashRecyclingCollection" to TRASH,
                "cellPhoneReception" to CELL_SERVICE,
                "internetConnectivity" to INTERNET,
                "staffOrVolunteerHostOnsite" to CAMP_HOST,
                "amphitheater" to AMPHITHEATER
            )
            yesNoFields.forEach { (key, amenity) ->
                if (amenities.cleanString(key).isYes()) found += amenity
            }
            return found
        }

        private fun String.isYes() = trim().startsWith("yes", ignoreCase = true)
    }
}
