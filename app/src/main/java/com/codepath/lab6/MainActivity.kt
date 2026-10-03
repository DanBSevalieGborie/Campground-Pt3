package com.codepath.lab6

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.codepath.lab6.databinding.ActivityMainBinding

/**
 * Hosts the two tabs. In this fragment-oriented setup the activity only
 * manages navigation: the toolbar, the bottom navigation bar, and swapping
 * [ParksFragment] / [CampgroundFragment] into the FrameLayout placeholder.
 * Each fragment loads and shows its own data.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Handle navigation selection: swap in the fragment for the tapped tab
        binding.bottomNavigation.setOnItemSelectedListener { item -> showTab(item.itemId) }

        // Tapping the tab you're already on scrolls its list back to the top
        // (instead of reloading the same fragment)
        binding.bottomNavigation.setOnItemReselectedListener {
            (supportFragmentManager.findFragmentById(R.id.main_frame_layout) as? ScrollsToTop)?.scrollToTop()
        }

        // Start on the Parks tab — only on a fresh start. After a rotation the
        // FragmentManager and the bottom bar restore the tab the user was on.
        // Parks is the bar's first item, so it's already selected: setting
        // selectedItemId to it would count as a *reselect* and never load the
        // fragment. So load it directly.
        if (savedInstanceState == null) {
            showTab(R.id.nav_parks)
        }
    }

    private fun showTab(@IdRes tabId: Int): Boolean {
        val fragment: Fragment = when (tabId) {
            R.id.nav_parks -> ParksFragment.newInstance()
            R.id.nav_campgrounds -> CampgroundFragment.newInstance()
            else -> return false
        }
        replaceFragment(fragment)
        updateTitle(tabId)
        return true
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // The bottom bar has restored its selected tab by now; match the toolbar to it
        updateTitle(binding.bottomNavigation.selectedItemId)
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.main_frame_layout, fragment)
        fragmentTransaction.commit()
    }

    private fun updateTitle(@IdRes tabId: Int) {
        supportActionBar?.title = getString(
            if (tabId == R.id.nav_campgrounds) R.string.title_campgrounds else R.string.title_parks
        )
    }
}
