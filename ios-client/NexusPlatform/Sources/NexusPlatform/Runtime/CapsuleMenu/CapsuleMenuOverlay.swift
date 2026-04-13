import SwiftUI

struct CapsuleMenuOverlay: View {
    let onExit: () -> Void
    let onRestart: () -> Void
    let onShare: () -> Void

    var body: some View {
        Menu {
            Button("退出", role: .destructive, action: onExit)
            Button("重启", action: onRestart)
            Button("分享", action: onShare)
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "ellipsis")
                    .font(.system(size: 13, weight: .bold))
                Divider()
                    .frame(height: 12)
                Image(systemName: "xmark")
                    .font(.system(size: 11, weight: .bold))
            }
            .padding(.horizontal, 12)
            .frame(height: 36)
            .background(.ultraThinMaterial, in: Capsule())
            .overlay(Capsule().stroke(AppTheme.ColorToken.border.opacity(0.35), lineWidth: 1))
            .foregroundStyle(AppTheme.ColorToken.textPrimary)
        }
    }
}
