# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- Custom ProGuard Rules for AI Education ---

# 1. Retrofit & Gson
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.google.gson.** { *; }
# Keep all data models used by Gson for parsing API responses
-keep class com.example.common.network.llm.** { *; }

# 2. Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# 3. Sherpa ONNX (JNI/C++)
-keep class com.k2fsa.sherpa.onnx.** { *; }
-keepclassmembers class com.k2fsa.sherpa.onnx.** {
    <methods>;
}

# 4. Math Parser (exp4j)
-keep class net.objecthunter.exp4j.** { *; }

# 5. FFmpegKit
-keep class com.arthenica.ffmpegkit.** { *; }

# 6. YouTubeDL
-keep class com.yausername.youtubedl_android.** { *; }

# 7. Hilt / Dagger
-keep class * extends android.app.Application {
    <init>();
}