# Clock
Clock is a privacy-conscious open-source clock, based on AOSP Clock.

## Table of Contents

- [Features](#features-)
  * [Common Issues](#common-issues)
- [Contributing](#contributing-)
  * [Reporting Issues](#reporting-issues)
  * [Code Contribution](#code-contribution)
- [License](#license)
- [Credits](#credits)
- [Download](#download)

# Features : 
* Flip and shake action to dismiss/postpone alarm;
* Turn off/postpone the alarm with the power button or volume buttons;
* For Snapdragon phones only, the alarm is triggered when the phone is switched off;
* Swipe to delete an alarm;
* Customize alarm title;
* Customizable ringtone;
* Light, dark or system theme;
* AMOLED mode for dark theme;
* Digital or analog clock style;
* Display home time when traveling;
* Display the time in many cities around the world;
* Timer, stopwatch and bedtime included;
* Possibility of sharing your stopwatch with your contacts;
* Customizable screensaver;
* Material design;
* Dynamic colors for Android 12+;

## Common Issues
* Problem encountered when using the "Dim background" option in bedtime mode;
* Maybe other things, but nothing about the alarm's functionality.

_As I'm not an expert developer, some problems may unfortunately not be solved._

# Contributing ❤

## Reporting Issues

Whether you encountered a bug, or want to see a new feature in Clock, you can contribute to the project by opening a new issue [here](https://github.com/BlackyHawky/Clock/issues). Your help is always welcome!

Before opening a new issue, be sure to check the following:
- **Does the issue already exist?** Make sure a similar issue has not been reported by browsing [existing issues](https://github.com/BlackyHawky/Clock/issues). Please search open and closed issues.
- **Is the issue still relevant?** Make sure your issue is not already fixed in the latest version of Clock.
- **Did you use the issue template?** It is important to make life of our kind contributors easier by avoiding issues that miss key information to their resolution.
  Note that issues that that ignore part of the issue template will likely get treated with very low priority, as often they are needlessly hard to read or understand (e.g. huge screenshots, or addressing multiple topics).

## Code Contribution

### Getting Started

Clock project is based on Gradle and Android Gradle Plugin. To get started, you can install [Android Studio](https://developer.android.com/studio), and import project 'from Version Control / Git / Github' by providing this git repository [URL](https://github.com/BlackyHawky/Clock.git) (or git SSH URL).
Of course you can also use any other compatible IDE, or work with text editor and command line.

Once everything is up correctly, you're ready to go!

### Guidelines

Clock is a complex application, when contributing, you must take a step back and make sure your contribution:
- **Is actually wanted**. Best check related open issues before you start working on a PR. Issues with "PR" and "contributor needed" labels are accepted, but still it would be good if you announced that you are working on it.
  If there is no issue related to your intended contribution, it's a good idea to open a new one to avoid disappointment of the contribution not being accepted. For small changes or fixing obvious bugs this step is not necessary.
- **Is only about a single thing**. Mixing unrelated contributions into a single PR is hard to review and can get messy.
- **Has a proper description**. What your contribution does is usually less obvious to reviewers than for yourself. A good description helps a lot for understanding what is going on, and for separating wanted from unintended changes in behavior.
- **Uses already in-place mechanism and take advantage of them**. In other terms, does not reinvent the wheel or uses shortcuts that could alter the consistency of the existing code.
- **Has a low footprint**. Some parts of the code are executed very frequently, and the keyboard should stay responsive even on older devices.
- **Does not bring any non-free code or proprietary binary blobs**. This also applies to code/binaries with unknown licenses. Make sure you do not introduce any closed-source library from Google.
  If your contribution contains code that is not your own, provide a link to the source.
- **Complies with the user privacy principle Clock follows**.

# License

Clock is licensed under GNU General Public License v3.0.

> Permissions of this strong copyleft license are conditioned on making available complete source code of licensed works and modifications, which include larger works using a licensed work, under the same license. Copyright and license notices must be preserved. Contributors provide an express grant of patent rights.

See repo's [LICENSE](/LICENSE-GPL-3) file.

Since the app is based on Apache 2.0 licensed AOSP Clock, an [Apache 2.0](LICENSE-Apache-2.0) license file is provided too.

# Credits
- [qw123wh](https://github.com/qw123wh/new-clock)
- [crDroid Android](https://github.com/crdroidandroid/android_packages_apps_DeskClock)
- [LineageOS](https://github.com/LineageOS/android_packages_apps_DeskClock)

# Download
[<img src="https://github.com/machiav3lli/oandbackupx/blob/034b226cea5c1b30eb4f6a6f313e4dadcbb0ece4/badge_github.png" alt="Get it on GitHub" height="80">](https://github.com/BlackyHawky/New-Clock/releases)
