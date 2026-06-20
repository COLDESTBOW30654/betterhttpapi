# BetterHTTPAPI

**PaperMC 服务器 HTTP API 管理插件** —— 通过 RESTful API 远程控制 Minecraft 服务器。

## 功能特性

- 9 个 HTTP API 端点，覆盖常用管理操作
- API Key 认证 + Host 白名单双重安全保护
- 每个端点可独立启用/禁用
- 可配置的 CORS 跨域支持
- API 调用日志记录
- 所有 Bukkit 操作在主线程安全执行

## 快速开始

### 1. 安装

将 `BetterHTTPAPI-1.0.0.jar` 放入服务器的 `plugins/` 目录，重启服务器。

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
  unban: true
  status: true
  players: true
  kick: true
  broadcast: true
  stop: true
  restart: true
```

修改配置后可在游戏内执行 `/reload confirm` 或重启服务器使其生效。

### 3. 验证

```bash
curl -H "X-API-Key: your-secret-key-here" http://localhost:8080/api/status
```

## API 接口文档

**通用规则：**
- 所有请求必须携带 Header `X-API-Key: <你的密钥>`
- POST 请求使用 `Content-Type: application/json`
- 成功响应包含 `"success": true`

---

### GET /api/status — 服务器状态

```bash
curl -H "X-API-Key: your-key" http://localhost:8080/api/status
```

**响应示例：**
```json
{
  "version": "1.20.4",
  "onlinePlayers": 5,
  "maxPlayers": 20,
  "tps": { "tps1m": 20.0, "tps5m": 19.95, "tps15m": 19.98 },
  "memory": { "usedMB": 2048, "maxMB": 4096, "totalMB": 3072, "freeMB": 1024 }
}
```

---

### GET /api/players — 在线玩家

```bash
curl -H "X-API-Key: your-key" http://localhost:8080/api/players
```

**响应示例：**
```json
{
  "success": true,
  "count": 2,
  "players": [
    {
      "name": "Steve",
      "uuid": "8667ba71-b85a-4004-af54-457a9734eed7",
      "world": "world",
      "x": 128.5, "y": 64.0, "z": -32.2,
      "health": 20.0,
      "gameMode": "SURVIVAL",
      "ipAddress": "192.168.1.100"
    }
  ]
}
```

---

### POST /api/execute — 执行控制台命令

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-key" \
  -d '{"command":"say 大家好！"}' \
  http://localhost:8080/api/execute
```

**请求体：**
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| command | string | 是 | 控制台命令（如 `say`, `gamemode`, `give` 等） |

---

### POST /api/ban — 封禁玩家

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-key" \
  -d '{"player":"Steve","reason":"违规行为"}' \
  http://localhost:8080/api/ban
```

**请求体：**
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| player | string | 是 | 玩家名称 |
| reason | string | 否 | 封禁原因（默认 "Banned via API"） |

---

### POST /api/unban — 解封玩家

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-key" \
  -d '{"player":"Steve"}' \
  http://localhost:8080/api/unban
```

---

### POST /api/kick — 踢出玩家

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-key" \
  -d '{"player":"Steve","reason":"请重新登录"}' \
  http://localhost:8080/api/kick
```

---

### POST /api/broadcast — 广播消息

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-key" \
  -d '{"message":"服务器将在 5 分钟后重启！"}' \
  http://localhost:8080/api/broadcast
```

---

### POST /api/stop — 关闭服务器

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-key" \
  -d '{"message":"服务器即将关闭维护"}' \
  http://localhost:8080/api/stop
```

> 如果提供 `message`，会先向全体玩家广播后再关闭。

---

### POST /api/restart — 重启服务器

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-key" \
  -d '{"message":"服务器即将重启，请稍候"}' \
  http://localhost:8080/api/restart
```

> 需要 Paper/Spigot 服务端支持。

---

## 错误码说明

| HTTP 状态码 | 含义 |
|-------------|------|
| 200 | 请求成功 |
| 400 | 请求体 JSON 格式错误或缺少必填字段 |
| 401 | API Key 缺失或不正确 |
| 403 | Host 不在白名单中 |
| 404 | 目标玩家不在线（/api/kick 时） |
| 503 | 该端点已被禁用 |

所有错误响应格式：
```json
{ "success": false, "message": "错误描述" }
```

## 安全性建议

1. **务必修改默认 API Key**：将 `change-me-default` 改为强密码。
2. **配置 Host 白名单**：限制只有可信任的域名/IP 能访问 API。
3. **使用 HTTPS 反向代理**：在生产环境中通过 Nginx/Caddy 等提供 SSL 加密。
4. **按需禁用端点**：不需要的功能在 `config.yml` 中设为 `false`。

## 构建

```bash
./gradlew clean shadowJar
```

输出文件：`build/libs/BetterHTTPAPI-1.0.0.jar`

## 技术栈

- Java 21 + Gradle 8.x
- Eclipse Jetty 11（嵌入式 HTTP 服务）
- Jackson 2.15（JSON 序列化）
- Paper API 1.20.4

## 作者

**白鹿原嚒** — [https://blog.blym.top](https://blog.blym.top)
