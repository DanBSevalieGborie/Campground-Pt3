package com.codepath.lab6

import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.codepath.asynchttpclient.AsyncHttpClient
import com.codepath.asynchttpclient.RequestParams
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler
import okhttp3.Headers
import org.json.JSONArray
import org.json.JSONException

/** How many results to ask the API for (it defaults to 50). */
private const val PAGE_SIZE = "50"

/** Why a request failed; each one gets its own message on screen. */
enum class LoadError { NETWORK, API_KEY, RATE_LIMIT, PARSE }

/** Everything a list tab needs to draw itself. */
data class ListState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = false,
    /** Set when the latest request failed. The old items (if any) are kept. */
    val error: LoadError? = null
)

/**
 * Loads one NPS endpoint and holds the results.
 *
 * MainActivity swaps the tabs with replace(), which destroys the fragment you
 * leave. The ViewModel is scoped to the activity instead, so the data, the
 * search text and the scroll position survive both switching tabs and
 * rotating the phone — nothing is fetched twice and nothing resets.
 */
abstract class NpsListViewModel<T> : ViewModel() {

    /** e.g. "https://developer.nps.gov/api/v1/parks" */
    protected abstract val endpoint: String
    protected abstract fun parse(data: JSONArray): List<T>

    private val _state = MutableLiveData(ListState<T>())
    val state: LiveData<ListState<T>> = _state

    /** The tab's search text. */
    var query: String = ""

    /** Where the list was scrolled to when the user left the tab. */
    var listPosition: Parcelable? = null

    private val client = AsyncHttpClient()

    // AsyncHttpClient reports connection failures (like airplane mode) on a
    // background thread, so every result is handed to the main thread first.
    private val mainHandler = Handler(Looper.getMainLooper())
    private var hasStarted = false

    /** Called by the fragment every time its view is created; only the first call fetches. */
    fun loadIfNeeded() {
        if (hasStarted) return
        hasStarted = true
        load()
    }

    /** Fetches fresh data (first load, Retry, and pull-to-refresh). Must be called on the main thread. */
    fun load() {
        val current = _state.value ?: ListState()
        if (current.isLoading) return
        _state.value = current.copy(isLoading = true, error = null)

        val params = RequestParams()
        // The API wants "api_key" with an underscore; "api-key" gets a 403.
        params["api_key"] = BuildConfig.API_KEY
        params["limit"] = PAGE_SIZE

        client.get(endpoint, params, object : JsonHttpResponseHandler() {

            override fun onSuccess(statusCode: Int, headers: Headers, json: JSON) {
                val items = try {
                    parse(json.jsonObject.getJSONArray("data"))
                } catch (e: JSONException) {
                    Log.e(javaClass.simpleName, "Couldn't parse $endpoint", e)
                    null
                }
                mainHandler.post {
                    _state.value = if (items != null) {
                        ListState(items = items)
                    } else {
                        latest().copy(isLoading = false, error = LoadError.PARSE)
                    }
                }
            }

            override fun onFailure(
                statusCode: Int,
                headers: Headers?,
                errorResponse: String?,
                throwable: Throwable?
            ) {
                Log.e(javaClass.simpleName, "Request to $endpoint failed: $statusCode $errorResponse", throwable)
                val error = when (statusCode) {
                    401, 403 -> LoadError.API_KEY
                    429 -> LoadError.RATE_LIMIT
                    else -> LoadError.NETWORK
                }
                mainHandler.post {
                    _state.value = latest().copy(isLoading = false, error = error)
                }
            }
        })
    }

    /** The fragment calls this after showing a refresh error as a Snackbar, so it only shows once. */
    fun errorShown() {
        val current = _state.value ?: return
        if (current.error != null && current.items.isNotEmpty()) {
            _state.value = current.copy(error = null)
        }
    }

    private fun latest(): ListState<T> = _state.value ?: ListState()
}

class ParksViewModel : NpsListViewModel<NationalPark>() {
    override val endpoint = "https://developer.nps.gov/api/v1/parks"
    override fun parse(data: JSONArray) = NationalPark.fromJsonArray(data)
}

class CampgroundsViewModel : NpsListViewModel<Campground>() {
    override val endpoint = "https://developer.nps.gov/api/v1/campgrounds"
    override fun parse(data: JSONArray) = Campground.fromJsonArray(data)
}
