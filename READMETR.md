# MindClearDemo

---

## ✨ Temel Özellikler

* **Bilinçli Engelleme Ekranı:** Kullanıcıyı bilgilendiren ve dikkatini dağıtıcı uygulamadan uzaklaştırmayı amaçlayan minimalist ve temiz bir arayüze sahip `BlockedActivity`.
* **Akıllı Geri Tuşu Yönetimi:** Engelleme ekranındayken geri tuşuna basıldığında, uygulamanın beklenmedik şekilde kapanması yerine, sadece bu engelleme katmanının kapatılması sağlanır.
* **Modern Android Altyapısı:** Proje, güncel ve kararlı Android kütüphaneleri kullanılarak inşa edilmiştir:
    * **AndroidX** (AppCompat, Core KTX)
    * **Material Design Components**
    * **ViewBinding** (Güvenli ve temiz görünüm erişimi için)
    * **Navigation Component** (Gelecekteki ekranlar arası geçişler için temel altyapı)

---

## 🛠️ Teknik Detaylar ve Kurulum

Projeyi hızlıca yerel makinenizde çalışır duruma getirmek için aşağıdaki adımları izleyin.

### Kullanılan Ana Teknolojiler

| Kategori | Teknoloji / Kütüphane | Notlar |
| :--- | :--- | :--- |
| **Dil** | **Kotlin** | Modern, güvenli ve üretken bir dil. |
| **Bağımlılıklar** | `androidx.appcompat:appcompat:1.7.1` | |
| | `com.google.android.material:material:1.13.0` | |
| | `androidx.constraintlayout:constraintlayout:2.2.1` | |
| | `androidx.navigation:navigation-fragment-ktx:2.9.5` | |
| **Asenkron Programlama** | *(Gelecek Planı: Kotlin Coroutines)* | |
| **Mimari** | *(Gelecek Planı: MVVM)* | |

### Başlatma Adımları

1.  Bu depoyu terminaliniz üzerinden klonlayın:
    ```bash
    git clone [https://github.com/mango/MindClearDemo.git](https://github.com/mango/MindClearDemo.git)
    ```
2.  Projeyi **Android Studio**'da açın.
3.  Gradle senkronizasyonunun (gerekli bağımlılıkların indirilmesi) otomatik olarak tamamlanmasını bekleyin.
4.  Projeyi bir **Android Emülatörü**nde veya **Fiziksel Cihaz**da çalıştırın.

---

## 🔮 Gelecek Planları

Bu demo projesinin tam teşekküllü bir dijital refah uygulamasına dönüşmesi için atılacak potansiyel adımlar şunlardır:

* [ ] **Ayarlar Menüsü:** Kullanıcının hangi uygulamaların engelleneceğini özelleştirebileceği kapsamlı bir ekran.
* [ ] **Zamanlama Özelliği:** Engelleme özelliğini belirli saatler arasında (örneğin çalışma saatleri) otomatik olarak aktif/pasif yapabilme yeteneği.
* [ ] **Raporlama ve İstatistikler:** Engelleme istatistiklerini (kaç kez engellendi, ne kadar zaman kazanıldı vb.) gösteren görsel bir raporlama ekranı.
* [ ] **Mimari İyileştirme:** Projenin sürdürülebilirliğini ve test edilebilirliğini artırmak için **MVVM (Model-View-ViewModel)** mimarisinin tam entegrasyonu.
