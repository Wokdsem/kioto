import SwiftUI
import app

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            NodeNavRenderer()
                .ignoresSafeArea(.all)
        }
    }
}

struct NodeNavRenderer: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        ExampleIOsApplication().getNodeNavUIViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
