<!--suppress CheckImageSize -->
# <img width="24" height="24" alt="image" src="/fastlane/metadata/android/en-US/images/icon.png" /> Clock
**Clock** is a customizable, privacy‑first, open-source clock app inspired by AOSP Clock.  
It combines modern design, powerful features, and transparency — giving you full control over your time.

# 📑 Table of Contents

- [Download](#-download)
- [Features](#-features)
- [Common Issues](#-common-issues)
- [Contributing](#-contributing)
- [License](#-license)
- [Screenshots](#-screenshots)
- [Credits](#-credits)

# 📥 Download

[<img src="/images/badge_github.png" alt="Get it on GitHub" height="80">](https://github.com/BlackyHawky/Clock/releases)
[<img src="/images/badge_f-droid.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/com.best.deskclock/)
[<img src="/images/badge_izzy_on_droid.png" alt="Get it on IzzyOnDroid" height="80">](https://apt.izzysoft.de/fdroid/index/apk/com.best.deskclock/)
[<img src="/images/badge_obtainium.png" alt="Get it on Obtainium" height="80">](https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/BlackyHawky/Clock/releases)
[<img src="/images/badge_openApk.png" alt="Get it on OpenApk" height="80">](https://www.openapk.net/clock/com.best.deskclock/)

> [!NOTE]  
> **Build variants:**
> - **Release:** Stable versions recommended for everyday use (available on all platforms above).
> - **Nightly:** Experimental builds with the latest changes, may be unstable (available on GitHub, and also on Obtainium if you enable _"Include prereleases"_).
> - **Debug:** Developer-oriented builds with extra logging and diagnostics (available only on GitHub).
>
> All variants (Release, Nightly, Debug) can be installed side by side without conflict.

# ✨ Features

### • ⏰ **Advanced alarms**

* Set alarms to a specific date
* Flip or shake to dismiss/postpone
* Use power or volume buttons to snooze/stop
* Swipe to delete, duplicate, or customize alarms
* Custom titles, backgrounds, and ringtones (including random playback)

### • 🎨 **Customization**
  * Light, dark, or system theme
  * AMOLED mode for deep blacks
  * Digital or analog clock styles
  * Customizable interface, screensaver, and widgets
  * Dynamic colors for Android 12+

### • 🌍 **World clock**
  * Display home time when abroad
  * View time in multiple cities worldwide

### • ⏱️ **Timer & stopwatch**
  * Built-in timer and stopwatch
  * Share stopwatch results with contacts

### • ⚙️ **Extra features**
  * Quick settings tiles (Android 7+)
  * Backup & restore (except custom ringtones)
  * Material Design UI
  * Support for [Direct Boot](https://developer.android.com/privacy-and-security/direct-boot)
  * Alarm support on some Snapdragon devices when powered off
  * [Reproducible Builds](https://reproducible-builds.org/) for transparency

> [!NOTE]  
> Some extra features may not work on certain devices:
> - **Direct Boot support**: see the discussion [here](https://github.com/BlackyHawky/Clock/issues/396).
> - **Power‑off alarm on Snapdragon devices**: may fail even if the _"com.qualcomm.qti.poweroffalarm"_ system app is present. See the discussion [here](https://github.com/BlackyHawky/Clock/issues/88).

# 🐞 Common Issues

* Device-specific issues may occur due to limited testing.
* On Android 14+ with HyperOS, the _"Full screen notification"_ permission may be revoked. Possible solution [here](https://github.com/BlackyHawky/Clock/discussions/303#discussioncomment-13407709).
* MIUI users may face problems due to aggressive battery optimizations.
  * Please make sure that battery optimizations are disabled for the app before opening an issue.

> [!NOTE]  
> I’m not an expert developer, so some problems may require community help to solve.

# 🤝 Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on reporting issues, translations, and code contributions.

<details>
<summary><b>Click here to see the translation status</b></summary>
<br>

[![Translation status](https://translate.codeberg.org/widget/clock/clock/multi-auto.svg)](https://translate.codeberg.org/engage/clock/)
</details>

# 📜 License

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

Clock is licensed under **GNU General Public License v3.0 (GPLv3)**.
This strong copyleft license requires that any modifications or larger works using Clock must also be distributed under the same license, with complete source code available.

See the [LICENSE](LICENSE) file for full details.

> [!NOTE]  
> Since Clock is based on **AOSP Clock**, which is licensed under **Apache License 2.0**, an additional [Apache 2.0](LICENSE-Apache-2.0) license file is provided in this repository.

# 📷 Screenshots

<details>
<summary><b>Click here to see screenshots</b></summary>
<br>
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/01.jpg" alt="Screenshot 01" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/02.jpg" alt="Screenshot 02" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/03.jpg" alt="Screenshot 03" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/04.jpg" alt="Screenshot 04" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/05.jpg" alt="Screenshot 05" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/06.jpg" alt="Screenshot 06" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/07.jpg" alt="Screenshot 07" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/08.jpg" alt="Screenshot 08" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/09.jpg" alt="Screenshot 09" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/10.jpg" alt="Screenshot 10" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/11.jpg" alt="Screenshot 11" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/12.jpg" alt="Screenshot 12" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/13.jpg" alt="Screenshot 13" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/14.jpg" alt="Screenshot 14" width="200" />
</details>

# 🏅 Credits
* 🖼️ **App icon** inspired by [LineageOS DeskClock](https://github.com/LineageOS/android_packages_apps_DeskClock), modified by [BlackyHawky](https://github.com/BlackyHawky)
* 💻 Code references and inspiration from:
    * [LineageOS](https://github.com/LineageOS/android_packages_apps_DeskClock)
    * [crDroid Android](https://github.com/crdroidandroid/android_packages_apps_DeskClock)
* 🌍 Translations provided by the community via [Weblate](https://translate.codeberg.org/projects/clock/)
* 🤝 Thanks to all [contributors](https://github.com/BlackyHawky/Clock/graphs/contributors) who help improve Clock