<div align="center">
  <br>
  <img alt="Anshi app icon" src="./resources/icons/icon-scalable.svg" width="120" />
  <h1>Anshi / 安时</h1>

  ![License](https://img.shields.io/badge/license-GPL--3.0--only-blue)
  ![Minimum SDK](https://img.shields.io/badge/Minimum%20SDK-35%20(Android%2015)-839192?logo=android&logoColor=white)
  ![Target SDK](https://img.shields.io/badge/Target%20SDK-37%20(Android%2017)-566573?logo=android&logoColor=white)

  <h3>简单、私密、完全离线的避孕药提醒</h3>
</div>

## 中文

安时是一款面向中文用户的开源 Android 避孕药提醒应用。所有设置和服药记录仅保存在设备本地，无账号、无广告、无分析服务。

主要功能：

- 按服药周期发送每日提醒
- 随时暂停提醒，并由用户决定下一周期何时开始
- 记录已服用药片和备注
- 提醒开始新药盒或补充药品
- 支持自定义有效药片和停药天数
- 本地备份与恢复
- 支持简体中文和英文

安时仅用于辅助提醒和个人记录，不能替代药品说明、医生或药师的专业建议。手机系统设置或第三方应用可能影响通知，请勿将本应用作为唯一提醒方式。

## English

Anshi is an open-source Android reminder for birth-control pill routines, designed primarily for Chinese-speaking users. Settings and pill records remain on the device. The app has no accounts, ads, analytics, or tracking.

Anshi supports Simplified Chinese and English. It provides configurable pill schedules, pause and resume controls, local history, refill reminders, and local backup and restore.

Anshi is a supportive reminder and personal record, not medical advice. Device settings or third-party applications may interfere with notifications, so the app should not be the only reminder method you rely on.

## Development

- Application ID: `io.github.anshireminder.app`
- Current development version: `0.1.0` (`versionCode` 1)
- Minimum supported version: Android 15 (API 35)
- Source branch: [`dev/anshi`](https://github.com/HaoranLI9/OpenPillReminder/tree/dev/anshi)

Build a debug APK with:

```shell
./gradlew assembleDebug
```

## Upstream And License

Anshi is a modified version of [OpenPillReminder](https://github.com/mariinkys/OpenPillReminder), originally created by Alex Marín. The Anshi fork was established in September 2026 and includes Simplified Chinese localization, explicit reminder pause controls, and independent product changes.

Original project copyright 2026 © Alex Marín.

Anshi modifications copyright 2026 © Haoran LI and contributors.

The complete application is distributed under [GNU GPL-3.0-only](./LICENSE). Modified releases must remain under GPL-3.0 and provide the corresponding source code.
