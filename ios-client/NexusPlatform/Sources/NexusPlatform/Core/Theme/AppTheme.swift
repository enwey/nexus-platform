import SwiftUI
import UIKit

/// Global design tokens for Nexus iOS app.
enum AppTheme {
    enum ColorToken {
        static let auroraPurple = Color(hex: 0x7C5CFF)
        static let auroraPink = Color(hex: 0xFF5FB2)
        static let auroraBlue = Color(hex: 0x4DA3FF)
        static let success = Color(hex: 0x36C282)
        static let warning = Color(hex: 0xF6B447)
        static let danger = Color(hex: 0xEF5A5A)

        static let background = Color.dynamic(light: 0xF5F7FB, dark: 0x0E1016)
        static let surface = Color.dynamic(light: 0xFFFFFF, dark: 0x171A24)
        static let surfaceSecondary = Color.dynamic(light: 0xEEF1F8, dark: 0x23283A)
        static let textPrimary = Color.dynamic(light: 0x131722, dark: 0xF2F5FF)
        static let textSecondary = Color.dynamic(light: 0x56607A, dark: 0xA8B1C7)
        static let border = Color.dynamic(light: 0xD7DDEF, dark: 0x30374B)
    }

    enum GradientToken {
        static let hero = LinearGradient(
            colors: [ColorToken.auroraPurple, ColorToken.auroraPink, ColorToken.auroraBlue],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    enum Layout {
        static let pagePadding: CGFloat = 16
        static let cardRadius: CGFloat = 18
        static let capsuleRadius: CGFloat = 22
        static let buttonHeight: CGFloat = 52
    }
}

extension View {
    func nexusPageBackground() -> some View {
        background(AppTheme.ColorToken.background.ignoresSafeArea())
    }

    func nexusGlassCard() -> some View {
        self
            .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: AppTheme.Layout.cardRadius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: AppTheme.Layout.cardRadius, style: .continuous)
                    .stroke(AppTheme.ColorToken.border.opacity(0.4), lineWidth: 1)
            )
    }

    func nexusPrimaryCTA() -> some View {
        self
            .frame(maxWidth: .infinity)
            .frame(height: AppTheme.Layout.buttonHeight)
            .background(AppTheme.GradientToken.hero, in: RoundedRectangle(cornerRadius: AppTheme.Layout.capsuleRadius, style: .continuous))
            .foregroundStyle(.white)
    }
}

extension Color {
    init(hex: UInt32, alpha: Double = 1.0) {
        let red = Double((hex >> 16) & 0xFF) / 255.0
        let green = Double((hex >> 8) & 0xFF) / 255.0
        let blue = Double(hex & 0xFF) / 255.0
        self = Color(.sRGB, red: red, green: green, blue: blue, opacity: alpha)
    }

    static func dynamic(light: UInt32, dark: UInt32) -> Color {
        Color(
            UIColor { trait in
                trait.userInterfaceStyle == .dark ? UIColor(hex: dark) : UIColor(hex: light)
            }
        )
    }
}

private extension UIColor {
    convenience init(hex: UInt32, alpha: CGFloat = 1) {
        let red = CGFloat((hex >> 16) & 0xFF) / 255.0
        let green = CGFloat((hex >> 8) & 0xFF) / 255.0
        let blue = CGFloat(hex & 0xFF) / 255.0
        self.init(red: red, green: green, blue: blue, alpha: alpha)
    }
}
