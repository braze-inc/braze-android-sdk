<p align="center">
  <img width="480" alt="Braze Logo" src=".github/assets/logo-light.png#gh-light-mode-only" />
  <img width="480" alt="Braze Logo" src=".github/assets/logo-dark.png#gh-dark-mode-only" />
</p>

# Android SDK

Successful marketing automation is essential to the future of your mobile app. Braze helps you engage your users beyond the download. Visit the following links for details and we'll have you up and running in no time!

- [Braze User Guide](https://www.braze.com/docs/user_guide/introduction/ "Braze User Guide")
- [Braze Developer Guide](https://www.braze.com/docs/developer_guide/platforms/android/sdk_integration/ "Braze Developer Guide")
- [KDoc](https://braze-inc.github.io/braze-android-sdk/kdoc/ "Braze Android SDK Class Documentation")

## Android API support

The Braze Android SDK supports Android API 25+.

> [!IMPORTANT]
> The Braze Android SDK declares a `minSdkVersion` of API 21+. This allows the SDK to be compiled into apps supporting as early as API 21. Note that while this allows the ability to compile, we are not reintroducing formal support for < API 25, and cannot guarantee that the SDK will work as intended on devices running those versions.
> 
> If your app supports those versions, you should:
> - Validate your integration of the SDK works as intended on physical devices (not just emulators) for those API versions.
> - If you cannot validate expected behavior, you must either call [disableSDK](https://braze-inc.github.io/braze-android-sdk/kdoc/braze-android-sdk/com.braze/-braze/-companion/disable-sdk.html), or not initialize the SDK on those versions. Otherwise, you may cause unintended side effects or degraded performance on your end users’ devices.

## Version Information

- The Braze Android SDK supports Android 5.0+ / API 21+ (Lollipop and up).
- Last Target SDK Version: 35
- Kotlin version: `org.jetbrains.kotlin:kotlin-stdlib:2.0.20`
- Last Compiled Firebase Cloud Messaging Version: 24.1.0
- Braze uses [Font Awesome](https://fontawesome.com/v4/) 4.3.0 for in-app message icons. Check out the [cheat sheet](https://fontawesome.com/v4/cheatsheet/) to browse available icons.

## Components

- `android-sdk-base` - the Braze SDK base analytics library.
- `android-sdk-ui` - the Braze SDK user interface library for in-app messages, push, and the news feed.
- `android-sdk-location` - the Braze SDK location library for location and geofences.
- `android-sdk-jetpack-compose` - the Braze SDK location library for Jetpack Compose support.
- `droidboy` - a sample app demonstrating how to use Braze in-depth.
- `android-sdk-unity` - a library that enables Braze SDK integrations on Unity.
- `samples` - a folder containing several sample apps for various integration options.

## Remote repository for gradle
The version should match the git version tag, or the most recent version noted in the changelog. An example dependency declaration is:

Our SDK is now hosted in Maven Central. You can remove `https://braze-inc.github.io/braze-android-sdk/sdk` from your build.gradle and make sure you have `mavenCentral()` as a repository.

```
dependencies {
  implementation 'com.braze:android-sdk-ui:36.0.+'
  implementation 'com.braze:android-sdk-location:36.0.+'
  ...
}
```

## Installing android-sdk-ui to Your Local Maven Repository
To install the UI library as an AAR file to your local maven repository, run the `install` task with
`./gradlew install`. You can reference it with groupId `com.braze` and artifactId `android-sdk-ui`. The version should
match the git version tag, or the most recent version noted in the changelog. An example dependency declaration is:

```
repositories {
  mavenLocal()
  ...
}
```

```
dependencies {
  implementation 'com.braze:android-sdk-ui:36.0.+'
}
```

## Questions?

If you have questions, please contact [support@braze.com](mailto:support@braze.com).
