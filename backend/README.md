# Nexus Platform Backend

Nexus 平台的 Java 后端服务，提供游戏上传、分发、用户认证等功能。

## 功能特性

- Spring Boot 3.x 框架
- PostgreSQL 数据库
- Redis 缓存
- MinIO 对象存储
- RESTful API
- 统一响应格式
- 全局异常处理
- 游戏审核系统

## 技术栈

- Java 17
- Spring Boot 3.2.0
- Spring Data JPA
- PostgreSQL
- Redis
- MinIO
- MyBatis
- Lombok

## 项目结构

```
src/main/java/com/nexus/platform/
├── NexusPlatformApplication.java    # 应用入口
├── controller/                     # 控制器层
│   ├── UserController.java          # 用户相关接口
│   └── GameController.java          # 游戏相关接口
├── service/                       # 服务层
│   ├── UserService.java             # 用户服务
│   └── GameService.java             # 游戏服务
├── repository/                    # 数据访问层
│   ├── UserRepository.java          # 用户仓库
│   └── GameRepository.java          # 游戏仓库
├── entity/                        # 实体类
│   ├── User.java                   # 用户实体
│   └── Game.java                   # 游戏实体
├── dto/                           # 数据传输对象
│   └── Result.java                # 统一响应格式
└── config/                        # 配置类
    ├── MinioConfig.java            # MinIO 配置
    └── GlobalExceptionHandler.java  # 全局异常处理
```

## 构建要求

- JDK 17
- Maven 3.6+
- PostgreSQL 14+
- Redis 6+
- MinIO

## 构建步骤

1. 克隆项目
```bash
git clone <repository-url>
cd nexus-platform/backend
```

2. 配置数据库
修改 `application.yml` 中的数据库连接信息。

3. 配置 MinIO
修改 `application.yml` 中的 MinIO 连接信息。

4. 构建项目
```bash
mvn clean package
```

5. 运行应用
```bash
java -jar target/nexus-platform-1.0.0.jar
```

## API 接口

### 用户相关

#### 注册
```
POST /api/v1/user/register
Content-Type: application/json

{
  "username": "test",
  "password": "123456",
  "email": "test@example.com"
}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "username": "test",
    "email": "test@example.com"
  }
}
```

#### 登录
```
POST /api/v1/user/login
Content-Type: application/json

{
  "username": "test",
  "password": "123456"
}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "username": "test",
    "email": "test@example.com"
  }
}
```

### 游戏相关

#### 上传游戏
```
POST /api/v1/game/upload
Content-Type: multipart/form-data

file: <game.zip>
name: "示例游戏"
description: "这是一个示例游戏"
developerId: 1

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "appId": "wx1234567890abcdef",
    "name": "示例游戏",
    "downloadUrl": "/api/v1/game/download/wx1234567890abcdef",
    "status": "PENDING"
  }
}
```

#### 获取游戏列表
```
GET /api/v1/game/list

Response:
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "appId": "wx1234567890abcdef",
      "name": "示例游戏",
      "description": "这是一个示例游戏",
      "downloadUrl": "/api/v1/game/download/wx1234567890abcdef",
      "status": "APPROVED"
    }
  ]
}
```

#### 下载游戏
```
GET /api/v1/game/download/{appId}

Response: Binary file (ZIP)
```

#### 审核游戏
```
POST /api/v1/game/approve/{id}

Response:
{
  "code": 0,
  "message": "success",
  "data": null
}

POST /api/v1/game/reject/{id}

Response:
{
  "code": 0,
  "message": "success",
  "data": null
}
```

## 数据库设计

### users 表
- id: 主键
- username: 用户名（唯一）
- password: 密码（加密）
- email: 邮箱
- phone: 手机号
- created_at: 创建时间
- updated_at: 更新时间

### games 表
- id: 主键
- app_id: AppID（唯一）
- name: 游戏名称
- description: 游戏描述
- icon_url: 图标 URL
- download_url: 下载 URL
- version: 版本号
- md5: MD5 校验值
- status: 状态（DRAFT/PENDING/APPROVED/REJECTED）
- developer_id: 开发者 ID
- created_at: 创建时间
- updated_at: 更新时间

## 配置说明

### application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/nexus_platform
    username: nexus
    password: nexus_password
  data:
    redis:
      host: localhost
      port: 6379

minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: nexus-games

server:
  port: 8080
  servlet:
    context-path: /api/v1
```

## 安全性

- 密码 BCrypt 加密
- MD5 文件校验
- HTTPS 通信
- JWT Token 认证（待实现）

## 性能优化

- Redis 缓存
- 数据库索引
- 分页查询
- 异步处理

## 故障排查

### 数据库连接失败

1. 检查 PostgreSQL 是否运行
2. 检查连接信息是否正确
3. 检查防火墙设置

### MinIO 连接失败

1. 检查 MinIO 是否运行
2. 检查连接信息是否正确
3. 检查 bucket 是否存在

### 游戏上传失败

1. 检查文件大小限制
2. 检查文件格式
3. 查看服务器日志

## License

MIT
