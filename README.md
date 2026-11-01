# TLPT Clock

A native Android poker clock for TLPT (The Little Poker Table). This first version only manages the clock. It has no blind amounts, players or chips.

## Using the clock

- **Change the level length:** tap `−` or `+` to change it by one minute, or tap a preset. To type any length from 1 to 180 minutes, tap the time or the length button.
- **Change it during a level:** the time already played stays the same, and the new length also applies to later levels. A change never ends a level by itself.
- **When the countdown reaches zero:** the app beeps three times and shows "Blinds up" for 10 seconds. The next level then runs from its full length without a tap.
- **Move between levels by hand:** `‹` and `›` change the level and restart the countdown.
- **Reset level** restarts the countdown of the current level.
- **New game** returns to a paused level 1 and keeps the level length.

The screen stays on while the clock runs. The beep uses the alarm volume, which the volume keys control while the app is on screen.

## Known limitation

The clock keeps correct time when the app is in the background, but it only beeps while the app is on screen. If a level ends in the background, the beep plays when you return.

## Building

The build needs JDK 17 or newer and an Android SDK found through `ANDROID_HOME`.

```sh
./gradlew :app:assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest    # also writes layout pictures to app/build/screenshots
```

The tests run without a device. They do not cover the length dialog, because its text field never goes idle under Robolectric (the library that runs Android code on a computer). Check the dialog on a device or emulator after changing it.
