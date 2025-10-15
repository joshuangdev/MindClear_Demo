// Bu sınıf, cihazda yüklü olan uygulamaları temsil eder
// Her AppInfo nesnesi bir uygulamayı (adı, paket adı, ikonu ve seçili durumu) tutar

package com.mang0.mindcleardemo

import android.graphics.drawable.Drawable // Uygulama ikonlarını göstermek için kullanılır

// 'data class' otomatik olarak equals(), hashCode() ve toString() gibi metotları üretir
data class AppInfo(
    val name: String,             // Uygulamanın görünen adı (örnek: "YouTube")
    val packageName: String,      // Uygulamanın sistemdeki benzersiz paket adı (örnek: "com.google.android.youtube")
    val icon: Drawable,            // Uygulamanın simgesi (görsel)
    var isSelected: Boolean = false // Kullanıcı tarafından seçilip seçilmediğini belirten durum (varsayılan: false)
)
