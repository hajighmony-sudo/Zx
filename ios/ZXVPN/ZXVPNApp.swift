import SwiftUI

@main
struct ZXVPNApp: App {
    @StateObject private var store = AppStore()
    @StateObject private var vpn = VPNController()
    @StateObject private var explorer = ExplorerEngine()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(store)
                .environmentObject(vpn)
                .environmentObject(explorer)
                .preferredColorScheme(.dark)
                .task { await vpn.load() }
        }
    }
}
