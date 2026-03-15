# Roadrunner ProGuard / R8 rules

# Keep Google Tink crypto classes (obfuscation breaks key serialization)
-keep class com.google.crypto.tink.** { *; }

# Keep OSMDroid (reflection-heavy map tile providers)
-keep class org.osmdroid.** { *; }

# Keep Retrofit DTOs / remote data classes (Gson uses reflection)
-keep class com.roadrunner.app.data.remote.dto.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Strip all Log calls from release builds (security: no plaintext leaks in logcat)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
    public static int wtf(...);
}

# Retrofit
-keepattributes Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
