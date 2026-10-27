# Setup: Android Studio and your phone (Windows, USB)

> **Verified 2026-10-27** on the personal laptop with a Samsung Galaxy A34 (SM-A346E, Android 16): Android Studio sync, build and ▶ Run over USB all worked. Builds can also still run in the cloud on GitHub Actions (D-013); see "No-cable install" at the bottom.

Everything here is free. Expect about 10 GB of disk space (keep **at least 15 GB free** before you start) and 30–60 minutes, mostly download time.

## 1. Install Android Studio
1. Go to <https://developer.android.com/studio> and download the latest **stable** Android Studio for Windows.
2. Run the downloaded `.exe` (from your Downloads folder) and accept the defaults (include **Android Virtual Device**).
3. On first launch choose **Standard** setup. It downloads the Android SDK, build tools, an emulator image, and a bundled Java runtime (you don't need to install Java separately).
4. When it finishes, you'll see the "Welcome to Android Studio" window.

**Check:** the SDK should now exist at `C:\Users\<you>\AppData\Local\Android\Sdk`.

> **Java 17 required for Gradle 8.9.** Android Studio bundles Java 25, which Gradle 8.9 cannot run on (the sync fails with just `25.0.3`). Install Temurin 17 (`winget install EclipseAdoptium.Temurin.17.JDK`), then in Android Studio: **Ctrl+Alt+S → search `Gradle JDK` → pick the Temurin 17 entry**, and sync.

> **Disk space:** if the disk fills during setup or sync, the Gradle sync stops with an error. Free some space, then use **File → Sync Project with Gradle Files** (or the elephant icon with the blue arrow) to resume. If it still fails, ask Claude to clear the Gradle cache (`%USERPROFILE%\.gradle`); it only re-downloads files.

## 2. Prepare your Android phone
1. Open **Settings → About phone** and tap **Build number** 7 times (Samsung: Settings → About phone → Software information → Build number). Enter your PIN if asked. You'll see "You are now a developer".
2. Go to **Settings → Developer options** and turn on **USB debugging**.
3. Connect the phone to the PC with a USB **data** cable (some cheap cables only charge).
4. On the phone, set the USB mode to **File transfer** if asked. Accept the "Allow USB debugging?" prompt and tick "Always allow from this computer".
5. **Samsung phones:** if the phone isn't detected, install the Samsung USB driver from Samsung's developer site. (Not needed on the A34 test: Windows handled it.)

## 3. Confirm the PC can see the phone
Open PowerShell and run:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
```

**Expected result:**
```
List of devices attached
RZCW60NZ16T    device
```

| If you see… | Fix |
|---|---|
| Empty list | Try another cable/USB port; make sure USB debugging is on; set USB mode to "File transfer" |
| `unauthorized` | Unlock the phone and tap **Allow** on the debugging prompt. If no prompt: Developer options → **Revoke USB debugging authorizations**, then unplug and replug |
| `adb.exe` not found | Android Studio setup didn't finish: open it and complete the Standard setup |

## 4. Open the project and run the app
1. In Android Studio's welcome page click **Open** and choose the repo's **`android`** folder (not the repo root). Choose **Trust Project** if asked.
2. Wait for the Gradle sync to finish (progress bar at the bottom right; the first sync takes several minutes).
3. In the top toolbar, check the device dropdown shows your phone, then click the green ▶ **Run** button.
4. The first build takes a few minutes. The app then installs and opens on the phone.

Later runs are much faster, and ▶ Run installs over the previous version.

## 5. Tell Claude
Once the app runs, tell Claude. With the phone connected, Claude can run `adb devices` and check the connection for you.

---

## No-cable install (from a GitHub Actions build)
1. On your phone's browser, sign in to GitHub and open the repo → **Actions** → the latest green ✅ "Android build" run.
2. Under **Artifacts**, download `identity-debug-apk` (a `.zip`). Open it with the Files app and tap the `.apk` inside.
3. Android will ask to allow installs from that app (browser/Files): allow it **only for this install**, then turn it back off afterwards.
4. Play Protect may warn that the app is unrecognised. That's expected for a debug build you made yourself.

## Optional: Wireless debugging (Android 11+, no cable)
Android Studio → **Device Manager → Pair devices using Wi-Fi**, then on the phone: Developer options → **Wireless debugging → Pair device with QR code**. Phone and laptop must be on the same Wi-Fi network. It is less reliable than USB (it can drop when the phone sleeps or the network changes), so use it only when you don't want a cable.
