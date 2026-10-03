package com.codepath.lab6

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the JSON parsing and the search rules on the JVM, no emulator needed.
 * Run with: ./gradlew test
 */
class NationalParkTest {

    private val acadiaJson = """
        {
          "parkCode": "acad",
          "fullName": "Acadia National Park",
          "designation": "National Park",
          "description": "Acadia protects rocky headlands.",
          "states": "ME",
          "url": "https://www.nps.gov/acad/index.htm",
          "latitude": "44.409286",
          "longitude": "-68.247501",
          "weatherInfo": "All four seasons.",
          "directionsInfo": "Take I-95 north.",
          "activities": [ { "name": "Hiking" }, { "name": "Biking" } ],
          "entranceFees": [
            { "title": "Timed Entry Reservation - Location", "cost": "6.00" },
            { "title": "Entrance - Private Vehicle", "cost": "35.00" }
          ],
          "images": [
            { "url": "https://example.com/1.jpg", "title": "Summit", "caption": "A hike",
              "altText": "Two hikers", "credit": "NPS" }
          ]
        }
    """.trimIndent()

    private val sparseJson = """
        {
          "parkCode": "afam",
          "fullName": "African American Civil War Memorial",
          "designation": "",
          "description": "A memorial.",
          "states": "DC",
          "latitude": "",
          "longitude": null,
          "activities": [],
          "entranceFees": [],
          "images": []
        }
    """.trimIndent()

    private val acadia = NationalPark.fromJson(JSONObject(acadiaJson))
    private val sparse = NationalPark.fromJson(JSONObject(sparseJson))

    @Test
    fun parsesRequiredFields() {
        assertEquals("Acadia National Park", acadia.fullName)
        assertEquals("Acadia protects rocky headlands.", acadia.description)
        assertEquals("ME", acadia.states)
        assertEquals("https://example.com/1.jpg", acadia.imageUrl)
    }

    @Test
    fun parsesStretchFields() {
        assertEquals("National Park", acadia.designation)
        assertEquals(listOf("Hiking", "Biking"), acadia.activities)
        assertEquals(44.409286, acadia.latitude!!, 0.000001)
        assertTrue(acadia.hasLocation)
        assertEquals("Two hikers", acadia.images.first().altText)
    }

    @Test
    fun prefersPrivateVehicleFeeOverFirstFee() {
        assertEquals("$35.00", acadia.entranceFee?.formattedCost)
        assertEquals("Private Vehicle", acadia.entranceFee?.shortTitle)
    }

    @Test
    fun handlesMissingAndNullValues() {
        assertNull(sparse.imageUrl)
        assertNull(sparse.entranceFee)
        assertNull(sparse.longitude)
        assertFalse(sparse.hasLocation)
        assertEquals("", sparse.url) // missing key, not the text "null"
    }

    @Test
    fun parsesArrayAndFormatsStates() {
        val parks = NationalPark.fromJsonArray(JSONArray("[$acadiaJson, $sparseJson]"))
        assertEquals(2, parks.size)

        val trail = acadia.copy(states = "KY,TN")
        assertEquals("KY, TN", trail.formattedStateCodes)
        assertEquals("Kentucky, Tennessee", trail.formattedStateNames)
    }

    @Test
    fun searchMatchesNameDesignationAndState() {
        val gettysburg = acadia.copy(
            parkCode = "gett", fullName = "Gettysburg", designation = "National Military Park", states = "PA"
        )
        val parks = listOf(acadia, sparse, gettysburg)

        fun codes(query: String) = ParkSearch.filter(parks, query).map { it.parkCode }

        assertEquals(listOf("acad"), codes("acadia"))            // name
        assertEquals(listOf("gett"), codes("military"))          // designation
        assertEquals(listOf("acad"), codes("maine"))             // full state name
        assertEquals(listOf("afam"), codes("dc"))                // state code
        assertEquals(listOf("afam"), codes("columbia"))          // any word of the state name
        assertEquals(listOf("gett"), codes("penn"))
        assertEquals(parks, ParkSearch.filter(parks, "  "))      // blank search shows everything
    }

    @Test
    fun shortStateQueryDoesNotMatchMiddleOfStateName() {
        // "va" should find Virginia (by code), not PennsylVAnia
        val parks = listOf(
            acadia.copy(parkCode = "shen", fullName = "Shenandoah", states = "VA"),
            acadia.copy(parkCode = "gett", fullName = "Gettysburg", states = "PA")
        )
        assertEquals(listOf("shen"), ParkSearch.filter(parks, "va").map { it.parkCode })
    }
}
