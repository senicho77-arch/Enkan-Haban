import SwiftUI
import Shared

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                // Google sign-in returns here: enkanhaban://login-callback
                .onOpenURL { url in AuthDeeplink_iosKt.handleAuthDeeplink(url: url) }
        }
    }
}