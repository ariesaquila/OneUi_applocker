# Project specific ProGuard / R8 rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Suppress compile-time only annotation warnings (ErrorProne, Javax, Checker)
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**

# Room Database
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# DataStore & Coroutines
-dontwarn androidx.datastore.**
-dontwarn kotlinx.coroutines.**

# AndroidX Biometrics
-dontwarn androidx.biometric.**
