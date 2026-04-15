import SwiftUI

struct CapsuleMenuOverlay: View {
    let isFavorite: Bool
    let onToggleFavorite: () -> Void
    let onExit: () -> Void
    let onRestart: () -> Void
    let onCopyLink: () -> Void
    let onShareWhatsApp: () -> Void
    let onShareFacebook: () -> Void
    let onFeedback: () -> Void

    var body: some View {
        Menu {
            Button(isFavorite ? "移除收藏" : "加入收藏", action: onToggleFavorite)
            Button("退出", role: .destructive, action: onExit)
            Button("重启", action: onRestart)
            Button("复制链接", action: onCopyLink)
            Button("WhatsApp", action: onShareWhatsApp)
            Button("Facebook", action: onShareFacebook)
            Button("反馈", action: onFeedback)
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
