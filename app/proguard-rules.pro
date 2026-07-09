# Ktor/Supabase - slf4j optional logging, tidak dipakai tapi direferensikan
-dontwarn org.slf4j.**
-keep class org.slf4j.** { *; }

# Ktor client engine
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }

# Kotlinx Serialization - dipakai reflection untuk data class model Supabase
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.web.apps.**$$serializer { *; }
-keepclassmembers class com.web.apps.** {
    *** Companion;
}
-keepclasseswithmembers class com.web.apps.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Supabase SDK
-dontwarn io.github.jan.supabase.**
-keep class io.github.jan.supabase.** { *; }