package com.nexus.platform.feature.common.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nexus.platform.R

class LegalWebViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { getString(R.string.app_name) }
        val url = intent.getStringExtra(EXTRA_URL).orEmpty()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F1320"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val back = TextView(this).apply {
            text = "<"
            setTextColor(Color.parseColor("#A7B0CD"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setPadding(dp(4), 0, dp(12), 0)
            setOnClickListener { finish() }
        }

        val titleView = TextView(this).apply {
            text = title
            setTextColor(Color.parseColor("#4AA3FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        }

        val webView = WebView(this).apply {
            settings.javaScriptEnabled = false
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {}
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            if (url.isNotBlank()) {
                loadUrl(url)
            } else {
                loadDataWithBaseURL(
                    null,
                    "<html><body><p>Empty url</p></body></html>",
                    "text/html",
                    "utf-8",
                    null
                )
            }
        }

        toolbar.addView(back)
        toolbar.addView(titleView)
        root.addView(toolbar)
        root.addView(webView)
        setContentView(root)
    }

    companion object {
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_URL = "extra_url"

        fun start(context: Context, title: String, url: String) {
            context.startActivity(
                Intent(context, LegalWebViewActivity::class.java)
                    .putExtra(EXTRA_TITLE, title)
                    .putExtra(EXTRA_URL, url)
            )
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
