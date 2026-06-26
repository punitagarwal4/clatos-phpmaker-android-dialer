# Keep Kotlinx Serialization metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}

# Retrofit
-keepattributes Signature, Exceptions
-dontwarn okhttp3.**
-dontwarn retrofit2.**

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
