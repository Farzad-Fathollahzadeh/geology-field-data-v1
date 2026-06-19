# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Jetpack Compose classes
-keep class androidx.compose.** { *; }

# Preserve local Room database entities and business logic data models
-keep class com.example.** { *; }

# Preserve Room type converters and custom queries
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
    @androidx.room.Query *;
}

# Retrofit, OkHttp, Moshi, and general Kotlin serialization support
-keepattributes Signature, InnerClasses, EnclosingMethod, AnnotationDefault, *Annotation*
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}

-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn com.squareup.moshi.**

# Preserve line number information for crash report readability
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

