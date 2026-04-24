# Kathakar Android — MVP Build

Multi-language, multi-category story platform · Kotlin + Jetpack Compose + Firebase

---

## Drop these files into your repo

```
Kathakar/
├── .github/
│   └── workflows/
│       └── android_ci.yml          ← GitHub Actions CI (build + lint + APK upload)
│
├── gradle/
│   └── libs.versions.toml          ← Version catalog for all dependencies
│
├── app/
│   ├── build.gradle.kts            ← App-level Gradle (all dependencies declared here)
│   ├── proguard-rules.pro          ← ProGuard keep rules
│   │
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/
│       │   └── values/
│       │       ├── strings.xml     ← ⚠ Replace default_web_client_id before building
│       │       └── themes.xml
│       │
│       └── java/com/kathakar/app/
│           ├── KathakarApp.kt          ← @HiltAndroidApp
│           ├── MainActivity.kt
│           │
│           ├── ui/
│           │   ├── theme/
│           │   │   └── KathakarTheme.kt
│           │   └── screens/
│           │       └── Screens.kt      ← ALL screens in one file
│           │
│           ├── viewmodel/
│           │   ├── ViewModels.kt       ← Auth, Home, Story, Writer, Library, Profile VMs
│           │   └── ReaderViewModel.kt
│           │
│           ├── repository/
│           │   ├── AuthRepository.kt   ← Google + Email auth, free coin grant
│           │   ├── StoryRepository.kt  ← Stories, episodes, author CRUD
│           │   ├── CoinRepository.kt   ← Firestore transaction unlock
│           │   └── LibraryRepository.kt
│           │
│           ├── domain/
│           │   └── model/
│           │       └── Models.kt       ← All data classes + MvpConfig
│           │
│           ├── navigation/
│           │   └── KathakarNavGraph.kt ← All routes including stubs
│           │
│           ├── di/
│           │   └── FirebaseModule.kt   ← Hilt providers
│           │
│           └── util/
│               └── Utils.kt            ← FirestoreCollections, Resource<T>
│
├── build.gradle.kts                ← Root build file
├── settings.gradle.kts
└── .gitignore                      ← google-services.json excluded
```

---

## Before your first build — 3 required steps

### 1. Add google-services.json
- Go to Firebase Console → your **kathakar** project → Project Settings
- Add Android app → package name `com.kathakar.app`
- Download `google-services.json` → place at `app/google-services.json`
- **Do NOT commit this file** (already in .gitignore)

### 2. Set your Web Client ID
- Firebase Console → Authentication → Sign-in method → Google → Web client ID
- Open `app/src/main/res/values/strings.xml`
- Replace `YOUR_WEB_CLIENT_ID_HERE` with your actual client ID

### 3. Add GitHub Secret for CI
- GitHub repo → Settings → Secrets → Actions → New repository secret
- Name: `GOOGLE_SERVICES_JSON`
- Value: paste the full contents of your `google-services.json`
- This lets GitHub Actions build without committing the file

---

## MVP behaviour

| Feature | Status |
|---------|--------|
| Google Sign-In | ✅ Live |
| Email / Password | ✅ Live |
| 100 free coins on signup | ✅ Live |
| Story list, search, filters | ✅ Live |
| Episode reading (ch.1 free) | ✅ Live |
| Episode unlock with coins | ✅ Live (Firestore transaction) |
| Author coin earnings (60%) | ✅ Live |
| Write stories (manual) | ✅ Live |
| Publish chapters | ✅ Live |
| Library / bookmarks | ✅ Live |
| Coin history | ✅ Live |
| "Buy Coins" button | 🚧 Stub screen |
| "Subscribe" button | 🚧 Stub screen |
| "AI Assist" tab | 🚧 Stub screen |
| Audio | 🔇 Hidden |
| Razorpay payments | 🔇 Disabled |

### Enabling features later
Everything is controlled by `MvpConfig` in `domain/model/Models.kt`:
```kotlin
object MvpConfig {
    const val PAYMENTS_ENABLED       = false   // flip → true, wire BuyCoinsScreen
    const val AI_WRITING_ENABLED     = false   // flip → true, wire AI screens
    const val SUBSCRIPTIONS_ENABLED  = false   // flip → true, wire SubscribeScreen
}
```

---

## Firestore security rules
Paste `firestore.rules` content in Firebase Console → Firestore → Rules → Publish.

## Firestore indexes
First run the app and check Logcat for index creation links.
Each missing index shows a direct URL — tap to create automatically.
