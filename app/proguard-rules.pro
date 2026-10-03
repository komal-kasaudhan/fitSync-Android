# Preserve data models and API interfaces for Gson and Retrofit
-keep class com.esha.fitsync.models.** { *; }
-keepclassmembers class com.esha.fitsync.models.** { *; }
-keep class com.esha.fitsync.api.** { *; }
-keepclassmembers class com.esha.fitsync.api.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}