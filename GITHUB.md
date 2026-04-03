# GitHub 协作与提交指南

更新日期：2026-04-03

## 1. 分支约定

- 当前长期协作分支：`lyw_function_code`
- 功能改动建议：`feature/<name>`
- 修复改动建议：`fix/<name>`

## 2. 提交流程（建议）

```bash
git pull --ff-only origin lyw_function_code
git add .
git commit -m "feat: <summary>"
git push origin lyw_function_code
```

## 3. 提交信息建议

- `feat:` 新功能
- `fix:` 问题修复
- `refactor:` 重构
- `docs:` 文档更新
- `chore:` 构建或工具链调整

示例：

- `fix: stabilize backend game download stream response`
- `feat: add ops game profile APIs and runtime integration`
- `docs: refresh deployment and engine integration guides`

## 4. 提交前检查

- 后端可编译：`mvn -f backend/pom.xml -DskipTests compile`
- 前端可构建：
  - `npm --prefix dev-portal run build`
  - `npm --prefix ops-portal run build`
- Android 可编译：`cmd /c android-client\\gradlew.bat -p android-client assembleDebug`

## 5. 注意事项

- 不要提交敏感信息（密码、token、私钥）
- `.env`、本地缓存、构建产物按 `.gitignore` 处理
- 若真机联调，请在提交说明里写清楚本次 APK 对应的后端 BASE_URL
