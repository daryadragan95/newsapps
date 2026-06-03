package com.newswinnerapp.app.ui.viewmodel

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.newswinnerapp.app.data.web.WebConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class WebGateViewModel(
    private val webConfigRepository: WebConfigRepository,
    context: Context,
) : ViewModel() {
    private val appContext = context.applicationContext

    sealed interface AppState {
        data object Loading : AppState
        data class WebView(val url: String) : AppState
        data object NormalApp : AppState
    }

    private val _appState = MutableStateFlow<AppState>(AppState.Loading)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    init {
        checkWebViewUrl()
    }

    private fun checkWebViewUrl() {
        viewModelScope.launch {
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val cachedUrl = prefs.getString(CACHED_URL, null)

            runCatching {
                withTimeoutOrNull(10_000L) {
                    webConfigRepository.getWebViewUrl()
                }
            }.fold(
                onSuccess = { url ->
                    when {
                        !url.isNullOrBlank() -> {
                            prefs.edit { putString(CACHED_URL, url) }
                            _appState.value = AppState.WebView(url)
                        }

                        else -> {
                            _appState.value = if (!cachedUrl.isNullOrBlank()) {
                                AppState.WebView(cachedUrl)
                            } else {
                                AppState.NormalApp
                            }
                        }
                    }
                },
                onFailure = {
                    _appState.value = if (!cachedUrl.isNullOrBlank()) {
                        AppState.WebView(cachedUrl)
                    } else {
                        AppState.NormalApp
                    }
                },
            )
        }
    }

    private companion object {
        const val PREFS_NAME = "webview_prefs"
        const val CACHED_URL = "cached_url"
    }
}

class WebGateViewModelFactory(
    private val webConfigRepository: WebConfigRepository,
    private val context: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WebGateViewModel(webConfigRepository, context) as T
    }
}
