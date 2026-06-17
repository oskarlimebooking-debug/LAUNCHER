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

# T2.7 — AppCompatDelegate loads this class by FQN string from the theme's
# viewInflaterClass attribute via Class.forName(). Without -keep, R8 renames
# it and AppCompatDelegate logs "Failed to instantiate" and falls back to the
# unsafe MaterialComponentsViewInflater on API 23.
-keep class com.oskar.retrolauncher.SafeMaterialComponentsViewInflater { <init>(); }

# Suppress Timber's no-op stripping warnings
-assumenosideeffects class timber.log.Timber {
    public static void v(...);
    public static void d(...);
}
