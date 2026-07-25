# iOS certificate material

Put your HSBC sandbox certificates here:

| File | What it is |
|---|---|
| `transport.p12` | mTLS client identity — transport certificate + private key, PKCS#12 |
| `signing_key.pem` | private key that signs the `private_key_jwt` client assertion (PS256) |

Both are read from the app bundle at runtime by
`core/network/src/nativeMain/.../NetworkPlatformModule.native.kt`, which looks them up under this
`certs` directory. See the repository README for how to obtain the credentials and convert them.

Neither file is ever committed — `.gitignore` keeps everything in this directory out of the
repository except this README. The README is what makes the directory itself survive a fresh clone,
which the iOS build needs: `iosApp`'s Copy Bundle Resources references this folder, and Xcode fails
the build when the path does not exist.

A build with no certificates present is expected to succeed. The missing material surfaces at
runtime instead, when the HSBC client is constructed.
