# Project specific ProGuard/R8 rules.

-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**
-keep class kotlin.Metadata { *; }
