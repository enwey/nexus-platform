package com.nexus.platform.feature.game.runtime

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
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
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.feature.game.data.GameManager
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
    private lateinit var nexusBridge: NexusBridge
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var loadJob: Job? = null
    private var currentGame: GameItem? = null

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
        findViewById<View>(R.id.capsuleMore).setOnClickListener {
            Toast.makeText(
                this,
                getString(R.string.runtime_capsule_more_todo),
                Toast.LENGTH_SHORT
            ).show()
        }
        runtimeRetry.setOnClickListener {
            val game = currentGame ?: return@setOnClickListener
            loadGame(game = game, forceRefresh = true)
        }
        gameManager = GameManager(this)
    }

    private fun initWindowInsets() {
        val baseTop = resources.getDimensionPixelSize(R.dimen.runtime_capsule_margin_top)
        ViewCompat.setOnApplyWindowInsetsListener(capsuleMenu) { view, insets ->
            val statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
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
        nexusBridge = NexusBridge(this, webView)
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
        loadJob?.cancel()
        loadJob = scope.launch {
            try {
                showLoading(message = getString(R.string.runtime_status_preparing), showRetry = false)
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
        scope.cancel()
        super.onDestroy()
    }
}
