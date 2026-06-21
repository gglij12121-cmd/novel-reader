// 项目级 build.gradle.kts
// 这里定义整个项目共享的插件和配置

plugins {
    // Android应用插件
    alias(libs.plugins.android.application) apply false
    // Kotlin Android插件
    alias(libs.plugins.kotlin.android) apply false
    // Kotlin序列化插件
    alias(libs.plugins.kotlin.serialization) apply false
    // Hilt依赖注入插件
    alias(libs.plugins.hilt) apply false
    // KSP注解处理器插件
    alias(libs.plugins.ksp) apply false
}
