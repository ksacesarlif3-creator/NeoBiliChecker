# Build the APK

## GitHub Actions (no Android Studio required)
1. Push this project to a GitHub repository.
2. Open **Actions → Build Android APK → Run workflow**.
3. After the workflow completes, open the run and download the artifact **NeoBiliChecker-debug-apk**.
4. The artifact contains `app-debug.apk`, which can be installed on an Android device after allowing installation from the browser/file manager used to open it.

The workflow installs Android API 37, Build Tools 36.0.0, JDK 17, and Gradle 9.6.0, then runs unit tests and builds the debug APK.

## Android Studio
Open the project and build **Build → Build App Bundle(s) / APK(s) → Build APK(s)**. The output is normally at:

`app/build/outputs/apk/debug/app-debug.apk`
