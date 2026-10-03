package com.codepath.lab6

/**
 * The Parks tab's search rules.
 *
 * A park matches when the query appears in:
 *  - its name            ("canyon"  -> Grand Canyon, Bryce Canyon...)
 *  - its designation     ("monument" -> every National Monument)
 *  - one of its states, by exact code ("VA") or by the start of a word in the
 *    full name ("carolina" or "north" -> North Carolina)
 *
 * State names use word prefixes rather than "contains" so that typing "va"
 * finds Virginia parks without also pulling in PennsylVAnia.
 */
object ParkSearch {

    fun filter(parks: List<NationalPark>, query: String): List<NationalPark> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return parks
        return parks.filter { matches(it, trimmed) }
    }

    fun matches(park: NationalPark, query: String): Boolean {
        if (park.fullName.contains(query, ignoreCase = true)) return true
        if (park.designation.contains(query, ignoreCase = true)) return true
        return park.stateCodes.any { StateNames.matches(it, query) }
    }
}

/**
 * The Campgrounds tab's search rules (from Campgrounds Lab 5, plus location).
 *
 * Every word in the query must match somewhere, so "grand rim" finds a
 * campground that mentions both words. A word matches when it appears in the
 * name, description, town or campground type, or when it is the campground's
 * state ("TX", "texas", "carolina").
 */
object CampgroundSearch {

    fun filter(campgrounds: List<Campground>, query: String): List<Campground> {
        val words = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return campgrounds
        return campgrounds.filter { campground -> words.all { matches(campground, it) } }
    }

    private fun matches(campground: Campground, word: String): Boolean {
        val text = listOf(
            campground.name,
            campground.description,
            campground.city,
            campground.classification
        )
        if (text.any { it.contains(word, ignoreCase = true) }) return true
        return campground.stateCode.isNotEmpty() && StateNames.matches(campground.stateCode, word)
    }
}
