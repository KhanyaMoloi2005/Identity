# Setup: Android Studio and your phone (Windows)

> **Current plan (D-013):** builds run in the cloud on GitHub Actions, so **you don't need this guide yet**. Use it later on your **personal** laptop. To run the app on your phone without a cable or Android Studio, see "No-cable install" at the bottom.

Everything here is free. Expect about 10 GB of disk space and 30–60 minutes, mostly download time.

## 1. Install Android Studio
1. Go to <https://developer.android.com/studio> and download the latest **stable** Android Studio for Windows.
2. Run the installer and accept the defaults (include **Android Virtual Device**).
3. On first launch choose **Standard** setup. It downloads the Android SDK, build tools, an emulator image, and a bundled Java runtime (you don't need to install Java separately).
4. When it finishes, you'll see the "Welcome to Android Studio" window. Leave it there for now.

**Check:** the SDK should now exist at `C:\Users\<you>\AppData\Local\Android\Sdk`.

## 2. Prepare your Android phone
1. Open **Settings → About phone** and tap **Build number** 7 times (on some phones: Settings → About phone → Software information → Build number). You'll see "You are now a developer".
2. Go to **Settings → System → Developer options** (location varies by brand) and turn on **USB debugging**.
3. Connect the phone to the PC with a USB **data** cable (some cheap cables only charge).
4. On the phone, accept the "Allow USB debugging?" prompt and tick "Always allow from this computer".
5. **Samsung phones:** if the phone isn't detected, install the Samsung USB driver from Samsung's developer site.

## 3. Confirm the PC can see the phone
Open PowerShell and run:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
```

**Expected result:**
```
List of devices attached
R58M12ABCDE    device
```

| If you see… | Fix |
|---|---|
| Empty list | Try another cable/USB port; make sure USB debugging is on; on the phone set USB mode to "File transfer" |
| `unauthorized` | Unlock the phone and accept the debugging prompt |
| `adb.exe not recognised` / path not found | Android Studio setup didn't finish: open it and complete the Standard setup |

## 4. Tell Claude
Once `adb devices` shows `device`, tell Claude:
- your phone model and Android version (Settings → About phone)
- how much RAM your PC has (Settings → System → About)

Claude will then walk you through opening the `android/` folder in Android Studio and running the app on your phone.

---

## No-cable install (from a GitHub Actions build)
1. On your phone's browser, sign in to GitHub and open the repo → **Actions** → the latest green ✅ "Android build" run.
2. Under **Artifacts**, download `identity-debug-apk` (a `.zip`). Open it with the Files app and tap the `.apk` inside.
3. Android will ask to allow installs from that app (browser/Files): allow it **only for this install**, then turn it back off afterwards.
4. Play Protect may warn that the app is unrecognised. That's expected for a debug build you made yourself.

## Wireless debugging (personal laptop, Android 11+, no cable)
Android Studio → **Device Manager → Pair devices using Wi-Fi**, then on the phone: Developer options → **Wireless debugging → Pair device with QR code**. Phone and laptop must be on the same Wi-Fi network.
