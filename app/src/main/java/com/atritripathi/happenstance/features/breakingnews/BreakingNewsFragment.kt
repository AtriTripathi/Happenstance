package com.atritripathi.happenstance.features.breakingnews

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.atritripathi.happenstance.R
import com.atritripathi.happenstance.databinding.FragmentBreakingNewsBinding
import com.atritripathi.happenstance.shared.NewsArticleListAdapter
import com.atritripathi.happenstance.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class BreakingNewsFragment : Fragment(R.layout.fragment_breaking_news) {

    private val viewModel: BreakingNewsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentBreakingNewsBinding.bind(view)

        val newsArticleAdapter = NewsArticleListAdapter()

        binding.apply {
            rvBreakingNews.apply {
                adapter = newsArticleAdapter
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(true)
            }

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                viewModel.breakingNews.collect {
                    val result = it ?: return@collect

                    swipeRefreshLayout.isRefreshing = result is Resource.Loading
                    rvBreakingNews.isVisible = !result.data.isNullOrEmpty()
                    tvError.isVisible = result.error != null && result.data.isNullOrEmpty()
                    tvError.text = getString(
                        R.string.could_not_refresh,
                        result.error?.localizedMessage ?: getString(R.string.unknown_error_occurred)
                    )
                    btnRetry.isVisible = result.error != null && result.data.isNullOrEmpty()

                    newsArticleAdapter.submitList(result.data)
                }
            }

            swipeRefreshLayout.setOnRefreshListener {
                viewModel.onManualRefresh()
            }

            btnRetry.setOnClickListener {
                viewModel.onManualRefresh()
            }

        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }
}