package com.atritripathi.happenstance.shared

import androidx.recyclerview.widget.DiffUtil
import com.atritripathi.happenstance.data.NewsArticle

object NewsArticleComparator : DiffUtil.ItemCallback<NewsArticle>() {
    override fun areItemsTheSame(oldItem: NewsArticle, newItem: NewsArticle) =
        oldItem.url == newItem.url

    override fun areContentsTheSame(oldItem: NewsArticle, newItem: NewsArticle) =
        oldItem == newItem
}