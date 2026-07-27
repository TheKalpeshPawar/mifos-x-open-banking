import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        // Supply the iOS mTLS client certificate to the shared module before any network client is
        // built. The shared Darwin challenge handler calls this to answer HSBC's client-cert challenge.
        IosMtlsBridgeKt.registerMtlsCredentialProvider { data, password in
            MtlsCredential.make(p12: data, password: password)
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                // HSBC's consent redirect. The registered HTTPS callback page relays the response
                // to our custom scheme, which iOS delivers here. Forwarded straight into shared
                // Kotlin, where the data layer parses and authenticates it.
                .onOpenURL { url in
                    ConsentRedirectBridgeKt.handleConsentRedirect(url: url.absoluteString)
                }
        }
    }
}
