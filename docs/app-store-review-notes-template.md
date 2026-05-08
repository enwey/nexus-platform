# App Store Review Notes Template

更新日期：2026-05-02

以下内容可直接整理后粘贴到 `App Store Connect -> App Review Information -> Notes`。

---

This app is a hosted mini-game container built for platform-reviewed HTML5 / JavaScript mini games in accordance with App Review Guideline 4.7.

How content is controlled:
1. Developers upload mini-game ZIP packages to our platform.
2. Our backend validates the package structure, `manifest.json`, entry file, and allowed mini-app type before distribution.
3. The platform normalizes package metadata and only approved versions are made available to the iOS app.
4. The iOS app only downloads and runs platform-approved hosted packages.

Runtime restrictions:
- Mini games run inside the app's controlled web runtime.
- The bridge exposed to mini games is limited to platform-defined APIs such as login, restricted network request, local mini-game storage, update check, and layout helpers.
- Mini-game network requests are restricted to our platform backend domain only.
- Host authentication credentials are only attached to approved backend requests and are not exposed to arbitrary third-party domains.

Reviewer test account:
- Account: `[REVIEWER_ACCOUNT]`
- Password: `[REVIEWER_PASSWORD]`

Review steps:
1. Open the app.
2. Sign in with the reviewer account above.
3. Go to the discovery page.
4. Open the game detail page for `[REVIEW_GAME_NAME]`.
5. Tap the launch button to run the approved mini game.

Sample review content:
- Review game name: `[REVIEW_GAME_NAME]`
- Review game appId: `[REVIEW_GAME_APP_ID]`
- Review-safe category: `[REVIEW_GAME_CATEGORY]`

If needed, the following internal review path can be demonstrated by our team in a separate environment:
- Developer uploads game package
- Platform validates manifest and package structure
- Operations team approves the reviewed version
- Approved version becomes available to the iOS host app

Additional notes:
- The app does not execute arbitrary third-party native code.
- Hosted packages are restricted to reviewed web content.
- The production iOS build uses HTTPS and certificate pinning for backend communication.

Fallback content in case the primary demo game is unavailable:
- Fallback game name: `[FALLBACK_GAME_NAME]`
- Fallback game appId: `[FALLBACK_GAME_APP_ID]`

Contact for App Review questions:
- Name: `[REVIEW_CONTACT_NAME]`
- Email: `[REVIEW_CONTACT_EMAIL]`
- Phone: `[REVIEW_CONTACT_PHONE]`

---

## 中文参考版

本应用是一个平台托管的小游戏宿主，承载的平台内容为经过审核的 `HTML5 / JavaScript` 小游戏，审核和分发流程遵循平台受控模式。

内容控制方式如下：
1. 开发者将小游戏 ZIP 包上传到平台。
2. 平台后端会校验包结构、`manifest.json`、入口文件和允许的小游戏类型。
3. 平台对包元数据进行规范化处理，只有审核通过的版本才会进入分发。
4. iOS 客户端只会下载并运行平台审核通过的托管包。

运行时限制如下：
- 小游戏运行在宿主受控的 Web 运行时内。
- 宿主仅向小游戏暴露受控 bridge，例如登录、受限网络请求、本地小游戏存储、更新检查和布局辅助能力。
- 小游戏网络请求仅允许访问平台后端域名。
- 宿主认证信息只会附加到平台允许的后端请求，不会暴露给任意第三方域名。

审核账号：
- 账号：`[REVIEWER_ACCOUNT]`
- 密码：`[REVIEWER_PASSWORD]`

审核步骤：
1. 打开应用。
2. 使用审核账号登录。
3. 进入发现页。
4. 打开 `[REVIEW_GAME_NAME]` 的详情页。
5. 点击启动按钮运行小游戏。
