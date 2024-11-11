# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
   static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
   static **$* *;
}
-keepclassmembers class <2>$<3> {
   kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
   public static ** INSTANCE;
}
-keepclassmembers class <1> {
   public static <1> INSTANCE;
   kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,Annotation,InnerClasses

# Keep Result class to avoid serialization issues
-keep class kotlin.Result { *; }

-keepclassmembers class kotlinx.** {
    volatile <fields>;
}

-keepclassmembers class io.ktor.** { volatile <fields>; }
-keep public class org.slf4j.** { *; }
-keep public class ch.** { *; }

-keep public class * extends androidx.lifecycle.ViewModel {*;}

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
   static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
   static **$* *;
}
-keepclassmembers class <2>$<3> {
   kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
   public static ** INSTANCE;
}
-keepclassmembers class <1> {
   public static <1> INSTANCE;
   kotlinx.serialization.KSerializer serializer(...);
}

##------------------------------------------Begin: GSON---------------------------------------------
# @see https://github.com/google/gson/blob/master/examples/android-proguard-example/proguard.cfg
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { <fields>; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Keep Gson TypeToken type parameters
-keepclassmembers class * extends com.google.gson.reflect.TypeToken {
    <fields>;
}
##------------------------------------------End:   GSON---------------------------------------------

##------------------BEGIN: libsodium--------------------##
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
##--------------------END: libsodium--------------------##

##------------------BEGIN: OpenTelemetry--------------------##
-dontwarn com.fasterxml.jackson.core.JsonFactory
-dontwarn com.fasterxml.jackson.core.JsonGenerator
-dontwarn com.google.auto.value.AutoValue
-dontwarn com.google.auto.value.AutoValue$CopyAnnotations
-dontwarn com.google.auto.value.extension.memoized.Memoized
-dontwarn com.google.common.annotations.VisibleForTesting
-dontwarn com.google.common.util.concurrent.FutureCallback
-dontwarn com.google.common.util.concurrent.Futures
-dontwarn com.google.common.util.concurrent.MoreExecutors
-dontwarn io.grpc.CallOptions
-dontwarn io.grpc.Channel
-dontwarn io.grpc.ClientCall
-dontwarn io.grpc.ClientInterceptor
-dontwarn io.grpc.ClientInterceptors
-dontwarn io.grpc.Codec$Gzip
-dontwarn io.grpc.Codec$Identity
-dontwarn io.grpc.Codec
-dontwarn io.grpc.Metadata$AsciiMarshaller
-dontwarn io.grpc.Metadata$Key
-dontwarn io.grpc.Metadata
-dontwarn io.grpc.MethodDescriptor$Builder
-dontwarn io.grpc.MethodDescriptor$Marshaller
-dontwarn io.grpc.MethodDescriptor$MethodType
-dontwarn io.grpc.MethodDescriptor
-dontwarn io.grpc.Status
-dontwarn io.grpc.Status$Code
-dontwarn io.grpc.stub.AbstractFutureStub
-dontwarn io.grpc.stub.AbstractStub$StubFactory
-dontwarn io.grpc.stub.AbstractStub
-dontwarn io.grpc.stub.ClientCalls
-dontwarn io.grpc.stub.MetadataUtils
-dontwarn java.awt.Component
-dontwarn java.awt.GraphicsEnvironment
-dontwarn java.awt.HeadlessException
-dontwarn java.awt.Window
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.impl.StaticMDCBinder
-dontwarn org.slf4j.impl.StaticMarkerBinder
##--------------------END: OpenTelemetry--------------------##
