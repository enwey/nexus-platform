package com.nexus.platform.feature.game.runtime

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import androidx.webkit.WebViewAssetLoader.InternalStoragePathHandler
import com.nexus.platform.R
import com.nexus.platform.core.bridge.NexusBridge
import com.nexus.platform.core.bridge.RuntimeMetricsProvider
import com.nexus.platform.data.local.GameEngagementStore
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.feature.game.data.GameManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class GameRuntimeActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var capsuleMenu: LinearLayout
    private lateinit var runtimeLoading: LinearLayout
    private lateinit var runtimeStatus: TextView
    private lateinit var runtimeRetry: TextView
    private lateinit var gameManager: GameManager
    private lateinit var engagementStore: GameEngagementStore
    private lateinit var nexusBridge: NexusBridge
    private var runtimeMenuDialog: BottomSheetDialog? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var loadJob: Job? = null
    private var currentGame: GameItem? = null
    private var statusBarInsetPx: Int = 0
    private var navBarBottomInsetPx: Int = 0

    companion object {
        private const val EXTRA_GAME = "game"
        private const val APP_ASSET_BASE = "https://appassets.androidplatform.net/assets/"

        fun start(context: Context, game: GameItem) {
            val intent = Intent(context, GameRuntimeActivity::class.java).apply {
                putExtra(EXTRA_GAME, game)
                if (context !is AppCompatActivity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        val game = readGameFromIntent()
        if (game == null) {
            finish()
            return
        }
        currentGame = game

        initViews()
        initWindowInsets()
        initWebView()
        initBridge()
        initBackDispatcher()
        loadGame(game = game, forceRefresh = false)
    }

    private fun readGameFromIntent(): GameItem? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_GAME, GameItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_GAME) as? GameItem
        }
    }

    private fun initViews() {
        webView = findViewById(R.id.gameWebView)
        capsuleMenu = findViewById(R.id.capsuleMenu)
        runtimeLoading = findViewById(R.id.runtimeLoading)
        runtimeStatus = findViewById(R.id.runtimeStatus)
        runtimeRetry = findViewById(R.id.runtimeRetry)
        findViewById<View>(R.id.capsuleClose).setOnClickListener { finish() }
        findViewById<View>(R.id.capsuleMore).setOnClickListener { showRuntimeMenu() }
        runtimeRetry.setOnClickListener {
            val game = currentGame ?: return@setOnClickListener
            loadGame(game = game, forceRefresh = true)
        }
        gameManager = GameManager(this)
        engagementStore = GameEngagementStore(this)
    }

    private fun initWindowInsets() {
        val baseTop = resources.getDimensionPixelSize(R.dimen.runtime_capsule_margin_top)
        ViewCompat.setOnApplyWindowInsetsListener(capsuleMenu) { view, insets ->
            val statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            statusBarInsetPx = statusTop
            navBarBottomInsetPx = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val lp = view.layoutParams as FrameLayout.LayoutParams
            val targetTop = baseTop + statusTop
            if (lp.topMargin != targetTop) {
                lp.topMargin = targetTop
                view.layoutParams = lp
            }
            insets
        }
        ViewCompat.requestApplyInsets(capsuleMenu)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.webChromeClient = WebChromeClient()
    }

    private fun initBridge() {
        val metricsProvider = object : RuntimeMetricsProvider {
            override fun getSystemInfo(): Map<String, Any> {
                val density = resources.displayMetrics.density
                val windowWidthPx = if (webView.width > 0) webView.width else resources.displayMetrics.widthPixels
                val windowHeightPx = if (webView.height > 0) webView.height else resources.displayMetrics.heightPixels
                val windowWidth = windowWidthPx / density
                val windowHeight = windowHeightPx / density
                val statusBarHeight = statusBarInsetPx / density
                val safeBottom = windowHeight - (navBarBottomInsetPx / density)

                return mapOf(
                    "brand" to Build.BRAND,
                    "model" to Build.MODEL,
                    "pixelRatio" to density,
                    "screenWidth" to windowWidth,
                    "screenHeight" to windowHeight,
                    "windowWidth" to windowWidth,
                    "windowHeight" to windowHeight,
                    "statusBarHeight" to statusBarHeight,
                    "safeArea" to mapOf(
                        "left" to 0f,
                        "right" to windowWidth,
                        "top" to statusBarHeight,
                        "bottom" to safeBottom
                    ),
                    "language" to java.util.Locale.getDefault().language,
                    "version" to "1.0.0",
                    "system" to "Android ${Build.VERSION.RELEASE}",
                    "platform" to "android",
                    "fontSizeSetting" to 16,
                    "SDKVersion" to "1.0.0",
                    "benchmarkLevel" to 1,
                    "albumAuthorized" to true,
                    "cameraAuthorized" to true,
                    "locationAuthorized" to true,
                    "microphoneAuthorized" to true,
                    "notificationAuthorized" to true,
                    "bluetoothAuthorized" to true
                )
            }

            override fun getMenuButtonRect(): Map<String, Any> {
                val density = resources.displayMetrics.density
                val location = IntArray(2)
                capsuleMenu.getLocationOnScreen(location)
                val left = location[0] / density
                val top = location[1] / density
                val width = capsuleMenu.width / density
                val height = capsuleMenu.height / density
                return mapOf(
                    "width" to width,
                    "height" to height,
                    "left" to left,
                    "top" to top,
                    "right" to left + width,
                    "bottom" to top + height
                )
            }

            override suspend fun checkForUpdate(): Map<String, Any> {
                val game = currentGame ?: return mapOf(
                    "hasUpdate" to false,
                    "forceUpdate" to false,
                    "ready" to false,
                    "errMsg" to "update.check:fail"
                )
                val result = gameManager.checkForUpdate(game)
                return mapOf(
                    "hasUpdate" to result.hasUpdate,
                    "forceUpdate" to result.forceUpdate,
                    "ready" to result.ready,
                    "version" to (result.latestVersion ?: ""),
                    "errMsg" to result.errMsg
                )
            }

            override suspend fun applyUpdate(): Map<String, Any> {
                val game = currentGame ?: return mapOf(
                    "success" to false,
                    "errMsg" to "update.apply:fail"
                )
                val success = gameManager.applyUpdate(game.id, null)
                if (success) {
                    runOnUiThread {
                        recreate()
                    }
                }
                return mapOf(
                    "success" to success,
                    "errMsg" to if (success) "update.apply:ok" else "update.apply:fail"
                )
            }
        }

        nexusBridge = NexusBridge(this, webView, metricsProvider)
        webView.addJavascriptInterface(nexusBridge, "AndroidApp")
        webView.addJavascriptInterface(nexusBridge.createSyncBridge(), "AndroidAppSync")
    }

    private fun initBackDispatcher() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (::webView.isInitialized && webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        finish()
                    }
                }
            }
        )
    }

    private fun loadGame(game: GameItem, forceRefresh: Boolean) {
        engagementStore.markPlayed(game.id)
        loadJob?.cancel()
        loadJob = scope.launch {
            try {
                showLoading(message = getString(R.string.runtime_status_preparing), showRetry = false)
                val update = withContext(Dispatchers.IO) {
                    gameManager.checkForUpdate(game = game, blockOnForce = true)
                }
                if (update.forceUpdate && !update.ready) {
                    throw IOException("Game download failed: force update required")
                }
                val prepared = withContext(Dispatchers.IO) {
                    gameManager.prepareGame(game = game, forceRefresh = forceRefresh)
                }

                val status = if (prepared.fromCache) {
                    getString(R.string.runtime_status_cache)
                } else {
                    getString(R.string.runtime_status_loading_resource)
                }
                showLoading(message = status, showRetry = false)

                val assetLoader = WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", InternalStoragePathHandler(this@GameRuntimeActivity, prepared.rootDir))
                    .addPathHandler("/res/", AssetsPathHandler(this@GameRuntimeActivity))
                    .build()

                webView.webViewClient = buildWebViewClient(assetLoader)
                val launchUrl = APP_ASSET_BASE + prepared.entryRelativePath
                webView.loadUrl(launchUrl)
            } catch (e: Exception) {
                showFailure(error = e)
            }
        }
    }

    private fun showRuntimeMenu() {
        val game = currentGame ?: return
        val dialog = runtimeMenuDialog ?: BottomSheetDialog(this).also { sheet ->
            val root = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(0xFF1A1B23.toInt())
                setPadding(dp(20), dp(16), dp(20), dp(24))
            }
            root.addView(TextView(this).apply {
                text = game.name
                setTextColor(0xFFFFFFFF.toInt())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            })
            root.addView(menuItem(getString(R.string.runtime_menu_share_title)) {
                val text = "${game.name}\n${game.downloadUrl}"
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.runtime_menu_share_title)))
            })

            val favoriteItem = menuItem("", {})
            val refreshFavoriteLabel = {
                favoriteItem.text = if (engagementStore.isFavorite(game.id)) {
                    getString(R.string.runtime_menu_remove_favorite)
                } else {
                    getString(R.string.runtime_menu_add_favorite)
                }
            }
            refreshFavoriteLabel()
            favoriteItem.setOnClickListener {
                val nowFavorite = engagementStore.toggleFavorite(game.id)
                refreshFavoriteLabel()
                Toast.makeText(
                    this,
                    if (nowFavorite) getString(R.string.runtime_favorite_added) else getString(R.string.runtime_favorite_removed),
                    Toast.LENGTH_SHORT
                ).show()
            }
            root.addView(favoriteItem)

            root.addView(menuItem(getString(R.string.runtime_menu_restart)) {
                sheet.dismiss()
                loadGame(game = game, forceRefresh = true)
            })
            root.addView(menuItem(getString(R.string.runtime_menu_feedback)) {
                Toast.makeText(this, getString(R.string.runtime_feedback_todo), Toast.LENGTH_SHORT).show()
            })
            root.addView(menuItem(getString(R.string.common_cancel)) {
                sheet.dismiss()
            })

            sheet.setContentView(root)
            runtimeMenuDialog = sheet
        }
        dialog.show()
    }

    private fun menuItem(label: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            setTextColor(0xFFF3F4F6.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(0, dp(14), 0, dp(14))
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun buildWebViewClient(assetLoader: WebViewAssetLoader): WebViewClient {
        return object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val uri = request?.url ?: return null
                return assetLoader.shouldInterceptRequest(uri)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                showLoading(message = getString(R.string.runtime_status_starting), showRetry = false)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectSDK()
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                hideLoading()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString().orEmpty()
                return !(url.startsWith("https://appassets.androidplatform.net/") || url.startsWith("https://game.local/"))
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    val desc = error?.description?.toString().orEmpty()
                    showFailure(IOException("WebView load failed: $desc"))
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (request?.isForMainFrame == true) {
                    val code = errorResponse?.statusCode ?: -1
                    showFailure(IOException("WebView http error: $code"))
                }
            }
        }
    }

    private fun injectSDK() {
        scope.launch(Dispatchers.IO) {
            val sdkContent = gameManager.readSDKContent().trim()
            if (sdkContent.isBlank()) {
                return@launch
            }
            withContext(Dispatchers.Main) {
                if (!::webView.isInitialized) {
                    return@withContext
                }
                webView.evaluateJavascript(sdkContent, null)
            }
        }
    }

    private fun showLoading(message: String, showRetry: Boolean) {
        if (!::runtimeLoading.isInitialized) {
            return
        }
        webView.visibility = View.INVISIBLE
        runtimeLoading.visibility = View.VISIBLE
        runtimeStatus.text = message
        runtimeRetry.visibility = if (showRetry) View.VISIBLE else View.GONE
    }

    private fun hideLoading() {
        if (!::runtimeLoading.isInitialized) {
            return
        }
        webView.visibility = View.VISIBLE
        runtimeLoading.visibility = View.GONE
        runtimeRetry.visibility = View.GONE
    }

    private fun showFailure(error: Throwable) {
        val message = userVisibleError(error)
        showLoading(message = message, showRetry = true)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun userVisibleError(error: Throwable): String {
        val msg = error.message.orEmpty()
        return when {
            msg.contains("checksum", ignoreCase = true) -> getString(R.string.runtime_error_checksum)
            msg.contains("download", ignoreCase = true) -> getString(R.string.runtime_error_download)
            msg.contains("entry", ignoreCase = true) -> getString(R.string.runtime_error_entry_missing)
            msg.contains("WebView", ignoreCase = true) -> getString(R.string.runtime_error_webview)
            else -> getString(R.string.runtime_error_startup)
        }
    }

    override fun onDestroy() {
        loadJob?.cancel()
        if (::nexusBridge.isInitialized) {
            nexusBridge.cleanup()
        }
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.removeJavascriptInterface("AndroidApp")
            webView.removeJavascriptInterface("AndroidAppSync")
            webView.webChromeClient = null
            webView.webViewClient = WebViewClient()
            webView.destroy()
        }
        runtimeMenuDialog?.dismiss()
        runtimeMenuDialog = null
        scope.cancel()
        super.onDestroy()
    }
}
