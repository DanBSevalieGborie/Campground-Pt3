package com.codepath.lab6

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.codepath.lab6.databinding.FragmentParksBinding
import com.google.android.material.snackbar.Snackbar

/**
 * The Parks tab: a searchable list of national park sites.
 *
 * The networking lives in [ParksViewModel]; this fragment only draws whatever
 * state the ViewModel is in (loading, error, the parks, or "no matches").
 */
class ParksFragment : Fragment(), ScrollsToTop {

    private var _binding: FragmentParksBinding? = null
    private val binding get() = _binding!!

    // Scoped to MainActivity, so the parks are still here after a tab switch or rotation
    private val viewModel: ParksViewModel by activityViewModels()

    private lateinit var parksAdapter: ParksAdapter
    private var restoredScroll = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        parksAdapter = ParksAdapter { park ->
            startActivity(ParkDetailActivity.newIntent(requireContext(), park))
        }
        binding.parks.layoutManager = LinearLayoutManager(requireContext())
        binding.parks.setHasFixedSize(true)
        binding.parks.adapter = parksAdapter

        binding.swipeContainer.styleForApp()
        binding.swipeContainer.setOnRefreshListener { viewModel.load() }
        binding.status.retryButton.setOnClickListener { viewModel.load() }

        addSearchMenu(R.string.search_parks_hint, { viewModel.query }) { query ->
            viewModel.query = query
            viewModel.state.value?.let { render(it, scrollToTop = true) }
        }

        viewModel.state.observe(viewLifecycleOwner) { render(it, scrollToTop = false) }
        viewModel.loadIfNeeded()
    }

    override fun onDestroyView() {
        // Remember the scroll position for when the user comes back to this tab
        viewModel.listPosition = binding.parks.layoutManager?.onSaveInstanceState()
        super.onDestroyView()
        _binding = null
    }

    override fun scrollToTop() {
        _binding?.parks?.smoothScrollToPosition(0)
    }

    private fun render(state: ListState<NationalPark>, scrollToTop: Boolean) {
        val results = ParkSearch.filter(state.items, viewModel.query)
        parksAdapter.submitList(results) {
            val binding = _binding ?: return@submitList
            if (scrollToTop) {
                binding.parks.scrollToPosition(0)
            } else if (!restoredScroll && results.isNotEmpty()) {
                restoredScroll = true
                viewModel.listPosition?.let { binding.parks.layoutManager?.onRestoreInstanceState(it) }
            }
        }

        val hasParks = state.items.isNotEmpty()
        binding.progressBar.isVisible = state.isLoading && !hasParks
        binding.swipeContainer.isRefreshing = state.isLoading && hasParks

        val error = state.error
        when {
            // Nothing to show: explain why, with a Retry button
            !hasParks && error != null ->
                binding.status.show(loadErrorMessage(error, R.string.parks), showRetry = true)

            hasParks && results.isEmpty() ->
                binding.status.show(getString(R.string.no_parks_match, viewModel.query.trim()), showRetry = false)

            !hasParks && !state.isLoading ->
                binding.status.show(getString(R.string.no_parks_found), showRetry = true)

            else -> binding.status.hide()
        }

        // A pull-to-refresh failed but the old parks are still on screen
        if (hasParks && error != null) {
            Snackbar.make(binding.root, loadErrorMessage(error, R.string.parks), Snackbar.LENGTH_LONG).show()
            viewModel.errorShown()
        }
    }

    companion object {
        fun newInstance(): ParksFragment {
            return ParksFragment()
        }
    }
}
