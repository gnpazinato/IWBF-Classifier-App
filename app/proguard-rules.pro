# kotlinx.serialization keeps generated serializers; keep @Serializable metadata.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# Keep the serializers for our model classes.
-keepclassmembers @kotlinx.serialization.Serializable class com.iwbfclassifier.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class com.iwbfclassifier.**
-keep class com.iwbfclassifier.**$$serializer { *; }
