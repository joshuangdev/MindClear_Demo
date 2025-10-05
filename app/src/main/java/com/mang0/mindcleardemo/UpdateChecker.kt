package com.mang0.mindcleardemo

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URL

object UpdateChecker {

    private const val VERSION_URL = "https://raw.githubusercontent.com/mang0incc/MindClear_Demo/master/version.json"


    fun checkForUpdate(context: Context, currentVersion: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = URL(VERSION_URL).readText()
                val latestVersion = JSONObject(json).getLong("versionCode")
                val downloadUrl = JSONObject(json).getString("downloadUrl")

                if (latestVersion > currentVersion) {
                    CoroutineScope(Dispatchers.Main).launch {
                        AlertDialog.Builder(context)
                            .setTitle("Güncelleme Mevcut")
                            .setMessage("Yeni bir sürüm mevcut! Güncellemek ister misiniz?")
                            .setPositiveButton("Evet") { _, _ ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                                context.startActivity(intent)
                            }
                            .setNegativeButton("Hayır", null)
                            .show()
                    }
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Güncelleme kontrolü başarısız.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
