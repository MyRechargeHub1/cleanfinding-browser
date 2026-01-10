package com.cleanfinding.browser

data class Tab(
    val id: Long = System.currentTimeMillis(),
    var url: String = "https://cleanfinding.com",
    var title: String = "New Tab",
    var isActive: Boolean = false
)
