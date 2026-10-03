package com.codepath.lab6

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.annotation.StringRes
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle

/**
 * Adds a search box to the toolbar while this fragment's tab is showing.
 *
 * The menu is tied to the fragment's view lifecycle, so switching tabs swaps
 * the Parks search for the Campgrounds search automatically.
 *
 * @param currentQuery the text to restore when the menu is (re)built
 * @param onQueryChanged called as the user types
 */
fun Fragment.addSearchMenu(
    @StringRes hint: Int,
    currentQuery: () -> String,
    onQueryChanged: (String) -> Unit
) {
    requireActivity().addMenuProvider(object : MenuProvider {

        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_search, menu)

            val searchItem = menu.findItem(R.id.action_search)
            val searchView = searchItem.actionView as SearchView
            searchView.queryHint = getString(hint)
            searchView.maxWidth = Int.MAX_VALUE

            // Re-open the search box with the old text (after a rotation or tab switch)
            val restored = currentQuery()
            if (restored.isNotEmpty()) {
                searchItem.expandActionView()
                searchView.setQuery(restored, false)
                searchView.clearFocus()
            }

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    searchView.clearFocus() // hide the keyboard
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    // When the user switches tabs, the toolbar collapses this search box,
                    // which clears its text. Ignore that so the tab keeps its search.
                    if (!isResumed) return true
                    onQueryChanged(newText.orEmpty())
                    return true
                }
            })
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false

    }, viewLifecycleOwner, Lifecycle.State.RESUMED)
}
