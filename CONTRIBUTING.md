# Contributing to Clock

Thank you for your interest in contributing! 🙌  
There are many ways to help improve Clock: reporting issues, translating, or contributing code.

## 📢 Reporting Issues

- Check if the issue already exists ([browse issues](https://github.com/BlackyHawky/Clock/issues)).
- Ensure it’s still relevant in the latest version.
- Keep it focused on a single topic.
- Write in English only.
- Use the issue template for clarity.

> [!IMPORTANT]  
> Issues that ignore the template may be treated with lower priority or even closed.

### Sharing Logs

- **Release builds**  
  To attach logs, please follow these steps:

1. Notice the bug.
2. Go to **About**, click 5 times on the version number.
3. Tap the insect 🐞 icon that appears.
4. Save the generated `.zip` file to your device.
5. Attach the `.zip` file in your issue comment.

- **Nightly & Debug builds**  
  Logs can be accessed directly via the insect 🐞 icon in **About** — no need to click 5 times on the version number.

> [!IMPORTANT]  
> Providing logs greatly increases the chance of your issue being resolved quickly.

## 🌍 Translation

Translations are managed via [Weblate](https://translate.codeberg.org/projects/clock/).

You can also request new languages directly on Weblate if your language is not yet available.

> [!IMPORTANT]  
> Please do not submit translation PRs directly, as they may conflict with Weblate.

## 💻 Code Contribution

### Getting Started

- Install [Android Studio](https://developer.android.com/studio) or another compatible IDE.
- Clone the repository:
  ```bash
  git clone https://github.com/BlackyHawky/Clock.git
  cd Clock
  ./gradlew build
  ```

### Guidelines

* [ ] ✅ Check if your contribution is wanted (issues with help wanted label are good starting points).
* [ ] 🧩 Keep PRs focused on a single topic.
* [ ] 📝 Provide a clear description.
* [ ] 🔄 Reuse existing mechanisms, don’t reinvent the wheel.
* [ ] ⚡ Keep performance in mind (especially for frequently executed code).
* [ ] 🚫 No non-free or proprietary code.
* [ ] 🔒 Respect user privacy principles.

> [!IMPORTANT]  
> Leave dependency upgrades to maintainers unless there’s a strong reason.

### Pull Requests

* Fork the repository.
* Create a feature branch (`git checkout -b feature/my-feature`).
* Commit your changes with clear, descriptive messages.
* Push to your fork and open a Pull Request.
* Ensure your PR description explains the motivation and changes clearly.

> [!IMPORTANT]  
> For larger changes, please open an issue first to discuss your ideas before starting development.
