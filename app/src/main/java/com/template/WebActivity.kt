package com.template

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Window
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import java.util.*


class WebActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var fcm: FirebaseMessaging

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)

        setContentView(R.layout.activity_web)
        webView = findViewById(R.id.webView)
        val extras = intent.extras

        analytics = Firebase.analytics
        fcm = FirebaseMessaging.getInstance()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (!isGranted) {
                    Snackbar.make(
                        findViewById(R.id.icon_image),
                        "Открыть настройки для разрешения уведомлений",
                        Snackbar.LENGTH_LONG
                    ).setAction("Настройки") {
                        startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                data = Uri.fromParts("package", packageName, null)
                            }
                        )
                    }.show()

                }
            }
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED)
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        val url = extras?.getString("url") + getUserAgent()
        val mapHeaders = mapOf("User-agent" to getUserAgent())

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                view?.loadUrl(request?.url.toString(), mapHeaders)
                return true
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                CookieManager.getInstance().flush()
                super.onPageFinished(view, url)
            }
        }

        webView.settings.apply {
            javaScriptEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            domStorageEnabled = true
            databaseEnabled = true
            setSupportZoom(true)
            allowFileAccess = true
            allowContentAccess = true
            javaScriptCanOpenWindowsAutomatically = true
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            url?.let { webView.loadUrl(it, mapHeaders) }
        }
    }

    private fun getUserAgent(): String = webView.settings.userAgentString

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onBackPressed() {
        if (webView.canGoBack())
            webView.goBack()
    }
}