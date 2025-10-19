# MindClearDemo

---

## ✨ Features

* **App Block Screen:** A minimalist and clean BlockedActivity interface designed to inform the user and help them step away from distracting apps.
* **Smart Back Button:** When the back button is pressed while on the blocking screen, only the blocking layer is dismissed, preventing the entire app from closing unexpectedly.
* **Modern Android Backend:** Built using up-to-date and stable Android libraries:
    * **AndroidX** (AppCompat, Core KTX)
    * **Material Design Components**
    * **ViewBinding** (for safe and clean view access)
    * **Navigation Component** (foundation for future screen navigation)

---

## Technical Details & Setup

Follow the steps below to run the project on your local machine.

### Backend

| Category | Technology/Library / Kütüphane | Note |
| :--- | :--- | :--- |
| **Language** | **Kotlin** | Modern, safe and flexible language. |
| **Dependencies** | `androidx.appcompat:appcompat:1.7.1` | |
| | `com.google.android.material:material:1.13.0` | |
| | `androidx.constraintlayout:constraintlayout:2.2.1` | |
| | `androidx.navigation:navigation-fragment-ktx:2.9.5` | |
| **Asynchronous Programming** | *(Planned: Kotlin Coroutines)* | |
| **Architecture** | *(Planned: MVVM)* | |

### Getting Started

1.  Clone this repository via terminal::
    ```bash
    git clone [https://github.com/mango/MindClearDemo.git](https://github.com/mango/MindClearDemo.git)
    ```
2.  Open the project in **Android Studio**.
3.  Wait for Gradle synchronization (it will automatically download required dependencies).
4.  Run the project on an **Android Emulator** or a **physical device**.

---

## 🔮 Future Plans

Potential steps to upgrade this demo project into a fully featured app:

* [ ] **Block Menu:** A detailed screen where users can customize which apps to block.
* [ ] **Scheduling Feature:** EAutomatically enable or disable blocking during specific hours (e.g., work hours).
* [ ] **Reports & Statistics:** A visual reporting screen showing how many times blocking was triggered, time saved, etc.
* [ ] **Architectural Improvements:** Full integration of **MVVM (Model-View-ViewModel)** architecture to enhance sustainability and testability.
