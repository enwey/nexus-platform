# PROGRESS

更新日期：2026-04-03

## 已完成

- 后端下载接口修复并可稳定返回二进制流（解决 Android 运行页“游戏下载失败”主因）
- 下载模式默认采用后端代理流，避免真机访问预签名地址失败
- 发现页与游戏库接入真实数据接口并支持分类加载
- Android 登录/游客双态：
  - 游客可直接使用核心页面
  - 登录后同步最近游玩/收藏/分享行为
- 运行页胶囊与安全区适配：提供 `getSystemInfoSync`、`getMenuButtonBoundingClientRect`
- UpdateManager 基础链路：`check-update`、`applyUpdate`、运行时提示
- 运营后台新增 Runtime Ops Console（原 AndroidConsole 重命名）
- 新增并接通单游戏运营资料（`game_ops_profile`）：
  - 运行页资料
  - 分享资料
  - 发现卡片封面/Logo 覆写
- 文档与编码乱码清理（持续中）

## 已验证

- `mvn -f backend/pom.xml -DskipTests compile` 通过
- `npm --prefix ops-portal run build` 通过
- `cmd /c android-client\\gradlew.bat -p android-client assembleDebug` 通过
- 本地接口可用：
  - `GET /actuator/health`
  - `GET /game/public/list`
  - `GET /discover/home`
  - `GET /game/{appId}/runtime-profile`
  - `GET /game/download/{appId}`

## 当前待继续

- Android 运行页进一步消费运营素材（横幅/Logo/UI样式）
- iOS 端与最新后端接口对齐回归
- 完整 E2E 自动化（上传 -> 审核 -> 真机下载运行）
- 文档持续与代码同步（本次已完成一轮全量更新）
