# TLPT Clock

A native Android poker clock for TLPT (The Little Poker Table). This first version only manages the clock. It has no blind amounts, players or chips.

## Using the clock

- **Change the level length:** pause the clock first. Then tap `−` or `+` to change it by one minute, or tap a preset. To type any length from 1 to 180 minutes, tap the time or the length button.
- **Change it during a level:** the time already played stays the same, and the new length also applies to later levels. A change never ends a level by itself.
- **While the clock runs or the alarm rings,** the length controls, **Reset level** and **New game** are turned off, so a stray tap cannot change the game.
- **When the countdown reaches zero:** the alarm rings 5 times and the app shows "Blinds up". Tap **Stop alarm**, on the clock or in the alarm notification, to silence it. The next level starts from its full length when the alarm stops by itself or when you stop it. In the settings you can instead let the next level start at once, while the alarm rings.
- **Move between levels by hand:** `‹` and `›` change the level and restart the countdown.
- **Reset level** restarts the countdown of the current level. It works only while paused.
- **New game** returns to a paused level 1 and keeps the level length. It works only while paused.

The screen stays on while the clock runs. The alarm uses the alarm volume, which the volume keys control while the app is on screen.

## In the background

The clock keeps running when you leave the app, turn the screen off or close the app. While it runs:

- A notification shows the level and counts down the time left. Tap it to return to the clock. The app asks for permission to show notifications the first time you start the clock.
- A system alarm wakes the device when the level ends, so the alarm plays on time. Android shows its alarm icon in the status bar while this alarm is set.

Pausing the clock removes the notification and the alarm. After a device restart the clock loads as paused.

## Settings

Tap the gear button to open the settings.

- **Theme:** the same as the device, light, or dark.
- **Alarm when a level ends:** beeps, a sound from the device, or no sound. If a device sound cannot play, the app beeps instead.
- **Number of rings:** 1 to 10. Each ring of a device sound lasts up to 3 seconds.
- **Vibrate:** vibrates with each ring.
- **Start the next level after the alarm:** on by default. When it is off, the next level starts as soon as the level ends.
- **Test alarm** plays the alarm with the current settings.

## Building

The build needs JDK 17 or newer and an Android SDK found through `ANDROID_HOME`.

```sh
./gradlew :app:assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest    # also writes layout pictures to app/build/screenshots
```

The tests run without a device. They do not cover the length dialog, because its text field never goes idle under Robolectric (the library that runs Android code on a computer). Check the dialog on a device or emulator after changing it.

## Continuous integration and releases

GitHub Actions runs two workflows:

- **CI** runs the unit tests and builds the debug APK on every push and pull request. The APK and the layout pictures are saved with the run.
- **Release** runs when you push a version tag such as `v0.2`. It builds the APK and attaches it to a GitHub release for that tag. The app's version name is the tag without the `v`, and its version code is the workflow run number.

The release APK is signed with the key stored in these repository secrets:

| Secret | Value |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | The keystore file, encoded in base64 |
| `RELEASE_KEYSTORE_PASSWORD` | The keystore password |
| `RELEASE_KEY_ALIAS` | The key alias |
| `RELEASE_KEY_PASSWORD` | The key password |

Without these secrets, the release gets the debug APK instead.

To build a signed release APK locally, set the same values as environment variables, with `RELEASE_KEYSTORE_FILE` holding the path to the keystore instead of its base64 text. Then run `./gradlew :app:assembleRelease`.
