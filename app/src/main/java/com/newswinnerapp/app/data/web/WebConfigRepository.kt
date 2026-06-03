package com.newswinnerapp.app.data.web

interface WebConfigRepository {
    suspend fun getWebViewUrl(): String?
}
