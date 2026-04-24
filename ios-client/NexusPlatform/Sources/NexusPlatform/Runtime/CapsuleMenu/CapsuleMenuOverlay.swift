import SwiftUI

struct CapsuleMenuOverlay: View {
    let onOpenMenu: () -> Void
    let onExit: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            Button(action: onOpenMenu) {
                Image(systemName: "ellipsis")
                    .font(.system(size: 13, weight: .bold))
                    .frame(width: 46, height: 34)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            Rectangle()
                .fill(AppTheme.ColorToken.border.opacity(0.35))
                .frame(width: 1, height: 14)

            Button(action: onExit) {
                Image(systemName: "xmark")
                    .font(.system(size: 11, weight: .bold))
                    .frame(width: 41, height: 34)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        }
        .background(.ultraThinMaterial, in: Capsule())
        .overlay(Capsule().stroke(AppTheme.ColorToken.border.opacity(0.35), lineWidth: 1))
        .foregroundStyle(AppTheme.ColorToken.textPrimary)
    }
}
