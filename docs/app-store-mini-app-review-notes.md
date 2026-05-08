# App Store Mini-App Review Notes

更新日期：2026-05-02

## 1. 产品定位

- 宿主 App 提供基于 `HTML5 / JavaScript` 的小游戏运行能力。
- 开发者上传 ZIP 包后，由平台进行校验、规范化、审核和分发。
- iOS 客户端只运行平台审核通过的 hosted mini-app package。

## 2. 运行时边界

- 小游戏包必须包含 `index.html` 与 `manifest.json`。
- 平台在服务端校验 `manifest.json` 的 `entry` 与 `kind`，并注入平台生成的 `appId`、`version`。
- iOS 客户端安装时再次校验 hosted manifest，防止非平台规范包运行。
- 小游戏 `wx.request` 仅允许访问平台后端域名，不允许任意第三方域名。
- 宿主认证信息只会附加到平台后端请求，不会透传到外部站点。

## 3. 宿主能力说明

- 提供受控 bridge：登录、受限网络请求、本地小游戏存储、更新检查、基础布局信息。
- 不暴露任意原生 API 给小游戏。
- 小游戏本地存储与宿主数据隔离，清理小游戏存储不会影响宿主全局数据。

## 4. 安全与传输

- 正式环境默认使用 `HTTPS` 平台接口。
- iOS 端启用证书指纹 pinning，支持多指纹轮换配置。
- 调试环境仅对 `localhost / 127.0.0.1` 允许本地开发例外。
- 宿主登录态存储在 Keychain，不使用明文 `UserDefaults` 持久化 token。

## 5. 平台审核流程

1. 开发者上传小游戏 ZIP。
2. 服务端检查 ZIP 路径、`index.html`、`manifest.json`、`entry`、`kind`。
3. 平台规范化 manifest 并生成运行时安全包。
4. 版本进入 `DRAFT / SUBMITTED / APPROVED / REJECTED` 审核流。
5. iOS 端只拉取和运行平台发布后的运行包。

## 6. Reviewer 说明建议

- 提供一个审核账号。
- 提供至少一个已审核通过的小游戏条目。
- 提供开发者上传后台和运营审核后台的测试路径。
- 说明小游戏内容由平台审核后分发，客户端不会运行任意第三方代码包。
- 说明小游戏请求只能访问平台后端，不会向任意外部域名发送宿主认证信息。

## 7. 提审前核对

- `PLATFORM_API_BASE_URL` 指向正式 `HTTPS` 域名。
- `BACKEND_CERT_SHA256` 已配置正式证书指纹，建议至少配置当前与下一张证书。
  当前仓库 release 默认值：`c9a7f0f39f555fc834ae1a401c3ab1ab0a5571d120150be933f47deb5e06d516`
- 审核账号可访问小游戏列表、详情与启动路径。
- 至少一款小游戏通过上传、审核、下载、启动、更新链路验证。
