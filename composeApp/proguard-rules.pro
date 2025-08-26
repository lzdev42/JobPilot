# proguard-rules.pro
-ignorewarnings

# 关闭可能导致验证错误的优化与混淆
-dontoptimize
-dontobfuscate

# 保持 kotlinx 和 androidx 库的完整性
-keep class kotlinx.** { *; }
-keep class androidx.** { *; }

# Kotlinx Serialization 生成的序列化器与内部实现
-keep class **$$serializer { *; }
-keep class kotlinx.serialization.descriptors.SerialDescriptor { *; }
-keep class kotlinx.serialization.internal.PluginGeneratedSerialDescriptor { *; }
-keep class kotlinx.serialization.encoding.** { *; }
-keep class kotlinx.serialization.modules.** { *; }

# GSON（根据 ProGuard 提示补充）
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class com.google.gson.stream.JsonReader { *; }
-keep class com.google.gson.stream.JsonWriter { *; }

# Playwright
-keep class com.microsoft.playwright.** { *; }
-dontwarn com.microsoft.playwright.**

# 保留必要的元数据属性
-keepattributes *Annotation*
-keepattributes Signature

# 保护关键库中可能被反射调用的方法
-keepclassmembers class kotlinx.** {
    public <methods>;
    protected <methods>;
}
-keepclassmembers class androidx.** {
    public <methods>;
    protected <methods>;
}