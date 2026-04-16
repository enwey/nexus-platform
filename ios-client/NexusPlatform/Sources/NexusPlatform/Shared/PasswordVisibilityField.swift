import SwiftUI

struct PasswordVisibilityField: View {
    let title: String
    @Binding var text: String
    @Binding var isVisible: Bool

    var body: some View {
        HStack(spacing: 8) {
            Group {
                if isVisible {
                    TextField(title, text: $text)
                } else {
                    SecureField(title, text: $text)
                }
            }
            .textInputAutocapitalization(.never)
#if os(iOS)
            .autocorrectionDisabled()
#endif

            Button {
                isVisible.toggle()
            } label: {
                Image(systemName: isVisible ? "eye.slash" : "eye")
                    .foregroundStyle(AppTheme.ColorToken.textSecondary)
            }
            .buttonStyle(.plain)
        }
    }
}
