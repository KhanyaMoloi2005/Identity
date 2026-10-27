# Setup: Android Studio and your phone (Windows)

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

Claude will then create the `android/` project skeleton and walk you through building it and running it on your phone.
