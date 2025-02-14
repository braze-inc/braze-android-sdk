# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in android-sdk/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keeping classes in com.braze.ui because not keeping them can cause
# build failures for users using Google Play Services with Braze.
-keepnames class com.braze.ui.** { *; }

-dontwarn com.amazon.device.messaging.**
-dontwarn bo.app.**
-dontwarn com.braze.ui.**
