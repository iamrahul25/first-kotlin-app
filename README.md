# Reel Blocker

Android app that blocks YouTube Shorts and Instagram Reels using an Accessibility Service.

## Generate an APK

### Signed debug APK for testing

From the project root, run:

```powershell
.\gradlew.bat assembleDebug
```

The installable APK is generated at:

```text
app\build\outputs\apk\debug\app-debug.apk
```

### Release APK

Build the release variant with:

```powershell
.\gradlew.bat assembleRelease
```

This project currently produces:

```text
app\build\outputs\apk\release\app-release-unsigned.apk
```

`app-release-unsigned.apk` cannot be installed because Android requires every APK to have a valid digital signature. Android may report that the package is invalid.

To create an installable release APK, use Android Studio:

```text
Build > Generate Signed Bundle / APK > APK
```

Select the `release` variant and choose or create a signing keystore. The signed APK will normally be generated at:

```text
app\build\outputs\apk\release\app-release.apk
```

Keep the keystore and signing credentials secure. Do not commit them to Git.

## Install and enable the service

After installing the APK, open:

```text
Settings > Accessibility > Installed apps > Reel Blocker
```

Enable the Accessibility Service, then return to the app.

For more details, see [generate-apk.md](generate-apk.md) and [how_to_run.md](how_to_run.md).
