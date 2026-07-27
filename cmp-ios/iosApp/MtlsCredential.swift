import Foundation
import Security

/// Builds the iOS mTLS client-certificate credential from a PKCS#12 bundle.
///
/// Kotlin/Native can't cleanly construct an `NSURLCredential` from a `SecIdentity`, so this Swift
/// helper does the `SecPKCS12Import` and hands the finished `URLCredential` to the shared module via
/// `IosMtlsBridgeKt.registerMtlsCredentialProvider`. Returns `nil` on any failure, so the Ktor Darwin
/// challenge handler falls back to default handling rather than crashing.
enum MtlsCredential {

    static func make(p12 data: Data, password: String) -> URLCredential? {
        // SecPKCS12Import rejects an empty passphrase, so bail early with a clear reason.
        guard !password.isEmpty else {
            NSLog("mTLS: empty PKCS#12 password — cannot import the client certificate.")
            return nil
        }

        let options = [kSecImportExportPassphrase as String: password] as CFDictionary
        var items: CFArray?
        let status = SecPKCS12Import(data as CFData, options, &items)
        guard status == errSecSuccess else {
            NSLog("mTLS: SecPKCS12Import failed (OSStatus \(status)) — check the p12 and password.")
            return nil
        }

        guard let dictionaries = items as? [[String: Any]],
              let first = dictionaries.first,
              let identityRef = first[kSecImportItemIdentity as String] else {
            NSLog("mTLS: no SecIdentity found in the PKCS#12 bundle.")
            return nil
        }

        let identity = identityRef as! SecIdentity
        let certificateChain = first[kSecImportItemCertChain as String] as? [SecCertificate]
        return URLCredential(identity: identity, certificates: certificateChain, persistence: .forSession)
    }
}
