# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve the line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to hide the original
# source file name.
#-renamesourcefileattribute SourceFile

# Keep model classes (used with reflection/serialisation)
-keep class com.apexcaliberlabs.apexsync.model.** { *; }

# ZXing
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.** { *; }
