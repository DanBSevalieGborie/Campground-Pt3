package com.codepath.lab6

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject

/**
 * One photo from an "images" array. Parks and campgrounds use the same shape,
 * so both detail screens share this model and the photo gallery.
 */
@Parcelize
data class NpsImage(
    val url: String,
    val title: String,
    val caption: String,
    val altText: String,
    val credit: String
) : Parcelable {

    companion object {
        /** The "images" array is occasionally empty, so an empty list is normal, not an error. */
        fun listFrom(array: JSONArray?): List<NpsImage> {
            if (array == null) return emptyList()
            return (0 until array.length())
                .mapNotNull { array.optJSONObject(it) }
                .map { image ->
                    NpsImage(
                        url = image.cleanString("url"),
                        title = image.cleanString("title"),
                        caption = image.cleanString("caption"),
                        altText = image.cleanString("altText"),
                        credit = image.cleanString("credit")
                    )
                }
                .filter { it.url.isNotEmpty() }
        }
    }
}

/**
 * optString() returns the text "null" for JSON nulls; treat those as empty instead.
 */
internal fun JSONObject.cleanString(key: String): String {
    if (!has(key) || isNull(key)) return ""
    return optString(key).trim()
}

/** "34" -> 34. Blank or non-numeric values count as 0. */
internal fun JSONObject.cleanInt(key: String): Int = cleanString(key).toIntOrNull() ?: 0

/** Every string in a JSON array of strings, trimmed, blanks dropped. */
internal fun JSONObject.cleanStringList(key: String): List<String> {
    val array = optJSONArray(key) ?: return emptyList()
    return (0 until array.length())
        .map { array.optString(it).trim() }
        .filter { it.isNotEmpty() && it != "null" }
}
