![Braze Logo](https://github.com/braze-inc/braze-android-sdk/blob/master/braze-logo.png)

# Android SDK

Successful marketing automation is essential to the future of your mobile app. Braze helps you engage your users beyond the download. Visit the following links for details and we'll have you up and running in no time!

- [Braze User Guide](https://www.braze.com/docs/user_guide/introduction/ "Braze User Guide")
- [Braze Developer Guide](https://www.braze.com/docs/developer_guide/platform_integration_guides/android/initial_sdk_setup/android_sdk_integration/ "Braze Developer Guide")
- [KDoc](https://braze-inc.github.io/braze-android-sdk/kdoc/ "Braze Android SDK Class Documentation")

## Version Information

- The Braze Android SDK supports Android 5.0+ / API 21+ (Lollipop and up).
- Last Target SDK Version: 34
- Kotlin version: `org.jetbrains.kotlin:kotlin-stdlib:1.8.10`
- Last Compiled Firebase Cloud Messaging Version: 23.2.0
- Braze uses [Font Awesome](https://fortawesome.github.io/Font-Awesome/) 4.3.0 for in-app message icons. Check out the [cheat sheet](http://fortawesome.github.io/Font-Awesome/cheatsheet/) to browse available icons.

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
  implementation 'com.braze:android-sdk-ui:30.2.+'
  implementation 'com.braze:android-sdk-location:30.2.+'
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
  implementation 'com.braze:android-sdk-ui:30.2.+'
}
```

## Questions?

If you have questions, please contact [support@braze.com](mailto:support@braze.com).
