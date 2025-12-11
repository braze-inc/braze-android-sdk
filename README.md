<p align="center">
  <img width="480" alt="Braze Logo" src=".github/assets/logo-light.png#gh-light-mode-only" />
  <img width="480" alt="Braze Logo" src=".github/assets/logo-dark.png#gh-dark-mode-only" />
</p>

# Braze Android SDK [![latest](https://img.shields.io/github/v/tag/braze-inc/braze-android-sdk?label=latest%20release&color=300266)](https://github.com/braze-inc/braze-android-sdk/releases) [![Static Badge](https://img.shields.io/badge/KDoc-801ed7)](https://braze-inc.github.io/braze-android-sdk/kdoc/)

Successful marketing automation is essential to the future of your mobile app. Braze helps you engage your users beyond the download. Visit the following links for details and we'll have you up and running in no time!

- [Braze User Guide](https://www.braze.com/docs/user_guide/introduction/ "Braze User Guide")
- [Braze Developer Guide](https://www.braze.com/docs/developer_guide/platforms/android/sdk_integration/ "Braze Developer Guide")

## Quickstart

``` groovy
// build.gradle

// ...
repositories {
  mavenCentral()
}
// ...
dependencies {
  `implementation 'com.braze:android-sdk-ui:40.1.+'`
  `implementation 'com.braze:android-sdk-location:40.1.+'`
}
// ...
```

``` xml
<!-- res/values/braze.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
  <string translatable="false" name="com_braze_api_key">YOUR_APP_IDENTIFIER_API_KEY</string>
  <string translatable="false" name="com_braze_custom_endpoint">YOUR_CUSTOM_ENDPOINT_OR_CLUSTER</string>
</resources>
```

``` Kotlin
Braze.getInstance(context).changeUser("Jane Doe");
```

See [the Braze Developer Guide](https://www.braze.com/docs/developer_guide/sdk_integration/?sdktab=android) for advanced integration options.

## Version Support

> [!IMPORTANT]
> The Braze Android SDK declares a `minSdkVersion` of API 21+. This allows the SDK to be compiled into apps supporting as early as API 21. Note that while this allows the ability to compile, we do not provide formal support for < API 25, and cannot guarantee that the SDK will work as intended on devices running those versions.
> 
> If your app supports those versions, you should:
> - Validate your integration of the SDK works as intended on physical devices (not just emulators) for those API versions.
> - If you cannot validate expected behavior, you must either call [disableSDK](https://braze-inc.github.io/braze-android-sdk/kdoc/braze-android-sdk/com.braze/-braze/-companion/disable-sdk.html), or not initialize the SDK on those versions. Otherwise, you may cause unintended side effects or degraded performance on your end users’ devices.

Tool | Minimum Supported Version
:----|:----
minSdk|5.0+ / API 21+ (Lollipop and up)
targetSdk|36
Kotlin|`org.jetbrains.kotlin:kotlin-stdlib:2.0.20`
Firebase Cloud Messaging|24.1.0
Font Awesome|4.3.0

## Modules

Module | Description
:----|:----
`android-sdk-base`|the Braze SDK base analytics library.
`android-sdk-ui`|the Braze SDK user interface library for in-app messages, push, content cards, and banners.
`android-sdk-location`|the Braze SDK location library for location and geofences.
`android-sdk-jetpack-compose`|the Braze SDK library for Jetpack Compose support.
`droidboy`|a sample app demonstrating how to use Braze in-depth.
`android-sdk-unity`|a library that enables Braze SDK integrations on Unity.
`samples`|a folder containing several sample apps for various integration options.

## Contact

If you have questions, please contact [support@braze.com](mailto:support@braze.com).
