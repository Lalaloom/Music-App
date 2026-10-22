# Add project specific ProGuard rules here.
# https://www.guardsquare.com/manual/configuration/usage

# Keep Hilt generated components
-keep class * extends dagger.hilt.android.internal.managers.ActivityComponentManager { *; }

# Keep Room entities
-keep @androidx.room.Entity class * { *; }

# Keep Media3 components
-keep class androidx.media3.** { *; }
