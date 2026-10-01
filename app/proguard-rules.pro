# ---- SafeShield R8 rules ----
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# Kotlin / Coroutines
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# Moshi (reflection-free codegen, keep generated adapters + annotations)
-keep class **JsonAdapter { *; }
-keepnames @com.squareup.moshi.JsonClass class *
-keepclassmembers @com.squareup.moshi.JsonClass class * { <init>(...); }
-keep,allowobfuscation @interface com.squareup.moshi.JsonClass

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * { @retrofit2.http.* <methods>; }
-dontwarn retrofit2.**
-dontwarn javax.annotation.**

# Room
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# Hilt / WorkManager workers instantiated reflectively
-keep class * extends androidx.work.ListenableWorker { <init>(...); }

# Our DTOs used by Moshi codegen
-keep class com.safeshield.app.data.remote.dto.** { *; }
-keep class com.safeshield.app.data.local.json.** { *; }

# Keep enum values used in Room type converters
-keepclassmembers enum com.safeshield.app.** { *; }
