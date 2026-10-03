package com.codepath.lab6

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the campground JSON parsing, fee choice, amenities and search rules
 * on the JVM, no emulator needed. Run with: ./gradlew test
 */
class CampgroundTest {

    // Trimmed from a real /campgrounds response (Padre Island National Seashore)
    private val birdIslandJson = """
        {
          "id": "C4CB1781",
          "url": "https://www.nps.gov/pais/planyourvisit/bird-island-basin-campground.htm",
          "name": "Bird Island Basin Campground",
          "parkCode": "pais",
          "description": "Bird Island Basin lies along Laguna Madre with easy access for fishing and kayaking.",
          "latitude": "27.466794",
          "longitude": "-97.313693",
          "latLong": "{lat:27.466794, lng:-97.313693}",
          "reservationInfo": "Reservations are not accepted. All sites are first-come, first-served.",
          "reservationUrl": "",
          "amenities": {
            "trashRecyclingCollection": "Yes - year round",
            "toilets": ["Vault Toilets - year round"],
            "internetConnectivity": "No",
            "showers": ["None"],
            "cellPhoneReception": "No",
            "laundry": "No",
            "amphitheater": "No",
            "dumpStation": "No",
            "campStore": "No",
            "staffOrVolunteerHostOnsite": "Yes - seasonal",
            "potableWater": ["No water"],
            "iceAvailableForSale": "No",
            "firewoodForSale": "No",
            "foodStorageLockers": "No"
          },
          "fees": [
            { "cost": "4.00", "description": "", "title": "Senior/Access Pass Campsite" },
            { "cost": "8.00", "description": "Nightly fee.", "title": "Standard Tent or RV Site" }
          ],
          "directionsOverview": "From the park entrance station, travel south on Park Road 22.",
          "operatingHours": [ { "description": "Open year-round, first-come, first-served." } ],
          "addresses": [
            { "city": "Corpus Christi", "stateCode": "TX", "type": "Mailing" },
            { "city": "Padre Island", "stateCode": "TX", "type": "Physical" }
          ],
          "images": [
            { "credit": "NPS photo", "title": "Buses at BIB", "altText": "Three school buses",
              "caption": "Converted school buses", "url": "https://example.com/bib.jpg" }
          ],
          "weatherOverview": "Windy year-round.",
          "numberOfSitesReservable": "0",
          "numberOfSitesFirstComeFirstServe": "34",
          "campsites": { "totalSites": "34", "electricalHookups": "0" },
          "accessibility": {
            "rvAllowed": "1",
            "rvMaxLength": "45",
            "classifications": ["Primitive Camping Areas"]
          }
        }
    """.trimIndent()

    // A campground with almost nothing filled in, plus a link missing "https://"
    private val sparseJson = """
        {
          "id": "X1",
          "name": "Backcountry Site",
          "description": "Hike in.",
          "latitude": "",
          "longitude": null,
          "url": "www.nps.gov/cany/camp.htm",
          "fees": [],
          "addresses": [],
          "images": [],
          "campsites": {},
          "accessibility": {}
        }
    """.trimIndent()

    private val birdIsland = Campground.fromJson(JSONObject(birdIslandJson))
    private val sparse = Campground.fromJson(JSONObject(sparseJson))

    @Test
    fun parsesMainFields() {
        assertEquals("Bird Island Basin Campground", birdIsland.name)
        assertEquals("pais", birdIsland.parkCode)
        assertEquals("Primitive Camping Areas", birdIsland.classification)
        assertEquals("https://example.com/bib.jpg", birdIsland.imageUrl)
        assertEquals("Three school buses", birdIsland.images.first().altText)
        assertEquals(27.466794, birdIsland.latitude!!, 0.000001)
        assertTrue(birdIsland.hasLocation)
        assertEquals("Open year-round, first-come, first-served.", birdIsland.hours)
    }

    @Test
    fun usesThePhysicalAddressForLocation() {
        assertEquals("Padre Island, TX", birdIsland.shortLocation)
        assertEquals("Padre Island, Texas", birdIsland.longLocation)
    }

    @Test
    fun parsesSiteCounts() {
        assertEquals(34, birdIsland.totalSites)
        assertEquals(0, birdIsland.reservableSites)
        assertEquals(34, birdIsland.firstComeSites)
        assertEquals(45, birdIsland.rvMaxLength)
    }

    @Test
    fun skipsSeniorRateWhenPickingTheFee() {
        assertEquals("Standard Tent or RV Site", birdIsland.fee?.title)
        assertEquals("$8.00", birdIsland.fee?.formattedCost)
        assertEquals("$8", birdIsland.fee?.compactCost)
    }

    @Test
    fun formatsCompactCosts() {
        assertEquals("$17.50", CampgroundFee("Site", "17.50").compactCost)
        assertEquals("Free", CampgroundFee("Site", "0.00").compactCost)
        assertTrue(CampgroundFee("Site", "0.00").isFree)
    }

    @Test
    fun readsAmenities() {
        assertEquals(
            listOf(Amenity.VAULT_TOILETS, Amenity.RVS_ALLOWED, Amenity.TRASH, Amenity.CAMP_HOST),
            birdIsland.amenities
        )
        assertFalse(Amenity.DRINKING_WATER in birdIsland.amenities) // "No water"
        assertFalse(Amenity.SHOWERS in birdIsland.amenities)        // "None"
    }

    @Test
    fun handlesMissingAndNullValues() {
        assertNull(sparse.imageUrl)
        assertNull(sparse.fee)
        assertNull(sparse.longitude)
        assertFalse(sparse.hasLocation)
        assertEquals("", sparse.classification)
        assertEquals("", sparse.shortLocation)
        assertEquals(0, sparse.totalSites)
        assertEquals("", sparse.reservationUrl) // missing key, not the text "null"
        assertEquals(emptyList<Amenity>(), sparse.amenities)
    }

    @Test
    fun addsMissingSchemeToLinks() {
        assertEquals("https://www.nps.gov/cany/camp.htm", sparse.url)
    }

    @Test
    fun parsesArray() {
        val list = Campground.fromJsonArray(JSONArray("[$birdIslandJson, $sparseJson]"))
        assertEquals(listOf("Bird Island Basin Campground", "Backcountry Site"), list.map { it.name })
    }

    // ----- Search (the Campgrounds Lab 5 rules, plus town and state) -----

    private fun campground(name: String, description: String, city: String = "", state: String = "") =
        sparse.copy(id = name, name = name, description = description, city = city, stateCode = state)

    private val mather = campground("Mather Campground", "Located in Grand Canyon Village on the South Rim.", "Grand Canyon", "AZ")
    private val desertView = campground("Desert View Campground", "Quiet sites near the east entrance.", "Grand Canyon", "AZ")
    private val bigMeadows = campground("Big Meadows", "Near Skyline Drive.", "Luray", "VA")
    private val fortWashington = campground("Fort Washington Group Camp", "A walk-in site by the river.", "", "PA")
    private val all = listOf(mather, desertView, bigMeadows, fortWashington)

    private fun names(query: String) = CampgroundSearch.filter(all, query).map { it.name }

    @Test
    fun blankQueryReturnsEverything() {
        assertEquals(all, CampgroundSearch.filter(all, ""))
        assertEquals(all, CampgroundSearch.filter(all, "   "))
    }

    @Test
    fun matchesNameAndDescriptionIgnoringCase() {
        assertEquals(listOf("Desert View Campground"), names("DESERT"))
        assertEquals(listOf("Mather Campground"), names("south rim"))
    }

    @Test
    fun everyWordMustMatchSomewhere() {
        assertEquals(listOf("Mather Campground"), names("mather village"))
        assertEquals(emptyList<String>(), names("mather river"))
    }

    @Test
    fun matchesTownAndState() {
        assertEquals(listOf("Big Meadows"), names("luray"))
        assertEquals(listOf("Big Meadows"), names("virginia"))
        assertEquals(listOf("Mather Campground", "Desert View Campground"), names("arizona"))
        assertEquals(listOf("Fort Washington Group Camp"), names("penn"))
    }

    @Test
    fun shortStateQueryDoesNotMatchMiddleOfStateName() {
        // "va" finds the Virginia campground, not the one in PennsylVAnia
        assertEquals(listOf("Big Meadows"), names("va"))
    }
}
