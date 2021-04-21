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
import com.atritripathi.happenstance.R
import com.atritripathi.happenstance.databinding.FragmentSearchNewsBinding
import com.atritripathi.happenstance.util.onQueryTextSubmit
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class SearchNewsFragment : Fragment(R.layout.fragment_search_news) {

    private val viewModel: SearchNewsViewModel by viewModels()

    private lateinit var newsArticleAdapter: NewsArticlePagingAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentSearchNewsBinding.bind(view)

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
                    tvInstructions.isVisible = false
                    swipeRefreshLayout.isEnabled = true

                    newsArticleAdapter.submitData(data)
                }
            }

            swipeRefreshLayout.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                newsArticleAdapter.loadStateFlow.collect { loadState ->
                    when (val refresh = loadState.mediator?.refresh) {
                        is LoadState.Loading -> {
                            tvError.isVisible = false
                            btnRetry.isVisible = false
                            swipeRefreshLayout.isRefreshing = true
                            tvNoResults.isVisible = false
                            rvBreakingNews.isVisible = newsArticleAdapter.itemCount > 0
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
        R.id.action_search -> {
            newsArticleAdapter.refresh()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}