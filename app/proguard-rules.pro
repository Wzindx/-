# Project specific ProGuard/R8 rules.

-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# AndroidX Security depends on Tink. Some Tink bytecode references
# compile-time-only annotation APIs that are not packaged at runtime.
# They are safe to ignore for release shrinking.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**

-keep class kotlin.Metadata { *; }
