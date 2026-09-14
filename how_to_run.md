# How to Run the App

## 1) Start an Android emulator

Open Android Studio and launch an emulator, or start one from the command line:

```bash
adb devices
```

If no device is listed, start an emulator from Android Studio first.

## 2) Build and install the app

From the project root:

```bash
cd "c:/all-projects/first-kotlin-app"
./gradlew installDebug
```

## 3) Launch the app manually

```bash
adb shell am start -n com.you.reelblocker/com.you.reelblocker.MainActivity
```

## 4) Optional: build + install + launch in one command

```bash
cd "c:/all-projects/first-kotlin-app"
./gradlew installDebug && adb shell am start -n com.you.reelblocker/com.you.reelblocker.MainActivity
```

## 5) If you want to inspect the app logs

```bash
adb logcat
```

Useful filters while testing the accessibility service:

```bash
adb logcat | grep -i "reel\|shorts\|accessibility"
```

## 6) Accessibility permission for the app

After installing the app on the emulator, go to:

Settings > Accessibility > Installed apps > Reel Blocker

Turn on the service, then return to the app and test the detection flow.
