package com.codepath.lab6

import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.codepath.lab6.databinding.ViewListStatusBinding

/** Implemented by both tabs so tapping the tab you're already on scrolls back to the top. */
interface ScrollsToTop {
    fun scrollToTop()
}

/** Shows a centered message (an error, or "no matches") instead of a blank screen. */
fun ViewListStatusBinding.show(message: String, showRetry: Boolean) {
    root.isVisible = true
    statusMessage.text = message
    retryButton.isVisible = showRetry
}

fun ViewListStatusBinding.hide() {
    root.isVisible = false
}

/** "Couldn't load campgrounds. Check your internet connection..." etc. */
fun Fragment.loadErrorMessage(error: LoadError, @StringRes thing: Int): String {
    val what = getString(thing)
    return when (error) {
        LoadError.NETWORK -> getString(R.string.error_network, what)
        LoadError.API_KEY -> getString(R.string.error_api_key)
        LoadError.RATE_LIMIT -> getString(R.string.error_rate_limit)
        LoadError.PARSE -> getString(R.string.error_parse, what)
    }
}

/** Forest-green pull-to-refresh spinner on a card-colored circle, in light and dark mode. */
fun SwipeRefreshLayout.styleForApp() {
    setColorSchemeResources(R.color.forest_green)
    setProgressBackgroundColorSchemeResource(R.color.card_background)
}
