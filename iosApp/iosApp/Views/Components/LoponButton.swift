import SwiftUI

struct LoponButton: View {
    let title: String
    var systemImage: String? = nil
    var role: ButtonRole? = nil
    var isLoading: Bool = false
    var action: () -> Void

    var body: some View {
        Button(role: role, action: action) {
            HStack(spacing: 8) {
                if isLoading {
                    ProgressView()
                        .tint(.white)
                }
                if let img = systemImage {
                    Image(systemName: img)
                }
                Text(title)
                    .font(.headline)
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        .controlSize(.large)
        .disabled(isLoading)
    }
}
