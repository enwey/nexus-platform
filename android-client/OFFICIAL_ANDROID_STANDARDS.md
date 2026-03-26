# Android 官方开发标准（项目执行版）

更新时间：2026-03-26  
适用范围：`android-client` 全模块（UI、交互、架构、可访问性、国际化、发布）

## 1. 官方依据（唯一来源）

以下文档是本项目标准的来源，后续实现必须对齐：

1. App Architecture（官方推荐架构）  
   https://developer.android.com/topic/architecture
2. Navigation with Compose  
   https://developer.android.com/develop/ui/compose/navigation
3. Predictive Back（实现）  
   https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture
4. Predictive Back（Compose）  
   https://developer.android.com/develop/ui/compose/system/predictive-back
5. Material 3 in Compose（含 Typography 官方字阶）  
   https://developer.android.com/develop/ui/compose/designsystems/material3
6. Material3 Typography API  
   https://developer.android.com/reference/kotlin/androidx/compose/material3/Typography
7. Edge-to-edge 设计指南  
   https://developer.android.com/design/ui/mobile/guides/layout-and-content/edge-to-edge
8. Compose Edge-to-edge Setup  
   https://developer.android.com/develop/ui/compose/system/setup-e2e
9. Accessibility（App Quality）  
   https://developer.android.com/guide/topics/ui/accessibility/apps

---

## 2. 架构标准（必须）

1. 分层：`UI -> (Domain) -> Data`，依赖方向单向。  
2. UI 层只消费状态，不直接拼装网络逻辑。  
3. 状态流采用单向数据流（UDF）：`Intent/Event -> State update -> Render`。  
4. Repository 不得被 UI 直接绕过。  
5. 业务错误用统一错误模型，不在各页面散落硬编码错误文案。

---

## 3. 导航与返回标准（必须）

1. 使用 Navigation Compose 管理导航图，不手写分叉式跳转链。  
2. 系统返回键、手势返回、页面返回按钮必须语义一致。  
3. 启用 Predictive Back，并使用官方支持 API。  
4. 不在根页面“吞掉”返回事件导致不可预期行为。  
5. 二级页面返回必须回到上一级，而不是退出应用。

---

## 4. 交互动效标准（必须）

1. 页面切换采用一致的前进/返回方向规则（前进与返回方向相反）。  
2. 优先使用系统/Compose 默认导航动画或 Material 体系动画。  
3. 禁止局部页面“无动效”、另一些页面“重动效”的割裂体验。  
4. 动效不能引入白屏/闪屏/露底。

---

## 5. 白闪与视觉稳定标准（必须）

1. `windowBackground`、Compose 根容器背景、页面容器背景必须一致。  
2. 切换动画容器不得露出默认白底。  
3. Edge-to-edge 时正确处理系统栏 inset，避免内容与手势区冲突。  
4. 使用 `adjustResize` 处理输入法，避免键盘弹出抖动与遮挡。

---

## 6. 字体与排版标准（必须）

使用 Material 3 官方字阶，不自造随机字号：

1. `displayLarge 57/64`
2. `displayMedium 45/52`
3. `displaySmall 36/44`
4. `headlineLarge 32/40`
5. `headlineMedium 28/36`
6. `headlineSmall 24/32`
7. `titleLarge 22/28`
8. `titleMedium 16/24`
9. `titleSmall 14/20`
10. `bodyLarge 16/24`
11. `bodyMedium 14/20`
12. `bodySmall 12/16`
13. `labelLarge 14/20`
14. `labelMedium 12/16`
15. `labelSmall 11/16`

项目要求：

1. 页面主标题优先 `headlineLarge/Medium`。  
2. 分组标题优先 `titleLarge/Medium`。  
3. 正文优先 `bodyMedium/Large`。  
4. 按钮文案优先 `labelLarge`。  
5. 禁止在页面内临时写死 `sp` 破坏体系。

---

## 7. 可访问性标准（必须）

1. 可交互控件触控区域至少 `48dp x 48dp`。  
2. 文字对比度：  
   - 小字（<18pt 或粗体 <14pt）≥ 4.5:1  
   - 其他文字 ≥ 3:1  
3. 自定义可交互元素需有可聚焦与可见反馈。  
4. 文案要可读，避免图标代替全部语义。

---

## 8. 国际化标准（必须）

1. 所有用户可见文案必须进 `strings.xml`，禁止硬编码。  
2. 至少维护：繁体中文、简体中文、英文。  
3. 语言切换需可持久化，并在界面层即时生效。  
4. 错误消息也要可本地化，不允许只返回英文内部错误。

---

## 9. 组件与设计系统标准（必须）

1. 颜色、圆角、字体、间距使用 Design Token，不在页面散写。  
2. 公共组件（按钮、底栏、卡片）统一行为与样式。  
3. 禁止同类组件多套风格并存。  
4. 新页面必须复用既有 token 与组件。

---

## 10. 质量门禁（合入前必须满足）

1. `assembleDebug` 通过。  
2. 核心导航链路人工验证通过：
   - 首页 -> 二级页 -> 返回
   - 手势返回与按钮返回一致
3. 语言切换三语验证通过。  
4. 无白闪、无明显掉帧、无页面露底。  
5. 关键页面文案无乱码、无硬编码遗留。

---

## 11. 本项目执行规则（给 AI 与开发者）

1. 任何 UI/交互改动，先对照本文档后实现。  
2. 偏离官方标准时，必须给出“偏离原因 + 风险 + 回退方案”。  
3. 不允许“先改了再说”；必须按标准一次做对。  
4. 评审意见优先级：交互一致性 > 可访问性 > 视觉细节。

