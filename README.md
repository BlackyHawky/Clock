<!--suppress CheckImageSize -->
# Clock
Clock is a privacy-conscious open-source clock, based on AOSP Clock.

[<img src="/images/badge_github.png" alt="Get it on GitHub" height="80">](https://github.com/BlackyHawky/Clock/releases)
[<img src="/images/badge_f-droid.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/com.best.deskclock/)
[<img src="/images/badge_izzy_on_droid.png" alt="Get it on IzzyOnDroid" height="80">](https://apt.izzysoft.de/fdroid/index/apk/com.best.deskclock/)

## Table of Contents

- [Features](#features)
  * [Common Issues](#common-issues)
- [Contributing](#contributing-)
  * [Reporting Issues](#reporting-issues)
  * [Translation](#translation)
  * [Code Contribution](#code-contribution)
- [License](#license)
- [Screenshots](#screenshots)
- [Credits](#credits)

# Features
* Flip and shake action to dismiss/postpone alarm;
* Turn off/postpone the alarm with the power button or volume buttons;
* For some Snapdragon devices only, the alarm is triggered when they are switched off;
  * Unfortunately, this feature may not work on some devices despite the presence of the _“com.qualcomm.qti.poweroffalarm”_ system app. See the discussion [here](https://github.com/BlackyHawky/Clock/issues/88).
* Swipe to delete an alarm;
* Duplicate alarms;
* Customizable alarm title;
* Customizable ringtone;
* Light, dark or system theme;
* AMOLED mode for dark theme;
* Digital or analog clock style;
* Display home time when traveling;
* Display the time in many cities around the world;
* Timer, stopwatch and bedtime _(experimental)_ included;
* Possibility of sharing your stopwatch with your contacts;
* Customizable interface;
* Customizable screensaver;
* Modern widgets;
* Customizable widgets;
* Backup and restore settings;
* Material design;
* Dynamic colors for Android 12+;
* Support for [Reproducible Builds](https://reproducible-builds.org/). See the discussion [here](https://github.com/BlackyHawky/Clock/issues/140).

## Common Issues
* Problem encountered with bedtime mode as this is an experimental feature;
* Maybe other things, but nothing about the alarm's functionality.

⚠ _<b>As I'm not an expert developer, some problems may unfortunately not be solved without help.</b>_ ⚠

# Contributing ❤

## Reporting Issues

Whether you encountered a bug, or want to see a new feature in Clock, you can contribute to the project by opening a new issue [here](https://github.com/BlackyHawky/Clock/issues). Your help is always welcome!

Before opening a new issue, be sure to check the following:
- **Does the issue already exist?** Make sure a similar issue has not been reported by browsing [existing issues](https://github.com/BlackyHawky/Clock/issues). Please search open and closed issues.
- **Is the issue still relevant?** Make sure your issue is not already fixed in the latest version of Clock.
- **Did you use the issue template?** It is important to make life of our kind contributors easier by avoiding issues that miss key information to their resolution.
  Note that issues that ignore part of the issue template will likely get treated with very low priority, as often they are needlessly hard to read or understand (e.g. huge screenshots, or addressing multiple topics).

## Translation
Translations can be added using [Weblate](https://translate.codeberg.org/projects/clock/). You will need an account to update translations and add languages. Add the language you want to translate to in Languages -> Manage translated languages in the top menu bar.
Updating translations in a PR will not be accepted, as it may cause conflicts with Weblate translations.

<details>
<summary><b>Click here to see the translation status</b></summary>
<br>

[![Translation status](https://translate.codeberg.org/widget/clock/clock/multi-auto.svg)](https://translate.codeberg.org/engage/clock/)
</details>

## Code Contribution

### Getting Started

Clock project is based on Gradle and Android Gradle Plugin. To get started, you can install [Android Studio](https://developer.android.com/studio), and import project 'from Version Control / Git / Github' by providing this git repository [URL](https://github.com/BlackyHawky/Clock.git) (or git SSH URL).
Of course you can also use any other compatible IDE, or work with text editor and command line.

Once everything is up correctly, you're ready to go!

### Guidelines

Clock is a complex application, when contributing, you must take a step back and make sure your contribution:
- **Is actually wanted**. Best check related open issues before you start working on a PR. Issues with "help wanted" label are accepted, but still it would be good if you announced that you are working on it.
  If there is no issue related to your intended contribution, it's a good idea to open a new one to avoid disappointment of the contribution not being accepted. For small changes or fixing obvious bugs this step is not necessary.
- **Is only about a single thing**. Mixing unrelated contributions into a single PR is hard to review and can get messy.
- **Has a proper description**. What your contribution does is usually less obvious to reviewers than for yourself. A good description helps a lot for understanding what is going on, and for separating wanted from unintended changes in behavior.
- **Uses already in-place mechanism and take advantage of them**. In other terms, does not reinvent the wheel or uses shortcuts that could alter the consistency of the existing code.
- **Has a low footprint**. Some parts of the code are executed very frequently, and the keyboard should stay responsive even on older devices.
- **Does not bring any non-free code or proprietary binary blobs**. This also applies to code/binaries with unknown licenses. Make sure you do not introduce any closed-source library from Google.
  If your contribution contains code that is not your own, provide a link to the source.
- **Complies with the user privacy principle Clock follows**.

Please leave dependency upgrades to the maintainers, unless it's an actual security issue.

# License

Clock is licensed under GNU General Public License v3.0.

> Permissions of this strong copyleft license are conditioned on making available complete source code of licensed works and modifications, which include larger works using a licensed work, under the same license. Copyright and license notices must be preserved. Contributors provide an express grant of patent rights.

See repo's [LICENSE](/LICENSE-GPL-3) file.

Since the app is based on Apache 2.0 licensed AOSP Clock, an [Apache 2.0](LICENSE-Apache-2.0) license file is provided too.

# Screenshots

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

# Credits
- [qw123wh](https://github.com/qw123wh)
- [crDroid Android](https://github.com/crdroidandroid/android_packages_apps_DeskClock)
- [LineageOS](https://github.com/LineageOS/android_packages_apps_DeskClock)
