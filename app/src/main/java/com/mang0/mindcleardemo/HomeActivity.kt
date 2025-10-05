package com.mang0.mindcleardemo

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

data class AppInfo(val name: String, val packageName: String, val icon: Drawable)

class HomeActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var searchBar: EditText
    private val allApps = mutableListOf<AppInfo>()
    private val adapter = AppAdapter(this, mutableListOf())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        recycler = findViewById(R.id.appListView)
        searchBar = findViewById(R.id.searchBar)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        loadInstalledApps()

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Servisi başlat
        val serviceIntent = Intent(this, AppMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

    }

    private fun loadInstalledApps() {
        Thread {
            try {
                val pm = packageManager
                val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                    .filter { it.packageName != packageName }
                    .map {
                        AppInfo(
                            pm.getApplicationLabel(it).toString(),
                            it.packageName,
                            pm.getApplicationIcon(it)
                        )
                    }
                    .sortedBy { it.name.lowercase(Locale.getDefault()) }

                runOnUiThread {
                    allApps.clear()
                    allApps.addAll(apps)
                    adapter.updateList(allApps)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Uygulamalar yüklenirken hata: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun filterApps(query: String) {
        val q = query.trim().lowercase(Locale.getDefault())
        if (q.isEmpty()) adapter.updateList(allApps)
        else adapter.updateList(allApps.filter { it.name.lowercase(Locale.getDefault()).contains(q) })
    }


}
