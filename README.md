**Vaultnote 📝🔒**

Vaultnote is a secure, offline-first note-taking Android application built with modern Android development best practices. It prioritizes user privacy by design, featuring robust local persistence, biometric security, custom theme controls, and end-to-end encryption (E2EE) where data is encrypted directly on the device before any future cloud synchronization.

✨ Features
Modern Android Tech Stack: Built entirely with Jetpack Compose, ViewModel, Hilt for dependency injection, and Room for local persistence.

Offline-First Architecture: Local storage serves as the single source of truth. The app is fully functional without an internet connection.

Biometric & PIN Security: Secure your notes with device biometrics (Fingerprint/Face unlock) or a secure fallback PIN using the BiometricPrompt API.

Appearance Settings: Toggle seamlessly between Light Mode, Dark Mode, or System Default directly from the settings screen.

End-to-End Encryption (E2EE): Notes are encrypted locally on the user's device. Any future sync service only receives encrypted ciphertexts, ensuring complete zero-knowledge privacy.

Advanced Key Management & Recovery:

Device-specific cryptographic keys.

Encrypted key sharing mechanisms between trusted devices.

Explicit recovery mechanisms to handle lost access to encryption keys.

Scalable Sync Architecture: Designed from day one with a clean separation of concerns, paving the way for seamless cloud synchronization once local CRUD and security features are fully stable.

🛠️ Tech Stack & Architecture
UI: Jetpack Compose, Material 3

Architecture: MVVM (Model-View-ViewModel) with Clean Architecture principles

Dependency Injection: Hilt

Local Database: Room (Offline-first data source)

Security & Cryptography: Android Keystore System, Jetpack Security / Tink, and androidx.biometric

Concurrency: Kotlin Coroutines & Flow

📂 Project Structure
com.vaultnote.app
│
├── data                    # Local database, entities, and encryption handlers
│   ├── local               # Room database, DAOs, and entities
│   └── security            # Cryptographic operations, key management, and E2EE logic
│
├── di                      # Hilt dependency injection modules
│
├── ui                      # Presentation layer (Jetpack Compose)
│   ├── components          # Reusable UI elements
│   ├── screens             # Feature screens (Note List, Editor, Settings, Auth, etc.)
│   └── theme               # Material 3 dynamic color schemes, typography, and dark/light palettes
│
└── viewmodel               # State management and business logic (ViewModels)

🚀 Getting Started
Prerequisites
Android Studio (Hedgehog or newer recommended)

JDK 17 or higher

Android SDK API Level 24 or higher (Min SDK: 24, Target SDK: 34+)

Installation & Setup
Clone the repository: 

git clone https://github.com/lankapati0/vaultnote.git

Open the project in Android Studio.

Sync Gradle and let all dependencies download.

Run the app on an emulator or a physical Android device.

🗺️ Roadmap
[x] Local CRUD operations with Room and Jetpack Compose

[x] Local encryption pipeline and Key management setup

[x] Biometric & PIN authentication lock screen

[x] Dark and Light mode theme toggling in Settings

[ ] Device-to-device encrypted key sharing

[ ] Recovery phrase / key backup mechanism

[ ] Cloud sync backend integration

📄 License
This project is licensed under the MIT License - see the LICENSE file for details.
