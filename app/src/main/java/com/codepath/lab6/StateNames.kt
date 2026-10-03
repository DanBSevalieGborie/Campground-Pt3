package com.codepath.lab6

/**
 * The NPS API only returns two-letter state codes ("UT", "KY,TN").
 * This lookup turns them into full names for the detail screens, and lets
 * search match "Utah" as well as "UT".
 */
object StateNames {

    private val names = mapOf(
        "AL" to "Alabama", "AK" to "Alaska", "AZ" to "Arizona", "AR" to "Arkansas",
        "CA" to "California", "CO" to "Colorado", "CT" to "Connecticut", "DE" to "Delaware",
        "DC" to "District of Columbia", "FL" to "Florida", "GA" to "Georgia", "HI" to "Hawaii",
        "ID" to "Idaho", "IL" to "Illinois", "IN" to "Indiana", "IA" to "Iowa",
        "KS" to "Kansas", "KY" to "Kentucky", "LA" to "Louisiana", "ME" to "Maine",
        "MD" to "Maryland", "MA" to "Massachusetts", "MI" to "Michigan", "MN" to "Minnesota",
        "MS" to "Mississippi", "MO" to "Missouri", "MT" to "Montana", "NE" to "Nebraska",
        "NV" to "Nevada", "NH" to "New Hampshire", "NJ" to "New Jersey", "NM" to "New Mexico",
        "NY" to "New York", "NC" to "North Carolina", "ND" to "North Dakota", "OH" to "Ohio",
        "OK" to "Oklahoma", "OR" to "Oregon", "PA" to "Pennsylvania", "RI" to "Rhode Island",
        "SC" to "South Carolina", "SD" to "South Dakota", "TN" to "Tennessee", "TX" to "Texas",
        "UT" to "Utah", "VT" to "Vermont", "VA" to "Virginia", "WA" to "Washington",
        "WV" to "West Virginia", "WI" to "Wisconsin", "WY" to "Wyoming",
        // Territories that have NPS sites
        "AS" to "American Samoa", "GU" to "Guam", "MP" to "Northern Mariana Islands",
        "PR" to "Puerto Rico", "VI" to "U.S. Virgin Islands"
    )

    fun nameFor(code: String): String? = names[code.trim().uppercase()]

    /**
     * True when [query] is the state's code ("va") or the start of its name or of
     * any word in it ("vir", "carolina") — but not the middle of a word, so "va"
     * doesn't match PennsylVAnia.
     */
    fun matches(code: String, query: String): Boolean {
        if (code.equals(query, ignoreCase = true)) return true
        val stateName = nameFor(code) ?: return false
        return stateName.startsWith(query, ignoreCase = true) ||
            stateName.split(" ").any { word -> word.startsWith(query, ignoreCase = true) }
    }
}
