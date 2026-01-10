package com.cleanfinding.browser

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BookmarkManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("bookmarks", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getBookmarks(): MutableList<Bookmark> {
        val json = prefs.getString("bookmark_list", null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<Bookmark>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addBookmark(bookmark: Bookmark) {
        val bookmarks = getBookmarks()
        // Check if already bookmarked
        if (bookmarks.none { it.url == bookmark.url }) {
            bookmarks.add(0, bookmark)
            saveBookmarks(bookmarks)
        }
    }

    fun removeBookmark(url: String) {
        val bookmarks = getBookmarks()
        bookmarks.removeAll { it.url == url }
        saveBookmarks(bookmarks)
    }

    fun isBookmarked(url: String): Boolean {
        return getBookmarks().any { it.url == url }
    }

    private fun saveBookmarks(bookmarks: List<Bookmark>) {
        val json = gson.toJson(bookmarks)
        prefs.edit().putString("bookmark_list", json).apply()
    }
}
