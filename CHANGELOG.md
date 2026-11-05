# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The release workflow uses the section whose version matches the pushed tag as the release notes.
Before you push a tag, move the entries under "Unreleased" into a new section for that version.

## [Unreleased]

## [1.1.0] - 2026-11-05

### Changed

- Buttons that change the level length by five minutes replace the preset lengths.
- Reset level asks for confirmation first, like New game.

## [1.0.0] - 2026-11-05

### Added

- A poker clock that counts down each blind level and starts the next level by itself.
- Level length from 1 to 180 minutes, set with `−` and `+`, presets, or by typing it.
- Buttons to move between levels, reset the current level and start a new game.
- The clock keeps running in the background, with the screen off, and after the app is closed.
- A notification that shows the level and a large countdown of the time left while the clock runs.
- A level-end alarm that rings 5 times, with a Stop alarm button on the clock and in its notification.
- A setting that starts the next level only after the alarm stops by itself or is stopped.
- Settings for the alarm sound (beeps, a sound from the device, or none), the number of rings,
  and vibration.
- A light, dark or device-matching theme.
- The level length, Reset level and New game can change only while the clock is paused.

[Unreleased]: https://github.com/RangelReale/tlpt-clock/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/RangelReale/tlpt-clock/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/RangelReale/tlpt-clock/releases/tag/v1.0.0
