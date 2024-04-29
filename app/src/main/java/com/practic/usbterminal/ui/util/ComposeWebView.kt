package com.practic.usbterminal.ui.util

import android.content.Intent
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import java.nio.charset.Charset

@Composable
fun ComposeWebView(
    url: String,
    onPageLoaded: ((url: String?) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var backEnabled by remember { mutableStateOf(false) }
    var webView: WebView? = null

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                val assetLoader = WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                    .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(context))
                    .build()

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        request?.url ?: return false

                        if (assetLoader.shouldInterceptRequest(request.url) != null) {
                            return false
                        }

                        context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                        return true
                    }

                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        if (request.url.toString().lowercase().endsWith("/favicon.ico")) {
                            // Timber.d("shouldInterceptRequest() returning empty response for FAVICON URL='${request.url}'")
                            val inputStream = "".byteInputStream(Charset.defaultCharset())
                            return WebResourceResponse("text", "UTF-8", inputStream)
                        }
                        return assetLoader.shouldInterceptRequest(request.url)
                    }

                    override fun onPageFinished(webView1: WebView?, url: String?) {
                        backEnabled = webView1?.canGoBack() ?: false
                        onPageLoaded?.invoke(url)
                    }
                }

                settings.allowFileAccess = false
                settings.allowContentAccess = false
                loadUrl(url)
            }
        },
        update = {
            webView = it
        }
    )
    BackHandler(enabled = backEnabled) {
        webView?.goBack()
    }
}