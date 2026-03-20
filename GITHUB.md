# GitHub 上传指南

本指南将帮助你将 Nexus Platform 项目上传到 GitHub。

## 📋 前置条件

### 1. GitHub 账号
- 如果还没有 GitHub 账号，请先注册：https://github.com/signup
- 建议使用个人账号而不是组织账号

### 2. Git 安装
- **Windows**: 下载 Git for Windows: https://git-scm.com/download/win
- **macOS**: 使用 Homebrew 安装：`brew install git`
- **Linux**: 使用包管理器安装：`sudo apt-get install git`

### 3. Git 配置
```bash
# 配置用户名和邮箱
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# 配置默认分支
git config --global init.defaultbranch main
```

---

## 🚀 创建 GitHub 仓库

### 方式一：通过 GitHub 网页创建（推荐）

#### 步骤 1：创建新仓库
1. 访问 GitHub：https://github.com/new
2. 填写仓库信息：
   - **Repository name**: nexus-platform
   - **Description**: Nexus Platform - 独立小游戏平台
   - **Public**: ✅ 勾选公开仓库
   - **Initialize with**: README
   - **Add .gitignore**: ✅ 勾选推荐
   - **Choose license**: MIT
3. 点击 "Create repository"

#### 步骤 2：上传项目文件
1. 进入项目目录：
   ```bash
   cd nexus-platform
   ```

2. 初始化 Git 仓库（如果还没有）：
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   ```

3. 如果已经有 Git 仓库，添加远程仓库：
   ```bash
   git remote add origin https://github.com/your-username/nexus-platform.git
   ```

4. 推送代码到 GitHub：
   ```bash
   git push -u origin main
   ```

---

### 方式二：通过 GitHub CLI（命令行）

#### 步骤 1：创建 Personal Access Token
1. 登录 GitHub
2. 点击右上角头像 → Settings
3. 左侧菜单 → Developer settings
4. 点击 "Personal access tokens" → "Generate new token"
5. 填写 token 信息：
   - **Note**: 输入描述，如 "Nexus Platform Development"
   - **Expiration**: 选择 "No expiration"（永不过期）
   - **Select scopes**: 勾选 `repo`（完整仓库访问权限）
6. 点击 "Generate token"
7. **重要**: 复制生成的 token，只显示一次，之后无法再查看

#### 步骤 2：使用 CLI 创建仓库
1. 安装 GitHub CLI（如果还没有）：
   ```bash
   # macOS
   brew install gh
   
   # Windows
   # 下载并安装：https://cli.github.com/
   
   # Linux
   sudo apt-get install gh
   ```

2. 登录 GitHub CLI：
   ```bash
   gh auth login
   ```
   
   输入你的 GitHub 用户名和密码

3. 创建仓库：
   ```bash
   cd nexus-platform
   gh repo create nexus-platform \
     --public \
     --description "Nexus Platform - 独立小游戏平台" \
     --source=. \
     --remote=origin
   ```

4. 添加远程仓库：
   ```bash
   cd nexus-platform
   git remote add origin https://github.com/your-username/nexus-platform.git
   ```

5. 推送代码到 GitHub：
   ```bash
   git add .
   git commit -m "Initial commit"
   git push -u origin main
   ```

---

## 📁 推送内容

### 推送的项目结构
```
nexus-platform/
├── .gitignore
├── README.md
├── plan.md
├── PROGRESS.md
├── DEPLOYMENT.md
├── mock-sdk/
│   ├── src/
│   ├── tests/
│   ├── dist/
│   ├── package.json
│   ├── vite.config.ts
│   ├── vitest.config.ts
│   └── README.md
├── android-client/
│   ├── app/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── README.md
├── ios-client/
│   ├── NexusPlatform/
│   ├── Package.swift
│   └── README.md
├── backend/
│   ├── src/main/
│   ├── pom.xml
│   └── README.md
├── dev-portal/
│   ├── src/
│   ├── package.json
│   ├── vite.config.js
│   └── README.md
└── docker-compose/
    ├── docker-compose.yml
    ├── docker-compose-prod.yml
    ├── init-nakama.sh
    ├── akama.yml
    ├── prometheus.yml
    ├── monitoring-config.json
    ├── backup.sh
    └── README.md
```

### 提交信息建议
```
Initial commit: 项目初始化

feat(mock-sdk): 完成 Mock SDK，支持 30+ 个微信小游戏 API

feat(android-client): 完成 Android 原生客户端，支持游戏加载和 API 桥接

feat(ios-client): 完成 iOS 原生客户端，支持游戏加载和 API 桥接

feat(backend): 完成 Java 后端服务，支持游戏上传和分发

feat(dev-portal): 完成 Vue 3 开发者后台，包含完整的用户认证、游戏管理、审核系统

feat(docker-compose): 完成 Docker 部署配置，支持 PostgreSQL、Nakama、MinIO、Redis

feat(nakama-integration): 完成 Nakama 社交功能集成，支持云存档、排行榜、好友系统

feat(testing): 完成单元测试和集成测试，配置 Vitest 和测试覆盖

feat(ci-cd): 完成 GitHub Actions CI/CD 流程配置

feat(monitoring): 完成 Prometheus 和 Grafana 监控配置

feat(backup): 完成自动备份脚本和备份策略

docs: 完整的项目文档和部署指南
```

---

## 🔧 常见问题

### 1. 文件太大
GitHub 有单个文件大小限制（100MB），如果文件过大：
- 使用 Git LFS：https://git-lfs.github.com/
- 或者压缩大文件后再上传

### 2. 敏感信息
确保不要提交以下敏感信息：
- `.env` 文件
- `application.yml` 中的密码和密钥
- `akama.yml` 中的密钥
- 任何 API 密钥或 token

### 3. .gitignore 配置
`.gitignore` 文件已经配置，会忽略：
- `node_modules/` - 依赖目录
- `dist/` - 构建产物
- `.DS_Store` - macOS 系统文件
- `*.log` - 日志文件
- `.env` - 环境变量文件

---

## 📊 推送后验证

### 检查仓库
1. 访问你的 GitHub 仓库：https://github.com/your-username/nexus-platform
2. 检查所有文件是否正确上传
3. 检查 README.md 是否正确显示

### 验证本地部署
1. 按照本地部署指南启动服务
2. 访问开发者后台：http://localhost:5173
3. 测试游戏上传功能
4. 检查监控面板：http://localhost:3000

---

## 🎯 后续操作

### 持续开发
1. 在 GitHub 上创建新的分支进行功能开发：
   ```bash
   git checkout -b feature/new-feature
   ```

2. 提交代码：
   ```bash
   git add .
   git commit -m "feat: add new feature"
   git push -u origin feature/new-feature
   ```

3. 合并到主分支：
   ```bash
   git checkout main
   git merge feature/new-feature
   git push -u origin main
   ```

### 版本发布
1. 创建版本标签：
   ```bash
   git tag -a v1.0.0 -m "Release version 1.0.0"
   git push -u origin v1.0.0
   ```

2. 创建 GitHub Release：
   - 在 GitHub 网页上创建 Release
   - 上传构建产物（Mock SDK、Android APK、iOS IPA 等）
   - 编写 Release Notes

---

## 📚 相关资源

### GitHub 文档
- [创建仓库](https://docs.github.com/en/repositories)
- [Git 基础](https://git-scm.com/docs)
- [GitHub CLI](https://cli.github.com/manual/)
- [Git LFS](https://git-lfs.github.com/)

### 项目文档
- [plan.md](../plan.md) - 完整开发计划
- [PROGRESS.md](../PROGRESS.md) - 项目进度报告
- [DEPLOYMENT.md](../DEPLOYMENT.md) - 本地部署指南
- 各模块 README.md - 详细文档

---

## 💡 提示

1. **首次推送**可能需要较长时间，请耐心等待
2. **网络问题**如果推送失败，请检查网络连接
3. **权限问题**如果遇到权限错误，请检查 token 是否正确
4. **大文件**如果文件过大，考虑使用 Git LFS

---

## 🎉 完成上传后

上传完成后，你将拥有一个完整的 GitHub 仓库，包含：
- 完整的源代码
- 详细的文档
- CI/CD 配置
- Docker 部署配置
- 测试用例

**祝上传顺利！🚀**
