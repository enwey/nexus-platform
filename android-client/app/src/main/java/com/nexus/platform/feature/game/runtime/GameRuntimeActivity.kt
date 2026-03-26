package com.nexus.platform.feature.game.runtime

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import androidx.webkit.WebViewAssetLoader.InternalStoragePathHandler
import com.nexus.platform.R
import com.nexus.platform.core.bridge.NexusBridge
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.feature.game.data.GameManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameRuntimeActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var runtimeLoading: LinearLayout
    private lateinit var gameManager: GameManager
    private lateinit var nexusBridge: NexusBridge
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        private const val EXTRA_GAME = "game"

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

        val game = intent.getSerializableExtra(EXTRA_GAME) as? GameItem
        if (game == null) {
            finish()
            return
        }

        initViews()
        initWebView()
        initBridge()
        loadGame(game)
    }

    private fun initViews() {
        webView = findViewById(R.id.gameWebView)
        runtimeLoading = findViewById(R.id.runtimeLoading)
        findViewById<TextView>(R.id.capsuleClose).setOnClickListener { finish() }
        gameManager = GameManager(this)
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

    private fun loadGame(game: GameItem) {
        scope.launch {
            try {
                showLoading(true)
                val unzipDir = gameManager.getGameDir(game.id)

                if (!gameManager.isGameDownloaded(game.id)) {
                    withContext(Dispatchers.IO) {
                        gameManager.downloadGame(game, unzipDir)
                    }
                }

                val assetLoader = WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", InternalStoragePathHandler(this@GameRuntimeActivity, unzipDir))
                    .addPathHandler("/res/", AssetsPathHandler(this@GameRuntimeActivity))
                    .build()

                webView.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                        val uri = request?.url ?: return null
                        return assetLoader.shouldInterceptRequest(uri)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        injectSDK()
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val url = request?.url?.toString().orEmpty()
                        return !(url.startsWith("https://game.local/") || url.startsWith("https://appassets.androidplatform.net/"))
                    }
                }

                webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")
            } catch (e: Exception) {
                showError("Game load failed: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun injectSDK() {
        scope.launch(Dispatchers.IO) {
            val sdkContent = gameManager.readSDKContent()
            if (sdkContent.isBlank()) {
                return@launch
            }
            withContext(Dispatchers.Main) {
                webView.evaluateJavascript(sdkContent, null)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        webView.visibility = if (show) View.INVISIBLE else View.VISIBLE
        runtimeLoading.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        if (::nexusBridge.isInitialized) {
            nexusBridge.cleanup()
        }
        if (::webView.isInitialized) {
            webView.destroy()
        }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
