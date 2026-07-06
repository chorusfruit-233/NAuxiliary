# Repository Guidelines

## Project Structure & Module Organization

This is an Android project with one application module, `app`.

- `app/src/main/java/com/fruit/xposed/nauxv/`: Java source for the launcher activity, libxposed module, DexKit lookup, configuration, and translation logic.
- `app/src/main/res/`: Android resources, including launcher icons, layouts, strings, themes, and XML config.
- `app/src/main/assets/translations/zh-CN/`: translation assets split into `strings.properties`, `exact.properties`, and `phrases.properties`.
- `app/src/main/resources/META-INF/xposed/`: Xposed metadata such as scope and module init lists.
- `app/src/test/java/`: JVM unit tests for translation behavior and asset validity.
- `app/src/androidTest/java/`: Android instrumentation tests.
- `reverse-engineering/`: ignored local analysis output for pulled/decompiled third-party APKs.

## Build, Test, and Development Commands

- `./gradlew :app:assembleDebug`: builds the debug APK.
- `./gradlew :app:testDebugUnitTest`: runs JVM unit tests.
- `./gradlew :app:assembleDebug :app:testDebugUnitTest --rerun-tasks`: performs a clean verification-style rebuild and test run.
- `./gradlew installDebug`: installs the debug APK on a connected device.

Gradle may need access to `~/.gradle` for wrapper and dependency caches.

## Coding Style & Naming Conventions

The codebase is Java-first. Use 4-space indentation, explicit visibility, and descriptive method names. Keep hook IDs stable and namespaced, for example `nauxv.resources.getString` or `nauxv.setting.aboutAppComposeEntry`.

Prefer small helper methods around reflection, DexKit lookup, and UI hook behavior. Keep user-facing Chinese strings in resources when they belong to the module UI; host-process injected strings may remain constants when they are not module resources.

## Testing Guidelines

Unit tests use JUnit 4. Name tests after the behavior under test and keep translation tests focused on asset parsing, exact replacements, phrase replacements, and formatting.

Run `./gradlew :app:testDebugUnitTest` before submitting changes. For hook or resource changes, also run `./gradlew :app:assembleDebug`. Device-specific behavior, especially niconico hooks, requires manual validation in the target app.

## Commit & Pull Request Guidelines

The current Git history only contains an initial commit, so no strict commit convention is established. Use concise imperative commit messages, such as `Add niconico settings config entry`.

Pull requests should include a short summary, test commands run, and screenshots or screen recordings for launcher UI, icon, or in-app settings changes. Mention target niconico versions when hook behavior depends on decompiled classes or resource IDs.

## Security & Configuration Tips

Do not commit pulled APKs, decompiled sources, local device dumps, signing files, or `local.properties`. Keep analysis artifacts under ignored paths such as `reverse-engineering/`.
