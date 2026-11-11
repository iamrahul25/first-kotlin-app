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
adb shell am start -n com.nightwise.timewise/com.nightwise.timewise.MainActivity
```

## 4) Optional: build + install + launch in one command

```bash
cd "c:/all-projects/first-kotlin-app"
./gradlew installDebug && adb shell am start -n com.nightwise.timewise/com.nightwise.timewise.MainActivity
```

## 5) If you want to inspect the app logs

```bash
adb logcat
```

## 6) Usage Access permission for the app

The first launch walks through onboarding. Tap **Allow** next to Usage Access, turn on
"Permit usage access" for TimeWise, then return to the app. Screen time, apps, unlocks
and the sleep log all depend on it.

To grant it from the command line on an emulator:

```bash
adb shell appops set com.nightwise.timewise GET_USAGE_STATS allow
```
