package com.atritripathi.happenstance.features.searchnews

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.atritripathi.happenstance.MainActivity.OnBottomNavigationReselectedListener
import com.atritripathi.happenstance.R
import com.atritripathi.happenstance.databinding.FragmentSearchNewsBinding
import com.atritripathi.happenstance.util.onQueryTextSubmit
import com.atritripathi.happenstance.util.showIfOrInvisible
import com.atritripathi.happenstance.util.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class SearchNewsFragment : Fragment(R.layout.fragment_search_news),
    OnBottomNavigationReselectedListener {

    private val viewModel: SearchNewsViewModel by viewModels()

    private var _binding: FragmentSearchNewsBinding? = null
    private val binding get() = _binding!!

    private lateinit var newsArticleAdapter: NewsArticlePagingAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentSearchNewsBinding.bind(view)

        newsArticleAdapter = NewsArticlePagingAdapter(
            onItemClick = { article ->
                val uri = Uri.parse(article.url)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                requireActivity().startActivity(intent)
            },
            onBookmarkClick = { article ->
                viewModel.onBookmarkToggle(article)
            }
        )

        binding.apply {
            rvBreakingNews.apply {
                adapter = newsArticleAdapter.withLoadStateFooter(
                    NewsArticleLoadStateAdapter(newsArticleAdapter::retry)
                )
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(true)
                itemAnimator?.changeDuration = 0
            }

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                viewModel.searchResults.collectLatest { data ->
                    newsArticleAdapter.submitData(data)
                }
            }

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                viewModel.hasCurrentQuery.collect { hasCurrentQuery ->
                    tvInstructions.isVisible = !hasCurrentQuery
                    swipeRefreshLayout.isEnabled = hasCurrentQuery

                    if (!hasCurrentQuery) {
                        rvBreakingNews.isVisible = false
                    }
                }
            }


            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                newsArticleAdapter.loadStateFlow
                    .distinctUntilChangedBy { it.source.refresh }
                    .filter { it.source.refresh is LoadState.NotLoading }
                    .collect {
                        if (viewModel.pendingScrollToTopAfterNewQuery) {
                            rvBreakingNews.scrollToPosition(0)
                            viewModel.pendingScrollToTopAfterNewQuery = false
                        }
                        if (viewModel.pendingScrollToTopAfterRefresh && it.mediator?.refresh is LoadState.NotLoading) {
                            rvBreakingNews.scrollToPosition(0)
                            viewModel.pendingScrollToTopAfterRefresh = false
                        }
                    }
            }

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                newsArticleAdapter.loadStateFlow.collect { loadState ->
                    when (val refresh = loadState.mediator?.refresh) {
                        is LoadState.Loading -> {
                            tvError.isVisible = false
                            btnRetry.isVisible = false
                            swipeRefreshLayout.isRefreshing = true
                            tvNoResults.isVisible = false

                            rvBreakingNews.showIfOrInvisible {
                                !viewModel.newQueryInProgress && newsArticleAdapter.itemCount > 0
                            }

                            viewModel.refreshInProgress = true
                            viewModel.pendingScrollToTopAfterRefresh = true
                        }
                        is LoadState.NotLoading -> {
                            tvError.isVisible = false
                            btnRetry.isVisible = false
                            swipeRefreshLayout.isRefreshing = false
                            rvBreakingNews.isVisible = newsArticleAdapter.itemCount > 0

                            val noResults = newsArticleAdapter.itemCount < 1
                                    && loadState.append.endOfPaginationReached
                                    && loadState.source.append.endOfPaginationReached

                            tvNoResults.isVisible = noResults

                            viewModel.refreshInProgress = false
                            viewModel.newQueryInProgress = false
                        }
                        is LoadState.Error -> {
                            swipeRefreshLayout.isRefreshing = false
                            tvNoResults.isVisible = false
                            rvBreakingNews.isVisible = newsArticleAdapter.itemCount > 0

                            val noCachedResults = newsArticleAdapter.itemCount < 1
                                    && loadState.source.append.endOfPaginationReached

                            tvError.isVisible = noCachedResults
                            btnRetry.isVisible = noCachedResults

                            val errorMessage = getString(
                                R.string.could_not_load_search_results,
                                refresh.error.localizedMessage
                                    ?: getString(R.string.unknown_error_occurred)
                            )
                            tvError.text = errorMessage

                            if (viewModel.refreshInProgress) {
                                showSnackbar(errorMessage)
                            }
                            viewModel.refreshInProgress = false
                            viewModel.pendingScrollToTopAfterRefresh = false
                            viewModel.newQueryInProgress = false
                        }
                    }
                }
            }

            swipeRefreshLayout.setOnRefreshListener {
                newsArticleAdapter.refresh()
            }


            btnRetry.setOnClickListener {
                newsArticleAdapter.retry()
            }
        }
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_search_news, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView

        searchView.onQueryTextSubmit { query ->
            viewModel.onSearchQuerySubmit(query)
            searchView.clearFocus()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_refresh -> {
            newsArticleAdapter.refresh()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onBottomNavigationReselected() {
        binding.rvBreakingNews.scrollToPosition(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvBreakingNews.adapter = null
        _binding = null
    }
}