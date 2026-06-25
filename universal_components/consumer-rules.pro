# --- Auth0 SDK Gson models ---------------------------------------------------
# The Auth0 Android SDK deserializes its API responses (Factor,
# AuthenticationMethod and its sealed subclasses) with Gson via reflection.
# R8 cannot see those reflective accesses, so without keep rules it strips the
# fields/classes and Gson fails — surfacing as "a0.sdk.internal_error.unknown".
# Gson also needs Signature (generic types like List<String>) and the
# @SerializedName annotations to map JSON keys correctly.
-keepattributes Signature
-keep class com.auth0.android.result.** { *; }
-keepclassmembers class com.auth0.android.result.** { *; }
# Polymorphic deserializer referenced via @JsonAdapter must be kept by name.
-keep class com.auth0.android.result.AuthenticationMethod$Deserializer { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --- Public enums passed across the API boundary -----------------------------
# AuthenticatorType is used as a kotlinx.serialization-backed Compose Navigation
# argument, which resolves the enum by its fully qualified name at runtime — so
# the class name must not be obfuscated.
-keep public enum com.auth0.universalcomponents.domain.model.AuthenticatorType {
    **[] $VALUES;
    public *;
}

# --- Strip SDK internal logging from minified consumer builds ----------------
-assumenosideeffects class com.auth0.universalcomponents.utils.logging.Logger {
    void d$*(...);
    void i$*(...);
    void v$*(...);
    void w$*(...);
    void e$*(...);
}