# BetterHTTPAPI

**PaperMC 服务器 HTTP API 管理插件** —— 通过 RESTful API 远程控制 Minecraft 服务器。

当前版本：**v1.1.0** | 作者：白鹿原嚒 | 博客：[https://blog.blym.top](https://blog.blym.top)

## 功能特性

### 基础管理
- **执行命令** (`/api/execute`) —— 远程执行任意控制台命令
- **玩家封禁/解封** (`/api/ban`, `/api/unban`) —— 带原因封禁/解封玩家
- **踢出玩家** (`/api/kick`) —— 带原因踢出在线玩家
- **广播消息** (`/api/broadcast`) —— 向全体玩家发送消息
- **服务器状态** (`/api/status`) —— 获取 TPS、内存、在线人数等信息
- **在线玩家列表** (`/api/players`) —— 获取在线玩家的详细信息
- **关闭/重启服务器** (`/api/stop`, `/api/restart`) —— 远程管理服务器进程

### 第三方插件集成（需对应插件安装）

| 插件 | 功能 | 基础端点 | 管理端点 |
|------|------|---------|---------|
| **LuckPerms** | 权限检查、组管理、权限修改、前缀/后缀 | `/api/luckperms/...` | `/api/luckperms/admin/...` |
| **Multiverse-Core** | 世界列表、创建/克隆、游戏模式/难度设置 | `/api/multiverse/...` | `/api/multiverse/admin/...` |
| **PlayerTitle** | 称号获取/授予/创建/删除 | `/api/playertitle/...` | `/api/playertitle/admin/...` |
| **SkinsRestorer** | 皮肤设置/重置/强制应用 | `/api/skins/...` | `/api/skins/admin/...` |
| **Residence** | 领地检查/创建/删除、成员管理、权限标志 | `/api/residence/...` | `/api/residence/admin/...` |
| **AuthMe** | 注册/登录检查、账户管理、密码修改 | `/api/authme/...` | `/api/authme/admin/...` |

### 安全机制
- API Key 认证（Header `X-API-Key`）
- Host 白名单（防止外部未授权访问）
- 每个端点可独立启用/禁用
- CORS 跨域支持
- 可选的 API 调用日志记录

## 快速开始

### 1. 安装

将 `BetterHTTPAPI-1.1.0.jar` 放入服务器的 `plugins/` 目录，重启服务器。

### 2. 配置

编辑 `plugins/BetterHTTPAPI/config.yml`：

```yaml
# 修改默认 API Key（生产环境必须修改！）
api-key: "your-secret-key-here"

# 允许访问的域名/IP
allowed-hosts:
  - "localhost"
  - "127.0.0.1"
  - "your-domain.com"

# HTTP 监听端口
port: 8080

# 功能开关（按需启用/禁用）
endpoints:
  execute: true
  ban: true
  # ... 更多端点开关见 config.yml
```

修改配置后可在游戏内执行 `/reload confirm` 或重启服务器使其生效。

### 3. 验证

```bash
# 获取服务器状态
curl -H "X-API-Key: your-secret-key" http://localhost:8080/api/status

# 执行命令
curl -X POST -H "Content-Type: application/json" -H "X-API-Key: your-secret-key" \
     -d '{"command":"say Hello"}' http://localhost:8080/api/execute

# 检查玩家权限
curl -X POST -H "Content-Type: application/json" -H "X-API-Key: your-secret-key" \
     -d '{"player":"Steve","permission":"minecraft.command.gamemode"}' \
     http://localhost:8080/api/luckperms/check

# 获取世界列表
curl -H "X-API-Key: your-secret-key" http://localhost:8080/api/multiverse/worlds

# 检查玩家注册状态
curl -H "X-API-Key: your-secret-key" \
     "http://localhost:8080/api/authme/registered?player=Steve"
```

## 依赖插件版本要求

| 插件 | 最低版本 | 用途 |
|------|---------|------|
| LuckPerms | 5.4+ | 权限管理 |
| Multiverse-Core | 4.3.0+ | 多世界管理 |
| PlayerTitle | 4.8.0+ | 称号管理 |
| SkinsRestorer | 14.x / 15.x | 皮肤管理 |
| Residence | 5.1.4+ / 6.x | 领地管理 |
| AuthMe | 5.6.0+ | 认证管理 |

> 以上插件均为可选依赖（softdepend），未安装时对应端点将返回插件不可用的错误。

## 错误码说明

| HTTP 状态码 | 含义 |
|-------------|------|
| 200 | 请求成功 |
| 400 | 请求体 JSON 格式错误或缺少必填字段 |
| 401 | API Key 缺失或不正确 |
| 403 | Host 不在白名单中 |
| 404 | 目标不存在（玩家不在线 / 权限组不存在等） |
| 500 | 服务器内部错误（插件未加载或操作失败） |
| 503 | 该端点已被禁用 |

所有错误响应格式：

```json
{ "success": false, "error": "错误描述" }
```

## 安全性建议

1. **务必修改默认 API Key**：将 `change-me-default` 改为强密码。
2. **配置 Host 白名单**：限制只有可信任的域名/IP 能访问 API。
3. **使用 HTTPS 反向代理**：在生产环境中通过 Nginx/Caddy 等提供 SSL 加密。
4. **按需禁用端点**：不需要的功能在 `config.yml` 中设为 `false`。

## 常见问题

### 插件加载失败？
检查依赖插件是否已安装且版本匹配。查看服务器日志获取详细错误信息。

### 返回 401 错误？
确认请求 Header 中携带了正确的 `X-API-Key`，且与 `config.yml` 中的 `api-key` 一致。

### 端点返回 503？
检查 `config.yml` 中对应端点是否已启用（设为 `true`）。

### 第三方插件端点返回错误？
确认对应插件已安装并正常加载。使用 `/plugins` 命令查看插件状态。

## 构建与开发

```bash
# 构建（生成胖 JAR）
./gradlew shadowJar

# 清理构建
./gradlew clean

# 输出文件
# build/libs/BetterHTTPAPI-1.1.0.jar
```

## 技术栈

- Java 21 + Gradle 8.x（Kotlin DSL）
- Eclipse Jetty 11（嵌入式 HTTP 服务）
- Jackson 2.15（JSON 序列化）
- Paper API 1.20.4

## 更新日志

### v1.1.0 (2026-07-04)
- 新增 PluginAPIManager 统一管理第三方插件 API
- 接入 LuckPerms、Multiverse-Core、PlayerTitle、SkinsRestorer、Residence、AuthMe 六个插件
- 新增 30+ HTTP 管理端点
- 修复 shadowJar 打包依赖文件重复问题
- 完善文档结构

### v1.0.0 (2026-06-20)
- 初始版本：新增基础 HTTP API（execute、ban、unban、status、players、kick、broadcast、stop、restart）

## 注意

本项目使用 AI 开发，如有抵触请勿使用，本项目安全性有待考量。

## 作者

**白鹿原嚒** — [https://blog.blym.top](https://blog.blym.top)
