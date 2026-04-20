# Moshi
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keep class kotlin.reflect.jvm.internal.impl.** { *; }

# Retrofit
-keepattributes RuntimeVisibleAnnotations
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Kotlinx serialization
-keepattributes InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.zeddihub.mobile.**$$serializer { *; }
-keepclassmembers class com.zeddihub.mobile.** { *** Companion; }
-keepclasseswithmembers class com.zeddihub.mobile.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Project DTOs
-keep class com.zeddihub.mobile.data.remote.dto.** { *; }
