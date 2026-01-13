package com.cleanfinding.browser

data class Bookmark(
    val id: Long = System.currentTimeMillis(),
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)
