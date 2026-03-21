# Backend

## 背景

`backend` 是 Nexus Platform 的 Java 后端服务，负责平台核心业务接口与外部服务集成。

## 功能

主要提供以下能力：

- 用户注册与登录
- 游戏上传、查询、审核
- MinIO 文件存储接入
- Nakama 能力接入
- PostgreSQL / Redis 配置与数据访问

## 目录

```text
backend/
├─ src/main/java/com/nexus/platform/
│  ├─ controller/   控制器
│  ├─ service/      业务服务
│  ├─ repository/   数据访问
│  ├─ entity/       实体定义
│  ├─ config/       配置类
│  └─ dto/          数据传输对象
├─ src/main/resources/
│  └─ application.yml
├─ src/test/
└─ pom.xml
```

## 依赖

- Java 17+
- Maven 3.8+
- PostgreSQL
- Redis
- MinIO
- Nakama（如启用社交能力）

## 启动

```bash
mvn -f backend/pom.xml spring-boot:run
```

## 构建

### 构建

```bash
npm run build:backend
```

### 测试

```bash
npm run test:backend
```

## 联调

后端联调推荐搭配以下服务一起启动：

- PostgreSQL
- Redis
- MinIO
- Nakama

如果使用仓库自带基础设施：

```bash
npm run infra:up
mvn -f backend/pom.xml spring-boot:run
```

如果修改后端接口，且开发者后台会调用，必须同步更新：

- `contracts/backend-api.json`

## 已知问题

- 当前默认配置偏向本地开发环境，不适合直接用于生产。
- 部分集成依赖在未启动对应服务时会影响联调。
- 仓库正在做平台收束，接口路径变更必须同步更新契约。
