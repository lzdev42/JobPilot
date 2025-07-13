# proguard-rules.pro
-ignorewarnings

# 保持 kotlinx 和 androidx 库的完整性
-keep class kotlinx.** { *; }
-keep class androidx.** { *; }

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
