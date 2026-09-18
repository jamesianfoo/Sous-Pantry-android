# ── Gson: field names ARE the JSON keys, so serialized classes must not be renamed ──
-keep class com.souspantry.app.data.models.** { *; }
# Proxy request/response bodies (DirectClaude.kt). Renaming these breaks every AI call.
-keep class com.souspantry.app.services.Anthropic* { *; }
# Parked ILMU client — kept so it still works if it's wired back up.
-keep class com.souspantry.app.services.Ilmu* { *; }

# Generic types are read back at runtime (Room converters store List<SuggestedMeal> as
# JSON via anonymous TypeToken subclasses). Gson 2.10.1, which Retrofit's converter
# pulls in, ships no R8 rules of its own, so without these history and plans crash.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep class com.google.gson.reflect.TypeToken { *; }
-keep,allowobfuscation class * extends com.google.gson.reflect.TypeToken

-keepattributes *Annotation*
