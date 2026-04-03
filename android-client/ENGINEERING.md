# Android Engineering Notes

更新日期：2026-04-03

## 当前状态

- 架构采用 feature + layered
- 运行页支持安全区与胶囊避让
- 发现页/游戏库接入真实数据
- 401 -> refresh -> retry 已在网络层实现
- 可按登录态区分本地缓存与服务端同步行为

## 关键实现点

1. `PlatformBackendApi` 统一后端访问
2. `GameManager` 负责下载、解压、版本检查、更新应用
3. `GameRuntimeActivity` 负责运行时容器与 bridge 注入
4. `BackendConfig` 通过 `BuildConfig.BACKEND_BASE_URL` 注入环境地址

## 联调注意

- 真机包必须用局域网后端地址编译
- 若运行页提示下载失败，优先排查 `/game/download/{appId}` 返回码

## 后续建议

- 运行页进一步消费运营素材（横幅/Logo）
- 增加 UI smoke 自动化测试
