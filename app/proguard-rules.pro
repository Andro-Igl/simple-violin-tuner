# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**

# Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# App-specific classes (Data Classes for Settings)
-keep class com.example.simpleviolintunerad_free.ui.components.FrequencySettings { *; }
-keep class com.example.simpleviolintunerad_free.audio.ViolinString { *; }
-keep class com.example.simpleviolintunerad_free.viewmodel.TunerState { *; }
-keep class com.example.simpleviolintunerad_free.viewmodel.TuningStatus { *; }
