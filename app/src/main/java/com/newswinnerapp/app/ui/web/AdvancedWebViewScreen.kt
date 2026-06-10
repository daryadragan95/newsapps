package com.newswinnerapp.app.ui.web

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import java.net.URISyntaxException
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AdvancedWebViewScreen(
    initialUrl: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("webview_cache", Context.MODE_PRIVATE) }
    val cachedUrl = remember { prefs.getString("cached_final_url", "") ?: "" }
    var currentUrl by remember { mutableStateOf(if (cachedUrl.isNotBlank()) cachedUrl else initialUrl) }
    val errorCounter = remember { AtomicInteger(0) }
    var lastErrorTime by remember { mutableLongStateOf(0L) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraLaunch by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }

    fun finishFileChooser(result: Array<Uri>) {
        filePathCallback?.onReceiveValue(result)
        filePathCallback = null
        cameraImageUri = null
    }

    val takePhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val result = if (success && cameraImageUri != null) arrayOf(cameraImageUri!!) else emptyArray()
        finishFileChooser(result)
    }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result: ActivityResult ->
        if (result.resultCode != Activity.RESULT_OK) {
            finishFileChooser(emptyArray())
            return@rememberLauncherForActivityResult
        }

        val data = result.data
        val uris = when {
            data?.clipData != null -> {
                val clipData = data.clipData!!
                Array(clipData.itemCount) { index -> clipData.getItemAt(index).uri }
            }

            data?.data != null -> arrayOf(data.data!!)
            cameraImageUri != null -> arrayOf(cameraImageUri!!)
            else -> emptyArray()
        }
        finishFileChooser(uris)
    }

    val requestWritePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // The user can tap the same download link again after granting permission.
    }

    val requestCameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            pendingCameraLaunch?.invoke()
        } else {
            finishFileChooser(emptyArray())
        }
        pendingCameraLaunch = null
    }

    val requestWebViewPermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grantResults ->
        val request = pendingPermissionRequest ?: return@rememberLauncherForActivityResult
        pendingPermissionRequest = null

        val grantedResources = request.resources.orEmpty().mapNotNull { resource ->
            when (resource) {
                PermissionRequest.RESOURCE_VIDEO_CAPTURE ->
                    resource.takeIf { grantResults[Manifest.permission.CAMERA] == true }

                PermissionRequest.RESOURCE_AUDIO_CAPTURE ->
                    resource.takeIf { grantResults[Manifest.permission.RECORD_AUDIO] == true }

                else -> null
            }
        }.toTypedArray()

        if (grantedResources.isNotEmpty()) {
            request.grant(grantedResources)
        } else {
            request.deny()
        }
    }

    fun grantWebViewPermissions(ctx: Context, request: PermissionRequest) {
        val resources = request.resources.orEmpty()
        val permissionsToRequest = buildList {
            if (
                PermissionRequest.RESOURCE_VIDEO_CAPTURE in resources &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.CAMERA)
            }
            if (
                PermissionRequest.RESOURCE_AUDIO_CAPTURE in resources &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.RECORD_AUDIO)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            pendingPermissionRequest?.deny()
            pendingPermissionRequest = request
            requestWebViewPermissions.launch(permissionsToRequest.toTypedArray())
            return
        }

        val grantedResources = resources.filter { resource ->
            resource == PermissionRequest.RESOURCE_VIDEO_CAPTURE ||
                resource == PermissionRequest.RESOURCE_AUDIO_CAPTURE
        }.toTypedArray()

        if (grantedResources.isNotEmpty()) {
            request.grant(grantedResources)
        } else {
            request.deny()
        }
    }

    fun createCameraImageUri(ctx: Context): Uri {
        val imageFile = File(ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "camera_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", imageFile)
    }

    fun launchCamera(ctx: Context) {
        val launch = {
            val imageUri = createCameraImageUri(ctx)
            cameraImageUri = imageUri
            takePhotoLauncher.launch(imageUri)
        }
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launch()
        } else {
            pendingCameraLaunch = launch
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    fun createFileChooserIntent(ctx: Context, fileChooserParams: WebChromeClient.FileChooserParams?): Intent {
        val baseIntent = runCatching {
            fileChooserParams?.createIntent()
        }.getOrNull() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        val accept = fileChooserParams?.acceptTypes?.joinToString(",").orEmpty()
        val acceptsImage = accept.isBlank() || accept.contains("image") || accept.contains("*/*")
        val chooser = Intent.createChooser(baseIntent, "Select file")

        if (acceptsImage) {
            val imageUri = createCameraImageUri(ctx)
            cameraImageUri = imageUri
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
        }

        return chooser
    }

    fun handleExternalUrl(ctx: Context, url: String): Boolean {
        val uri = runCatching { url.toUri() }.getOrNull() ?: return false
        val scheme = uri.scheme.orEmpty()
        if (scheme == "http" || scheme == "https") return false

        if (scheme == "intent") {
            return try {
                val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                if (intent.resolveActivity(ctx.packageManager) != null) {
                    ctx.startActivity(intent)
                } else if (!fallbackUrl.isNullOrBlank()) {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, fallbackUrl.toUri()))
                }
                true
            } catch (_: ActivityNotFoundException) {
                true
            } catch (_: URISyntaxException) {
                true
            }
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(ctx.packageManager) != null) {
                ctx.startActivity(intent)
            }
            true
        } catch (_: ActivityNotFoundException) {
            true
        }
    }

    fun startDownload(
        ctx: Context,
        url: String,
        contentDisposition: String?,
        mimeType: String?,
        userAgent: String?,
    ) {
        try {
            val downloadManager = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(url.toUri())
            val cookies = runCatching { CookieManager.getInstance().getCookie(url) }.getOrNull()
            if (!cookies.isNullOrBlank()) request.addRequestHeader("Cookie", cookies)
            if (!userAgent.isNullOrBlank()) request.addRequestHeader("User-Agent", userAgent)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setAllowedOverMetered(true)
            request.setAllowedOverRoaming(true)
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                URLUtil.guessFileName(url, contentDisposition, mimeType),
            )
            @Suppress("DEPRECATION")
            request.allowScanningByMediaScanner()
            downloadManager.enqueue(request)
        } catch (_: Throwable) {
            runCatching {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
            }
        }
    }

    AndroidView(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.systemBars)
            .fillMaxSize(),
        factory = { ctx ->
            val container = FrameLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }

            val wv = WebView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = false
                settings.textZoom = 100
                settings.setSupportZoom(false)
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                settings.allowContentAccess = true
                settings.allowFileAccess = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    settings.safeBrowsingEnabled = true
                }
                setInitialScale(100)
                CookieManager.getInstance().setAcceptCookie(true)
                runCatching { CookieManager.getInstance().setAcceptThirdPartyCookies(this, true) }

                setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        requestWritePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                    startDownload(ctx, url, contentDisposition, mimeType, userAgent)
                })

                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest) {
                        grantWebViewPermissions(ctx, request)
                    }

                    override fun onPermissionRequestCanceled(request: PermissionRequest) {
                        if (pendingPermissionRequest === request) {
                            pendingPermissionRequest = null
                        }
                        super.onPermissionRequestCanceled(request)
                    }

                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback_: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?,
                    ): Boolean {
                        filePathCallback?.onReceiveValue(null)
                        filePathCallback = filePathCallback_
                        val accept = fileChooserParams?.acceptTypes?.joinToString(",") ?: "*/*"

                        if (accept.contains("image") && fileChooserParams?.isCaptureEnabled == true) {
                            launchCamera(ctx)
                        } else {
                            fileChooserLauncher.launch(createFileChooserIntent(ctx, fileChooserParams))
                        }
                        return true
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean {
                        val url = request?.url?.toString() ?: return false
                        return handleExternalUrl(ctx, url)
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        val cleaned = url
                            .replace("&&", "&")
                            .replace("?&", "?")
                            .replace("??", "?")
                        currentUrl = cleaned
                        if (cleaned.startsWith("http")) {
                            prefs.edit { putString("cached_final_url", cleaned) }
                        }
                        CookieManager.getInstance().flush()
                        super.onPageFinished(view, url)
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?,
                    ) {
                        if (request?.isForMainFrame == true) bumpError()
                        super.onReceivedHttpError(view, request, errorResponse)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?,
                    ) {
                        if (request?.isForMainFrame == true) bumpError()
                        super.onReceivedError(view, request, error)
                    }

                    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                        runCatching {
                            (view.parent as? ViewGroup)?.removeView(view)
                            view.destroy()
                        }
                        webView = null
                        return true
                    }

                    fun bumpError() {
                        val now = System.currentTimeMillis()
                        val last = lastErrorTime
                        if (now - last <= 5_000L) {
                            errorCounter.incrementAndGet()
                        } else {
                            errorCounter.set(1)
                        }
                        lastErrorTime = now
                    }
                }

                loadUrl(currentUrl)
                webView = this
            }

            container.addView(wv)
            container
        },
        update = { container ->
            val wv = container.getChildAt(0) as? WebView
            if (wv != null && wv.url != currentUrl) {
                wv.loadUrl(currentUrl)
            }
        },
        onRelease = { container ->
            val wv = container.getChildAt(0) as? WebView
            runCatching { wv?.stopLoading() }
            runCatching { wv?.destroy() }
        },
    )

    BackHandler(enabled = true) {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            (context as? Activity)?.moveTaskToBack(true)
        }
    }
}
