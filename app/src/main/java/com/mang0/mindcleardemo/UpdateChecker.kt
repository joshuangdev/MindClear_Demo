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

/**
 * Bu obje, uygulamanın güncel sürümünü kontrol etmek için kullanılır.
 * Github üzerinde barındırılan version.json dosyasından en son sürüm bilgisi alınır.
 */
object UpdateChecker {

    private const val VERSION_URL =
        "https://raw.githubusercontent.com/mang0incc/MindClear_Demo/master/version.json"
    // Sürüm bilgisi JSON dosyasının URL'si

    /**
     * Uygulamanın güncel sürümü ile sunucudaki sürümü karşılaştırır.
     * @param context: UI işlemleri için gerekli context
     * @param currentVersion: Mevcut uygulama sürüm kodu
     */
    fun checkForUpdate(context: Context, currentVersion: Long) {
        // Ağ işlemleri IO thread üzerinde yapılır
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = URL(VERSION_URL).readText() // JSON içeriğini al
                val latestVersion = JSONObject(json).getLong("versionCode") // En son sürüm kodu
                val downloadUrl = JSONObject(json).getString("downloadUrl") // Güncelleme linki

                // Eğer sunucudaki sürüm daha yeni ise kullanıcıya uyarı göster
                if (latestVersion > currentVersion) {
                    CoroutineScope(Dispatchers.Main).launch {
                        AlertDialog.Builder(context)
                            .setTitle("Güncelleme Mevcut")
                            .setMessage("Yeni bir sürüm mevcut! Güncellemek ister misiniz?")
                            .setPositiveButton("Evet") { _, _ ->
                                // Kullanıcı evet derse tarayıcıda güncelleme sayfasını aç
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                                context.startActivity(intent)
                            }
                            .setNegativeButton("Hayır", null)
                            .show()
                    }
                }
            } catch (e: Exception) {
                // Hata durumunda kullanıcıya toast mesajı göster
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Güncelleme kontrolü başarısız.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
