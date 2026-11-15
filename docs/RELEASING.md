# Releasing Pustaka

Pustaka is distributed as an APK on GitHub Releases, not through the Play Store (decision D1). The project owner signs and publishes each release (decisions D21 and D22). This guide covers the one-time setup, then the steps for every release.

## One-time setup: the signing key

Every release must be signed with the same key, or phones that have Pustaka installed can't update it. **Keep the keystore and its passwords safe, and back them up somewhere other than this computer.** If the key is lost, users would have to uninstall Pustaka (losing their progress and highlights) to install a new version.

1. Create the keystore with `keytool`, which comes with Android Studio. Keep it **outside** this repository, for example in a `keys` folder in your home folder:

   ```bash
   mkdir -p ~/keys
   "/c/Program Files/Android/Android Studio/jbr/bin/keytool" -genkeypair -v -keystore ~/keys/pustaka-release.jks -alias pustaka -keyalg RSA -keysize 4096 -validity 36500
   ```

   `keytool` asks for a keystore password, your name and organisation (these are stored in the certificate, which anyone can read from the APK), and a key password. Using the same password for both is fine.

2. Create `keystore.properties` in the project root (next to `settings.gradle.kts`):

   ```properties
   storeFile=C:/Users/<you>/keys/pustaka-release.jks
   storePassword=<keystore password>
   keyAlias=pustaka
   keyPassword=<key password>
   ```

   Use forward slashes in the path. Both `keystore.properties` and `*.jks` files are in `.gitignore`, so they can't be committed by accident. Without this file, release builds are still made, but they're unsigned and can't be installed.

## Every release

1. **Check the version** in `app/build.gradle.kts`. `versionName` is what users see, such as `1.0.0`. `versionCode` must go up by at least 1 for every release, or Android won't install it as an update.

2. **Run the checks:**

   ```bash
   ./gradlew testDebugUnitTest lintDebug
   ```

   With a phone or emulator connected, also run the UI tests:

   ```bash
   ./gradlew connectedDebugAndroidTest
   ```

   Gradle uninstalls the app after `connectedDebugAndroidTest`, which deletes its data (progress, highlights, edits). Run it on a test device, not on the phone you read on.

3. **Build the signed APK:**

   ```bash
   ./gradlew assembleRelease
   ```

   The APK is `app/build/outputs/apk/release/app-release.apk`. The build shrinks the code with R8, so it's much smaller than a debug build. R8's mapping file, which turns crash stack traces back into readable names, is `app/build/outputs/mapping/release/mapping.txt`. Keep it with the release.

4. **Check the signature** (optional):

   ```bash
   "$ANDROID_HOME/build-tools/36.1.0/apksigner" verify --print-certs app/build/outputs/apk/release/app-release.apk
   ```

5. **Try it on a phone:** install it, open a book, turn some pages, highlight some text, and search the library.

6. **Publish on GitHub:**
   - Create a tag such as `v1.0.0` on the commit that was built, and push it.
   - Create a GitHub Release from the tag. Use the release notes in `docs/releases/` as its description.
   - Upload the APK, renamed to `Pustaka-1.0.0.apk`, and attach `mapping.txt` as `Pustaka-1.0.0-mapping.txt`.
