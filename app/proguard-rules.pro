# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep Jsoup classes
-keeppackagenames org.jsoup.nodes
-keep class org.jsoup.** { *; }

# Keep Retrofit interfaces
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep Room entities
-keep class com.reader.novel.data.local.entity.** { *; }

# Keep data models
-keep class com.reader.novel.data.model.** { *; }
