# FAPI signs the private_key_jwt client assertion with PS256 (RSA-PSS). On Android that only works
# because `cryptography-provider-jdk-bc` contributes a ServiceLoader `DefaultJdkSecurityProvider`
# that hands BouncyCastle to the JDK cryptography provider — Android's own JCA never registers the
# standard name `RSASSA-PSS`.
#
# The implementation is reached *only* by name, from
# META-INF/services/dev.whyoleg.cryptography.providers.jdk.DefaultJdkSecurityProvider, so R8 sees no
# reference to it and is free to strip it. That failure is invisible in debug (minify off) and fatal
# in release: NoSuchAlgorithmException "RSASSA-PSS Signature not available" the moment a user taps
# through to the bank. Keep the SPI and its implementation.
-keep class dev.whyoleg.cryptography.providers.jdk.DefaultJdkSecurityProvider { *; }
-keep class dev.whyoleg.cryptography.providers.jdk.bc.** { *; }

# The BouncyCastle classes the provider instantiates reflectively.
-keep class org.bouncycastle.jce.provider.BouncyCastleProvider { *; }
-dontwarn dev.whyoleg.cryptography.providers.jdk.**
