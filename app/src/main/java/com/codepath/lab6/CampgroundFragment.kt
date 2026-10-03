package com.codepath.lab6

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.codepath.lab6.databinding.FragmentCampgroundBinding
import com.google.android.material.snackbar.Snackbar

/**
 * The Campgrounds tab: a searchable list of NPS campgrounds, drawn with the
 * same card style as the Parks tab.
 *
 * The networking lives in [CampgroundsViewModel]; this fragment only draws
 * whatever state the ViewModel is in (loading, error, the campgrounds, or "no matches").
 */
class CampgroundFragment : Fragment(), ScrollsToTop {

    private var _binding: FragmentCampgroundBinding? = null
    private val binding get() = _binding!!

    // Scoped to MainActivity, so the campgrounds are still here after a tab switch or rotation
    private val viewModel: CampgroundsViewModel by activityViewModels()

    private lateinit var campgroundAdapter: CampgroundAdapter
    private var restoredScroll = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCampgroundBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        campgroundAdapter = CampgroundAdapter { campground ->
            startActivity(CampgroundDetailActivity.newIntent(requireContext(), campground))
        }
        binding.campgroundRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.campgroundRecyclerView.setHasFixedSize(true)
        binding.campgroundRecyclerView.adapter = campgroundAdapter

        binding.swipeContainer.styleForApp()
        binding.swipeContainer.setOnRefreshListener { viewModel.load() }
        binding.status.retryButton.setOnClickListener { viewModel.load() }

        addSearchMenu(R.string.search_campgrounds_hint, { viewModel.query }) { query ->
            viewModel.query = query
            viewModel.state.value?.let { render(it, scrollToTop = true) }
        }

        viewModel.state.observe(viewLifecycleOwner) { render(it, scrollToTop = false) }
        viewModel.loadIfNeeded()
    }

    override fun onDestroyView() {
        // Remember the scroll position for when the user comes back to this tab
        viewModel.listPosition = binding.campgroundRecyclerView.layoutManager?.onSaveInstanceState()
        super.onDestroyView()
        _binding = null
    }

    override fun scrollToTop() {
        _binding?.campgroundRecyclerView?.smoothScrollToPosition(0)
    }

    private fun render(state: ListState<Campground>, scrollToTop: Boolean) {
        val results = CampgroundSearch.filter(state.items, viewModel.query)
        campgroundAdapter.submitList(results) {
            val binding = _binding ?: return@submitList
            if (scrollToTop) {
                binding.campgroundRecyclerView.scrollToPosition(0)
            } else if (!restoredScroll && results.isNotEmpty()) {
                restoredScroll = true
                viewModel.listPosition?.let {
                    binding.campgroundRecyclerView.layoutManager?.onRestoreInstanceState(it)
                }
            }
        }

        val hasCampgrounds = state.items.isNotEmpty()
        binding.progressBar.isVisible = state.isLoading && !hasCampgrounds
        binding.swipeContainer.isRefreshing = state.isLoading && hasCampgrounds

        val error = state.error
        when {
            // Nothing to show: explain why, with a Retry button
            !hasCampgrounds && error != null ->
                binding.status.show(loadErrorMessage(error, R.string.campgrounds), showRetry = true)

            hasCampgrounds && results.isEmpty() ->
                binding.status.show(
                    getString(R.string.no_campgrounds_match, viewModel.query.trim()),
                    showRetry = false
                )

            !hasCampgrounds && !state.isLoading ->
                binding.status.show(getString(R.string.no_campgrounds_found), showRetry = true)

            else -> binding.status.hide()
        }

        // A pull-to-refresh failed but the old campgrounds are still on screen
        if (hasCampgrounds && error != null) {
            Snackbar.make(binding.root, loadErrorMessage(error, R.string.campgrounds), Snackbar.LENGTH_LONG).show()
            viewModel.errorShown()
        }
    }

    companion object {
        fun newInstance(): CampgroundFragment {
            return CampgroundFragment()
        }
    }
}
