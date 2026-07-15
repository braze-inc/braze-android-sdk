<p align="center">
  <img width="480" alt="Braze Logo" src=".github/assets/logo-light.png#gh-light-mode-only" />
  <img width="480" alt="Braze Logo" src=".github/assets/logo-dark.png#gh-dark-mode-only" />
</p>

# Braze Android SDK [![latest](https://img.shields.io/github/v/tag/braze-inc/braze-android-sdk?label=latest%20release&color=300266)](https://github.com/braze-inc/braze-android-sdk/releases) [![Static Badge](https://img.shields.io/badge/KDoc-801ed7)](https://braze-inc.github.io/braze-android-sdk/kdoc/)

Successful marketing automation is essential to the future of your mobile app. Braze helps you engage your users beyond the download. To get started, see the following resources:

- [Braze User Guide](https://www.braze.com/docs/user_guide/introduction/ "Braze User Guide")
- [Braze Developer Guide](https://www.braze.com/docs/developer_guide/platforms/android/sdk_integration/ "Braze Developer Guide")

## Quickstart

The following snippets show the minimum configuration required to add the Braze Android SDK to your app.

``` groovy
// build.gradle

// ...
repositories {
  mavenCentral()
}
// ...
dependencies {
  `implementation 'com.braze:android-sdk-ui:43.0.+'`
  `implementation 'com.braze:android-sdk-location:43.0.+'`
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

For more information about advanced integration options, see the [Braze Developer Guide](https://www.braze.com/docs/developer_guide/sdk_integration/?sdktab=android).

## Version support

> [!IMPORTANT]
> The Braze Android SDK declares a `minSdkVersion` of API 21+, which allows the SDK to compile into apps supporting as early as API 21. While the SDK compiles for those versions, Braze doesn't provide formal support for API versions below 25, and the SDK may not work as intended on devices running those versions.
>
> If your app supports those versions, do the following:
>
> - Validate that your integration of the SDK works as intended on physical devices (not just emulators) for those API versions.
> - If you can't validate expected behavior, you must either call [disableSDK](https://braze-inc.github.io/braze-android-sdk/kdoc/braze-android-sdk/com.braze/-braze/-companion/disable-sdk.html) or skip initializing the SDK on those versions. Otherwise, you may cause unintended side effects or degraded performance on your users' devices.

The following table lists the minimum supported versions for tools used by the Braze Android SDK.

Tool | Minimum supported version
:----|:----
minSdk|5.0+ / API 21+ (Lollipop and up)
targetSdk|37
Kotlin|`org.jetbrains.kotlin:kotlin-stdlib:2.2.20`
Firebase Cloud Messaging|25.1.0
Font Awesome|4.3.0

## Modules

The following table describes each module in the Braze Android SDK.

Module | Description
:----|:----
`android-sdk-base`|The Braze SDK base analytics library.
`android-sdk-ui`|The Braze SDK user interface library for in-app messages, push, Content Cards, and banners.
`android-sdk-location`|The Braze SDK location library for location and geofences.
`android-sdk-jetpack-compose`|The Braze SDK library for Jetpack Compose support.
`droidboy`|A sample app that demonstrates how to use Braze in depth.
`android-sdk-unity`|A library that enables Braze SDK integrations on Unity.
`samples`|A folder that contains sample apps for various integration options.

## Contact

For questions, contact Braze Technical Support.
