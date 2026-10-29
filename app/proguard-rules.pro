# Proguard rules for Aurora Music
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepclassmembers class * {
    @androidx.room.* *;
}
