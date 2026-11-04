package com.corpsign.standardizer

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

/**
 * Activity de navegacao corporativa.
 * - User-Agent corporativo (Chrome Mobile / Galaxy S23)
 * - Todo o trafego de saida e roteado via SOCKS5 para o VPS em Ararangua
 *   (interceptacao de requests pelo OkHttp com proxy SOCKS).
 * - Injecao de corp_metadata.js em todas as paginas.
 */
class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val targetUrl: String by lazy {
        intent.getStringExtra(EXTRA_URL) ?: CorpProfile.CONTRACT_URL
    }

    // Cliente OkHttp roteado pelo SOCKS5 do VPS corporativo.
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress(CorpProfile.VPS_HOST, CorpProfile.VPS_SOCKS_PORT)))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        webView = findViewById(R.id.webView)

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        settings.userAgentString = CorpProfile.USER_AGENT
        settings.setSupportMultipleWindows(false)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null
                // Ignora esquemas nao-HTTP (data:, blob:, etc.)
                if (!url.startsWith("http://") && !url.startsWith("https://")) return null
                return try {
                    val reqBuilder = Request.Builder().url(url)
                    request.requestHeaders?.forEach { (k, v) -> reqBuilder.addHeader(k, v) }
                    val response = okHttpClient.newCall(reqBuilder.build()).execute()
                    val contentType = response.header("Content-Type") ?: "text/plain; charset=utf-8"
                    val mimeType = contentType.substringBefore(";").trim()
                    val charset = contentType.substringAfter("charset=", "utf-8").trim()
                    WebResourceResponse(mimeType, charset, response.body?.byteStream())
                } catch (e: Exception) {
                    null
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectMetadata(view)
            }
        }

        webView.loadUrl(targetUrl)
    }

    private fun injectMetadata(view: WebView?) {
        val js = try {
            assets.open("js/corp_metadata.js").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            ""
        }
        if (js.isNotBlank()) {
            view?.evaluateJavascript(js, null)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    companion object {
        const val EXTRA_URL = "extra_url"
    }
}