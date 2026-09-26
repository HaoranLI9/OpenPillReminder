# Changelog

All notable changes to Anshi are documented here.

## [0.1.2] - 2026-09-26

### Fixed

- Refresh the calendar's current date automatically after midnight without requiring interaction.
- Refresh the current date immediately when returning to the app.
- Move to the new current-day page only when the user was still viewing yesterday's page.

### Testing

- Added DST-aware unit tests for next-midnight scheduling.
- Added an Android 15 instrumented regression test for background-to-foreground date rollover.

## [0.1.1] - 2026-09-16

### Fixed

- Keep reminders deliverable while the app is in the background by using an
  allow-while-idle fallback when exact alarm access is unavailable.
- Restore pill, strong, and refill reminders after a device restart or app update.
- Reschedule saved reminders after exact alarm access is granted.
- Create notification channels at application startup so background receivers can
  post notifications before the main screen has been opened.
- Apply the same reliable scheduling path to refill reminders.

### Testing

- Added an Android instrumented regression test for restoring strong reminders.
- Passed JVM unit tests, Android lint, debug APK compilation, and Android test APK
  compilation.

[0.1.2]: https://github.com/HaoranLI9/OpenPillReminder/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/HaoranLI9/OpenPillReminder/compare/v0.1.0...v0.1.1
