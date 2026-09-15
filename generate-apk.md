# Generate APK File

## Build an unsigned release APK

Open PowerShell in the project root:

```powershell
cd C:\all-projects\first-kotlin-app
.\gradlew.bat assembleRelease
```

The APK will be generated at:

```text
app\build\outputs\apk\release\app-release-unsigned.apk
```

An unsigned APK is useful for build verification and testing. It cannot be distributed as a normal release APK.

## Generate a signing keystore

Create a keystore once and keep it secure:

```powershell
keytool -genkeypair -v `
  -keystore reelblocker-release.jks `
  -alias reelblocker `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000
```

Do not commit the keystore, passwords, or signing credentials to Git.

## Build a signed release APK

In Android Studio, choose:

```text
Build > Generate Signed Bundle / APK
```

Then:

1. Select **APK**.
2. Select or create the keystore.
3. Choose the `release` build variant.
4. Complete the signing wizard.

The signed APK is normally written to:

```text
app\build\outputs\apk\release\app-release.apk
```

## Android App Bundle for Google Play

For Google Play publishing, choose **Android App Bundle** instead of APK in the signing wizard. The output file uses the `.aab` extension.

## Troubleshooting

If PowerShell reports that Gradle cannot run, use the project wrapper exactly as shown:

```powershell
.\gradlew.bat assembleRelease
```

Make sure Java and the Android SDK are installed and configured in Android Studio.
