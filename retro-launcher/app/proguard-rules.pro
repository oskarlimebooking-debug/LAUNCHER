# Moshi codegen-generated adapters
-keep class **JsonAdapter { *; }
-keepclassmembers class kotlin.Metadata { public <methods>; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { *; }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** { **[] $VALUES; public *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Reflection wrappers in system/HiddenApi.kt — keep nothing else.
-keep class com.oskar.retrolauncher.system.HiddenApi { *; }

# Suppress Timber's no-op stripping warnings
-assumenosideeffects class timber.log.Timber {
    public static void v(...);
    public static void d(...);
}
