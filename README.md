# TLPT Clock

A native Android poker clock for TLPT (The Little Poker Table). This first version only manages the clock. It has no blind amounts, players or chips.

## Using the clock

- **Change the level length:** tap `−` or `+` to change it by one minute, or tap a preset. To type any length from 1 to 180 minutes, tap the time or the length button.
- **Change it during a level:** the time already played stays the same, and the new length also applies to later levels. A change never ends a level by itself.
- **When the countdown reaches zero:** the app plays the alarm and shows "Blinds up" for 10 seconds. The next level then runs from its full length without a tap.
- **Move between levels by hand:** `‹` and `›` change the level and restart the countdown.
- **Reset level** restarts the countdown of the current level.
- **New game** returns to a paused level 1 and keeps the level length.

The screen stays on while the clock runs. The alarm uses the alarm volume, which the volume keys control while the app is on screen.

## In the background

The clock keeps running when you leave the app, turn the screen off or close the app. While it runs:

- A notification shows the level and counts down the time left. Tap it to return to the clock. The app asks for permission to show notifications the first time you start the clock.
- A system alarm wakes the device when the level ends, so the alarm plays on time. Android shows its alarm icon in the status bar while this alarm is set.

Pausing the clock removes the notification and the alarm. After a device restart the clock loads as paused.

## Settings

Tap the gear button to open the settings.

- **Theme:** the same as the device, light, or dark.
- **Alarm when a level ends:** 1 to 10 beeps, a sound from the device, or no sound. A device sound plays for at most 8 seconds. If it cannot play, the app beeps instead.
- **Vibrate:** vibrates three times with the alarm.
- **Test alarm** plays the alarm with the current settings.

## Building

The build needs JDK 17 or newer and an Android SDK found through `ANDROID_HOME`.

```sh
./gradlew :app:assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest    # also writes layout pictures to app/build/screenshots
```

The tests run without a device. They do not cover the length dialog, because its text field never goes idle under Robolectric (the library that runs Android code on a computer). Check the dialog on a device or emulator after changing it.
