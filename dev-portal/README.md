# Nexus Platform 开发者后台

Nexus 平台的 Vue 3 开发者后台，提供游戏上传、管理、审核等功能。

## 功能特性

- Vue 3 + Composition API
- Element Plus UI 组件库
- Pinia 状态管理
- Vue Router 路由管理
- Axios 网络请求
- 响应式设计
- 用户认证系统
- 游戏管理
- 审核系统

## 技术栈

- Vue 3.x
- Element Plus
- Pinia
- Vue Router
- Axios
- Vite

## 项目结构

```
src/
├── api/                       # API 接口
│   ├── request.js            # Axios 配置
│   └── index.js              # API 方法
├── views/                     # 页面组件
│   ├── Login.vue              # 登录页面
│   ├── Register.vue           # 注册页面
│   ├── Dashboard.vue          # 仪表板
│   ├── Games.vue              # 游戏列表
│   ├── GameUpload.vue         # 游戏上传
│   └── Audit.vue              # 审核页面
├── stores/                    # 状态管理
│   └── user.js               # 用户状态
├── router/                    # 路由配置
│   └── index.js              # 路由定义
├── utils/                     # 工具函数
├── composables/               # 组合式函数
├── App.vue                    # 根组件
└── main.js                    # 入口文件
```

## 构建要求

- Node.js 16+
- npm 或 yarn

## 构建步骤

1. 克隆项目
```bash
git clone <repository-url>
cd nexus-platform/dev-portal
```

2. 安装依赖
```bash
npm install
```

3. 配置环境变量
创建 `.env` 文件：
```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

4. 启动开发服务器
```bash
npm run dev
```

5. 构建生产版本
```bash
npm run build
```

## 页面说明

### 登录页面 (Login.vue)
- 用户名和密码登录
- 表单验证
- 登录成功后跳转到仪表板

### 注册页面 (Register.vue)
- 用户名、密码、邮箱注册
- 密码确认验证
- 注册成功后跳转到登录页

### 仪表板 (Dashboard.vue)
- 显示游戏统计
- 快速操作入口
- 最近游戏列表

### 游戏列表 (Games.vue)
- 显示用户的所有游戏
- 游戏状态显示
- 游戏查看和删除

### 游戏上传 (GameUpload.vue)
- ZIP 文件上传
- 游戏信息填写
- 上传进度显示

### 审核页面 (Audit.vue)
- 待审核游戏列表
- 审核通过/拒绝操作
- 游戏状态更新

## API 接口

### 用户相关

#### 登录
```javascript
import { login } from '../api'

const res = await login({
  username: 'test',
  password: '123456'
})
```

#### 注册
```javascript
import { register } from '../api'

const res = await register({
  username: 'test',
  password: '123456',
  email: 'test@example.com'
})
```

### 游戏相关

#### 获取游戏列表
```javascript
import { getGameList } from '../api'

const res = await getGameList()
```

#### 上传游戏
```javascript
import { uploadGame } from '../api'

const formData = new FormData()
formData.append('file', file)
formData.append('name', '游戏名称')
formData.append('description', '游戏描述')

const res = await uploadGame(formData)
```

#### 审核游戏
```javascript
import { approveGame, rejectGame } from '../api'

await approveGame(gameId)
await rejectGame(gameId)
```

## 状态管理

### User Store

```javascript
import { useUserStore } from '../stores/user'

const userStore = useUserStore()

userStore.setUser(userData)
userStore.setToken(token)
userStore.logout()
```

## 路由配置

### 路由守卫

- 未登录用户访问需要认证的页面时，重定向到登录页
- 已登录用户访问登录/注册页时，重定向到仪表板

## 样式定制

### Element Plus 主题

可以在 `main.js` 中自定义 Element Plus 主题：

```javascript
import { ElConfigProvider } from 'element-plus'

app.use(ElConfigProvider, {
  size: 'large',
  zIndex: 3000
})
```

## 故障排查

### 登录失败

1. 检查后端服务是否运行
2. 检查 API 地址配置
3. 查看浏览器控制台错误

### 上传失败

1. 检查文件格式（必须是 ZIP）
2. 检查文件大小限制
3. 检查网络连接

### 页面空白

1. 检查路由配置
2. 检查组件导入
3. 查看浏览器控制台错误

## 性能优化

- 路由懒加载
- 组件按需加载
- 图片懒加载
- 防抖和节流

## 安全性

- Token 存储在 localStorage
- 请求头添加 Authorization
- 路由守卫验证登录状态
- XSS 防护

## License

MIT
