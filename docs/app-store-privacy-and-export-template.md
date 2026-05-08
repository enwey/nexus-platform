# App Store Privacy And Export Template

更新日期：2026-05-02

本文件用于填写 `App Store Connect` 中的：
- `App Privacy`
- `Export Compliance`

## 1. Export Compliance

建议默认填写思路：

- `Does your app use encryption?`
  - `Yes`
- `Is your app designed to use cryptography or does it contain or incorporate cryptography?`
  - `Yes`
- `Does your app qualify for any exemptions provided in Category 5, Part 2 of the U.S. Export Administration Regulations?`
  - `Yes`

仓库侧已配：
- [Info.plist](/Users/apple/Documents/WordSpace/nexus-platform/ios-client/NexusPlatformApp/Info.plist) 中 `ITSAppUsesNonExemptEncryption = NO`

适用前提：
- 仅使用系统提供的 `HTTPS/TLS`
- 不提供自定义加密算法给最终用户
- 不把 App 作为通用加密工具分发

如果后续加入了：
- 自定义加密库
- 端到端私有协议加密产品能力
- 面向企业的通用加密/安全工具功能

则需要重新核对出口合规答案。

## 2. App Privacy

下面不是法律结论，而是基于当前代码结构给出的提交模板。正式提交前仍需产品/法务确认。

### 很可能需要披露的数据类型

- `Contact Info`
  - 如果注册/登录使用邮箱、手机号，通常需要披露。
- `Identifiers`
  - 如果服务端保存用户 ID、账号 ID、设备会话 ID，通常需要披露。
- `Purchases`
  - 如果账单、充值、订单记录与账号关联，通常需要披露。
- `User Content`
  - 如果用户可提交反馈、邀请内容、资料内容，需要按真实情况披露。

### 很可能不需要披露为“tracking”的前提

- 当前仓库未接入广告归因或 `AppTrackingTransparency`
- 未发现 `AdSupport` / `ATTrackingManager` / 第三方广告 SDK
- 运行时 bridge 已限制为平台受控能力，不向任意第三方域名透传宿主认证

### 提交前逐项确认

- 登录是否收集邮箱/手机号
- 服务端是否记录设备 ID、会话 ID、IP、崩溃日志
- 是否存在充值、订单、账单、邀请奖励等与账号绑定的数据
- 是否存在用户上传头像、昵称、反馈、聊天、客服工单
- 是否存在第三方统计 SDK、推送 SDK、客服 SDK

## 3. Recommended Submission Note

可附在内部提交记录里：

`The current iOS build uses standard platform HTTPS/TLS for backend communication and certificate pinning for server trust validation, but does not provide non-exempt encryption functionality to end users as a standalone encryption product. App Privacy answers should reflect the final production backend data flows associated with account login, billing, and hosted mini-game distribution.`
