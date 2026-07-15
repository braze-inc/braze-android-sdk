# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in android-sdk/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep names for XML-inflated views and manifest-declared components
-keepnames class com.braze.ui.** { *; }
-keepnames class com.braze.push.** { *; }

# Public integration entry points outside com.braze.ui / com.braze.push
-keepnames class com.braze.IBrazeDeeplinkHandler
-keepnames class com.braze.BrazeActivityLifecycleCallbackListener

-dontwarn com.braze.ui.**
-dontwarn com.google.firebase.messaging.**

-keepclassmembers class * {
   @android.webkit.JavascriptInterface <methods>;
}
