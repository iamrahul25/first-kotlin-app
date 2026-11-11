# TimeWise

Android focus and screen time app. It shows where your time goes, using Android's
usage stats, then nudges you to spend it better.

## Features

- Total screen time today, compared with yesterday
- Per-app screen time, ranked, with app icons
- Phone unlock count (Android 9+)
- Weekly bar chart (tap a day to see its apps) and week-over-week change
- Hourly timeline of when the phone was used
- Bedtime schedule with a reminder notification
- Automatic sleep log from when the phone was put down and picked up (Android 9+)
- 1-hour focus challenge with streaks and badges
- Screen time badges: 15 achievements (Zen Master, Digital Monk, ...) and 11 warnings in a
  Hall of Shame (Screen Addict, Doom Spiral, ...), each with a lifetime count of times earned
- Profile level from the last 7 days' average: Monk (under 2h), Mindful (2–4h), Hooked (4–6h),
  Addict (6–8h) or Zombie (8h+), with what it takes to level up and what it costs a year
- Month view (last 30 days) and a monthly trend of average daily screen time
- Weekly and monthly reports, shareable as an image

Everything is computed on the device. No data leaves the phone unless you share a report.

Android keeps detailed usage events for only about 10 days, so TimeWise saves a daily
summary (total, unlocks, top 10 apps, sleep) every night just after midnight in the
background, and whenever the app is opened. The month view, trend and reports fill in from
that saved history over time. Settings shows when the last nightly save ran.

To trigger the nightly save by hand on a debug build:

```bash
adb shell am broadcast -n com.nightwise.timewise/.snapshot.DailySnapshotReceiver
```

## Permissions

| Permission | Why |
| --- | --- |
| Usage Access (required) | Screen time, per-app time, unlocks, sleep log |
| Notifications (recommended) | Bedtime reminder, focus challenge results |

Usage Access is granted from `Settings > Apps > Special app access > Usage access > TimeWise`.
The app links straight there from onboarding.

## Build

```powershell
.\gradlew.bat assembleDebug          # app\build\outputs\apk\debug\app-debug.apk
.\gradlew.bat testDebugUnitTest      # unit tests
python app.py                        # interactive menu: run, build, sign, release
```

For signing and Play Store builds, see [generate-apk.md](generate-apk.md) and [how_to_run.md](how_to_run.md).
