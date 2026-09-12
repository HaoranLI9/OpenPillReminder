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
- 提供标准单相 21+7 复方短效避孕药的漏服处理指南
- 本地备份与恢复
- 支持简体中文和英文

安时仅用于辅助提醒和个人记录，不能替代药品说明、医生或药师的专业建议。手机系统设置或第三方应用可能影响通知，请勿将本应用作为唯一提醒方式。

## English

Anshi is an open-source Android reminder for birth-control pill routines, designed primarily for Chinese-speaking users. Settings and pill records remain on the device. The app has no accounts, ads, analytics, or tracking.

Anshi supports Simplified Chinese and English. It provides configurable pill schedules, pause and resume controls, local history, refill reminders, a missed-pill guide for standard monophasic 21+7 combined pills, and local backup and restore.

Anshi is a supportive reminder and personal record, not medical advice. Device settings or third-party applications may interfere with notifications, so the app should not be the only reminder method you rely on.

## 下载与安装

需要 **Android 15 或更高版本**（API 35）。更早的系统无法安装。

最新版本：[GitHub Releases](https://github.com/HaoranLI9/OpenPillReminder/releases/latest)

### 方式一：直接下载 APK

在 Releases 页面下载 `Anshi-v*.apk`，用手机打开即可安装。系统会提示"未知来源应用"，
需要在弹出的设置里允许你的浏览器或文件管理器安装应用。安装包已用发布密钥签名，
与这里公布的 SHA-256 校验值一致。

这种方式适合装一次就够用的情况。想省事的话推荐用下面的方式二。

### 方式二：用 Obtainium 自动更新（推荐）

[Obtainium](https://github.com/ImranR98/Obtainium) 是一个开源的 Android 应用更新器，
它直接从 GitHub Releases 读取安装包，因此不需要应用商店，也不经过任何中间方。

1. 先装 Obtainium 本身（从它的 Releases 页面下载 APK）
2. 打开 Obtainium，点添加应用
3. 粘贴本仓库地址：`https://github.com/HaoranLI9/OpenPillReminder`
4. 保存后即可安装，之后有新版本会自动提示并更新

装完这一步，你就不用再手动找安装包了。

### 关于应用商店

安时没有上架 Google Play。开源应用商店的收录进度会在这里更新。

## Download & Install

Requires **Android 15 or later** (API 35). Earlier versions cannot install it.

Latest release: [GitHub Releases](https://github.com/HaoranLI9/OpenPillReminder/releases/latest)

### Option 1: Download the APK

Grab `Anshi-v*.apk` from the Releases page and open it on your phone. Android will ask
about installing from an unknown source — allow it for the browser or file manager you
used. The APK is signed with the release key and matches the published SHA-256 checksum.

This works well if you only need to install once.

### Option 2: Automatic updates with Obtainium (recommended)

[Obtainium](https://github.com/ImranR98/Obtainium) is an open-source Android app updater
that reads installers straight from GitHub Releases. No store, no middleman.

1. Install Obtainium itself (APK from its Releases page)
2. Open Obtainium and add an app
3. Paste this repository: `https://github.com/HaoranLI9/OpenPillReminder`
4. Save, install, and future versions will be offered automatically

### Stores

Anshi is not on Google Play. Listing progress for open-source stores will be noted here.

## Development

- Application ID: `io.github.anshireminder.app`
- Current development version: `0.1.0` (`versionCode` 1)
- Minimum supported version: Android 15 (API 35)
- Source branch: [`main`](https://github.com/HaoranLI9/OpenPillReminder/tree/main)

Build a debug APK with:

```shell
./gradlew assembleDebug
```

## Upstream And License

Anshi is a modified version of [OpenPillReminder](https://github.com/mariinkys/OpenPillReminder), originally created by Alex Marín. The Anshi fork was established in September 2026 and includes Simplified Chinese localization, explicit reminder pause controls, and independent product changes.

Original project copyright 2026 © Alex Marín.

Anshi modifications copyright 2026 © lmoon and contributors.

The complete application is distributed under [GNU GPL-3.0-only](./LICENSE). Modified releases must remain under GPL-3.0 and provide the corresponding source code.
