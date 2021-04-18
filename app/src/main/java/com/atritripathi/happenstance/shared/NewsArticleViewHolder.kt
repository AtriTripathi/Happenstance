package com.atritripathi.happenstance.shared

import androidx.recyclerview.widget.RecyclerView
import com.atritripathi.happenstance.R
import com.atritripathi.happenstance.data.NewsArticle
import com.atritripathi.happenstance.databinding.ItemNewsArticleBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class NewsArticleViewHolder(
    private val binding: ItemNewsArticleBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(article: NewsArticle) {
        binding.apply {
            Glide.with(itemView)
                .load(article.thumbnailUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.ic_image_broken)
                .into(ivArticle)

            tvTitle.text = article.title ?: ""
            ivBookmark.setImageResource(
                when (article.isBookmarked) {
                    true -> R.drawable.ic_bookmark_selected
                    false -> R.drawable.ic_bookmark_unselected
                }
            )
        }
    }
}