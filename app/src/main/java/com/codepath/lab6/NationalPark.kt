package com.codepath.lab6

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject

/**
 * The model representing a single park, parsed from
 * https://developer.nps.gov/api/v1/parks
 *
 * Beyond name / description / state / image, it keeps the extra details the
 * detail screen shows (designation, fees, activities, weather, directions,
 * coordinates, website, and every photo).
 *
 * @Parcelize lets a whole park be handed to [ParkDetailActivity] through an Intent.
 */
@Parcelize
data class NationalPark(
    val parkCode: String,
    val fullName: String,
    val designation: String,
    val description: String,
    val states: String,
    val url: String,
    val latitude: Double?,
    val longitude: Double?,
    val weatherInfo: String,
    val directionsInfo: String,
    val activities: List<String>,
    val entranceFee: EntranceFee?,
    val images: List<NpsImage>
) : Parcelable {

    /** The first photo, shown on the list card. */
    val imageUrl: String?
        get() = images.firstOrNull()?.url

    /** "KY,TN" -> ["KY", "TN"] */
    val stateCodes: List<String>
        get() = states.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    /** "KY,TN" -> "KY, TN" for the list card. */
    val formattedStateCodes: String
        get() = stateCodes.joinToString(", ")

    /** "KY,TN" -> "Kentucky, Tennessee" for the detail screen. */
    val formattedStateNames: String
        get() = stateCodes.joinToString(", ") { StateNames.nameFor(it) ?: it }

    val hasLocation: Boolean
        get() = latitude != null && longitude != null

    companion object {

        /** Turns the "data" array from the API response into a list of parks. */
        fun fromJsonArray(parkJsonArray: JSONArray): List<NationalPark> {
            return (0 until parkJsonArray.length())
                .mapNotNull { parkJsonArray.optJSONObject(it) }
                .map { fromJson(it) }
        }

        fun fromJson(parkJson: JSONObject): NationalPark {
            return NationalPark(
                parkCode = parkJson.cleanString("parkCode"),
                fullName = parkJson.cleanString("fullName"),
                designation = parkJson.cleanString("designation"),
                description = parkJson.cleanString("description"),
                states = parkJson.cleanString("states"),
                url = parkJson.cleanString("url"),
                latitude = parkJson.cleanString("latitude").toDoubleOrNull(),
                longitude = parkJson.cleanString("longitude").toDoubleOrNull(),
                weatherInfo = parkJson.cleanString("weatherInfo"),
                directionsInfo = parkJson.cleanString("directionsInfo"),
                activities = parseActivities(parkJson.optJSONArray("activities")),
                entranceFee = EntranceFee.pickFrom(parkJson.optJSONArray("entranceFees")),
                images = NpsImage.listFrom(parkJson.optJSONArray("images"))
            )
        }

        private fun parseActivities(array: JSONArray?): List<String> {
            if (array == null) return emptyList()
            return (0 until array.length())
                .mapNotNull { array.optJSONObject(it)?.cleanString("name") }
                .filter { it.isNotEmpty() }
        }
    }
}

/**
 * The entrance fee shown on the park detail screen.
 *
 * Parks list several fees (vehicle, motorcycle, per person, commercial...).
 * The first one isn't always the useful one — Acadia's is a $6 timed-entry
 * reservation — so [pickFrom] prefers the standard private-vehicle fee.
 */
@Parcelize
data class EntranceFee(
    val title: String,
    val cost: String
) : Parcelable {

    /** "35.00" -> "$35.00", "0.00" -> "Free" */
    val formattedCost: String
        get() = formatCost(cost)

    /** "Entrance - Private Vehicle" -> "Private Vehicle" */
    val shortTitle: String
        get() = title.substringAfter(" - ", title).trim()

    companion object {
        fun pickFrom(fees: JSONArray?): EntranceFee? {
            if (fees == null || fees.length() == 0) return null

            val all = (0 until fees.length())
                .mapNotNull { fees.optJSONObject(it) }
                .map { EntranceFee(it.cleanString("title"), it.cleanString("cost")) }

            return all.firstOrNull { it.title.contains("Private Vehicle", ignoreCase = true) }
                ?: all.firstOrNull { it.title.contains("Per Person", ignoreCase = true) }
                ?: all.firstOrNull()
        }
    }
}

/** "35.00" -> "$35.00", "0.00" -> "Free". Anything unparseable is shown as-is. */
internal fun formatCost(cost: String): String {
    val amount = cost.toDoubleOrNull() ?: return cost
    return if (amount == 0.0) "Free" else "$" + String.format(java.util.Locale.US, "%.2f", amount)
}
