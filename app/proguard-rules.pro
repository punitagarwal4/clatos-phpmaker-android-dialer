# ---- Kotlinx Serialization ----
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-dontnote kotlinx.serialization.**

# Keep the serializer() lookup and generated serializers
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    static **$Companion Companion;
    *** Companion;
}
-keepclasseswithmembers class **$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep the @Serializable model classes (DTOs) and their fields so JSON
# (de)serialization works in minified release builds.
-keep @kotlinx.serialization.Serializable class com.clatos.dialer.core.network.dto.** { *; }
-keep,includedescriptorclasses class com.clatos.dialer.**$$serializer { *; }

# ---- Retrofit / OkHttp ----
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep,allowobfuscation interface com.clatos.dialer.core.network.CrmApi
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ---- Room ----
-keep class * extends androidx.room.RoomDatabase { *; }
