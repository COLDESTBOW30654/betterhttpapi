# BetterHTTPAPI 接口文档

版本：**v1.1.0** | 更新时间：2026-07-04

## 概述

- **Base URL**：`http://localhost:8080`（默认端口，可在 `config.yml` 中修改）
- **认证方式**：所有请求必须在 Header 中携带 `X-API-Key: <你的密钥>`
- **请求格式**：POST 请求使用 `Content-Type: application/json`
- **响应格式**：JSON，统一结构如下：

**成功响应：**
```json
{ "success": true, "data": { ... } }
```

**错误响应：**
```json
{ "success": false, "error": "错误描述" }
```

---

## 1. 服务器管理

### GET /api/status — 获取服务器状态

**描述**：获取服务器版本、TPS、内存使用、在线人数等信息。

**请求示例：**
```bash
curl -H "X-API-Key: your-key" http://localhost:8080/api/status
```

**成功响应：**
```json
{
  "success": true,
  "data": {
    "version": "1.20.4",
    "onlinePlayers": 5,
    "maxPlayers": 20,
    "tps": { "tps1m": 20.0, "tps5m": 19.95, "tps15m": 19.98 },
    "memory": { "usedMB": 2048, "maxMB": 4096, "totalMB": 3072, "freeMB": 1024 }
  }
}
```

---

### GET /api/players — 获取在线玩家

**描述**：获取所有在线玩家的详细信息，包括 UUID、坐标、血量、游戏模式、IP 等。

**请求示例：**
```bash
curl -H "X-API-Key: your-key" http://localhost:8080/api/players
```

**成功响应：**
```json
{
  "success": true,
  "data": {
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
}
```

---

### POST /api/execute — 执行控制台命令

**描述**：以控制台身份执行任意命令。

**请求参数（JSON body）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| command | string | 是 | 控制台命令 |

**请求示例：**
```bash
curl -X POST -H "Content-Type: application/json" -H "X-API-Key: your-key" \
     -d '{"command":"say 大家好"}' http://localhost:8080/api/execute
```

**成功响应：**
```json
{ "success": true, "data": { "executed": "say 大家好" } }
```

---

### POST /api/broadcast — 广播消息

**描述**：向全体在线玩家广播消息。

**请求参数（JSON body）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| message | string | 是 | 广播内容 |

**请求示例：**
```bash
curl -X POST -H "Content-Type: application/json" -H "X-API-Key: your-key" \
     -d '{"message":"服务器将在 5 分钟后重启！"}' http://localhost:8080/api/broadcast
```

**成功响应：**
```json
{ "success": true, "data": { "message": "服务器将在 5 分钟后重启！" } }
```

---

### POST /api/stop — 关闭服务器

**描述**：远程关闭服务器。可先广播消息再关闭。

**请求参数（JSON body）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| message | string | 否 | 关闭前广播的消息 |

**请求示例：**
```bash
curl -X POST -H "Content-Type: application/json" -H "X-API-Key: your-key" \
     -d '{"message":"服务器即将关闭维护"}' http://localhost:8080/api/stop
```

---

### POST /api/restart — 重启服务器

**描述**：远程重启服务器（需要 Paper/Spigot 服务端支持）。

**请求参数（JSON body）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| message | string | 否 | 重启前广播的消息 |

**请求示例：**
```bash
curl -X POST -H "Content-Type: application/json" -H "X-API-Key: your-key" \
     -d '{"message":"服务器即将重启"}' http://localhost:8080/api/restart
```

---

## 2. 玩家管理

### POST /api/ban — 封禁玩家

**描述**：封禁指定玩家（支持原因）。

**请求参数（JSON body）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| player | string | 是 | 玩家名称 |
| reason | string | 否 | 封禁原因，默认 "Banned via API" |

**请求示例：**
```bash
curl -X POST -H "Content-Type: application/json" -H "X-API-Key: your-key" \
     -d '{"player":"Steve","reason":"违规行为"}' http://localhost:8080/api/ban
```

**成功响应：**
```json
{ "success": true, "data": { "player": "Steve", "reason": "违规行为" } }
```

---

### POST /api/unban — 解封玩家

**描述**：解封已封禁的玩家。

**请求参数（JSON body）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| player | string | 是 | 玩家名称 |

**请求示例：**
```bash
curl -X POST -H "Content-Type: application/json" -H "X-API-Key: your-key" \
     -d '{"player":"Steve"}' http://localhost:8080/api/unban
```

---

### POST /api/kick — 踢出玩家

**描述**：踢出指定在线玩家（支持原因）。

**请求参数（JSON body）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| player | string | 是 | 玩家名称（必须在线） |
| reason | string | 否 | 踢出原因 |

**请求示例：**
```bash
curl -X POST -H "Content-Type: application/json" -H "X-API-Key: your-key" \
     -d '{"player":"Steve","reason":"请重新登录"}' http://localhost:8080/api/kick
```

**错误响应（玩家不在线）：**
```json
{ "success": false, "error": "玩家不在线" }
```

---

## 3. LuckPerms 权限管理

> 需要安装 LuckPerms 插件（v5.4+）。

### POST /api/luckperms/check — 检查玩家权限

**描述**：检查指定在线玩家的权限。

**请求参数（JSON body）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| player | string | 是 | 玩家名称 |
| permission | string | 是 | 权限节点 |

**请求示例：**
```bash
curl -X POST -H "Content-Type: application/json" -H "X-API-Key: your-key" \
     -d '{"player":"Steve","permission":"minecraft.command.gamemode"}' \
     http://localhost:8080/api/luckperms/check
```

**成功响应：**
```json
{ "success": true, "data": { "player": "Steve", "permission": "minecraft.command.gamemode", "hasPermission": true } }
```

---

### GET /api/luckperms/groups — 获取玩家权限组

**描述**：获取指定玩家的权限组列表。

**请求参数（Query）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| player | string | 是 | 玩家名称 |

**请求示例：**
```bash
curl -H "X-API-Key: your-key" "http://localhost:8080/api/luckperms/groups?player=Steve"
```

**成功响应：**
```json
{ "success": true, "data": { "player": "Steve", "groups": ["default", "vip"] } }
```

---

### LuckPerms 管理端点（/api/luckperms/admin/）

> 以下端点为高级管理操作，需要对 LuckPerms 有管理权限。

| 端点 | 方法 | 功能 | 请求参数 |
|------|------|------|----------|
| `/api/luckperms/admin/group/create` | POST | 创建权限组 | `{ "group": "组名" }` |
| `/api/luckperms/admin/group/delete` | POST | 删除权限组 | `{ "group": "组名" }` |
| `/api/luckperms/admin/group/setpermission` | POST | 设置组权限 | `{ "group": "组名", "permission": "节点", "value": true/false }` |
| `/api/luckperms/admin/player/addgroup` | POST | 将玩家加入权限组 | `{ "player": "玩家名", "group": "组名" }` |
| `/api/luckperms/admin/player/removegroup` | POST | 将玩家移出权限组 | `{ "player": "玩家名", "group": "组名" }` |
| `/api/luckperms/admin/player/addpermission` | POST | 添加玩家权限节点 | `{ "player": "玩家名", "permission": "节点" }` |
| `/api/luckperms/admin/player/removepermission` | POST | 移除玩家权限节点 | `{ "player": "玩家名", "permission": "节点" }` |
| `/api/luckperms/admin/player/setprefix` | POST | 设置玩家前缀 | `{ "player": "玩家名", "prefix": "前缀" }` |
| `/api/luckperms/admin/player/setsuffix` | POST | 设置玩家后缀 | `{ "player": "玩家名", "suffix": "后缀" }` |

**示例 —— 创建权限组：**
```bash
curl -X POST -H "Content-Type: application/json" -H "X-API-Key: your-key" \
     -d '{"group":"admin"}' http://localhost:8080/api/luckperms/admin/group/create
```

**示例 —— 将玩家加入权限组：**
```bash
curl -X POST -H "Content-Type: application/json" -H "X-API-Key: your-key" \
     -d '{"player":"Steve","group":"admin"}' \
     http://localhost:8080/api/luckperms/admin/player/addgroup
```

---

## 4. Multiverse-Core 世界管理

> 需要安装 Multiverse-Core 插件（v4.3.0+）。

### GET /api/multiverse/worlds — 获取世界列表

**描述**：获取所有世界名称。

**请求示例：**
```bash
curl -H "X-API-Key: your-key" http://localhost:8080/api/multiverse/worlds
```

**成功响应：**
```json
{ "success": true, "data": { "worlds": ["world", "world_nether", "world_the_end"] } }
```

---

### POST /api/multiverse/world/create — 创建世界

**描述**：创建新世界。

**请求参数（JSON body）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 世界名称 |
| environment | string | 否 | 环境类型：NORMAL（默认）、NETHER、THE_END |
| seed | long | 否 | 世界种子 |
| gamemode | string | 否 | 默认游戏模式：SURVIVAL、CREATIVE、ADVENTURE、SPECTATOR |

**请求示例：**
```bash
curl -X POST -H "Content-Type: application/json" -H "X-API-Key: your-key" \
     -d '{"name":"test_world","environment":"NORMAL","seed":12345}' \
     http://localhost:8080/api/multiverse/world/create
```

**成功响应：**
```json
{ "success": true, "data": { "world": "test_world", "environment": "NORMAL" } }
```

---

### Multiverse-Core 管理端点（/api/multiverse/admin/）

| 端点 | 方法 | 功能 | 请求参数 |
|------|------|------|----------|
| `/api/multiverse/admin/world/clone` | POST | 克隆世界 | `{ "source": "源世界", "target": "新世界名" }` |
| `/api/multiverse/admin/world/gamemode` | POST | 设置世界游戏模式 | `{ "world": "世界名", "gamemode": "SURVIVAL/CREATIVE/..." }` |
| `/api/multiverse/admin/world/difficulty` | POST | 设置世界难度 | `{ "world": "世界名", "difficulty": "PEACEFUL/EASY/NORMAL/HARD" }` |
| `/api/multiverse/admin/world/border` | POST | 设置世界边界 | `{ "world": "世界名", "size": 500, "centerX": 0, "centerZ": 0 }` |
| `/api/multiverse/admin/world/spawn` | POST | 设置世界重生点 | `{ "world": "世界名", "x": 0, "y": 70, "z": 0 }` |

---

## 5. PlayerTitle 称号管理

> 需要安装 PlayerTitle 插件（v4.8.0+）。

### GET /api/playertitle/get — 获取玩家当前称号

**描述**：获取玩家当前使用的称号。

**请求参数（Query）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| player | string | 是 | 玩家名称 |

**请求示例：**
```bash
curl -H "X-API-Key: your-key" "http://localhost:8080/api/playertitle/get?player=Steve"
```

**成功响应：**
```json
{ "success": true, "data": { "player": "Steve", "title": "VIP" } }
```

---

### POST /api/playertitle/grant — 授予玩家称号

**描述**：授予指定玩家一个称号。

**请求参数（JSON body）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| player | string | 是 | 玩家名称 |
| titleId | string | 是 | 称号 ID |

**请求示例：**
```bash
curl -X POST -H "Content-Type: application/json" -H "X-API-Key: your-key" \
     -d '{"player":"Steve","titleId":"vip"}' http://localhost:8080/api/playertitle/grant
```

**成功响应：**
```json
{ "success": true, "data": { "player": "Steve", "titleId": "vip" } }
```

---

### PlayerTitle 管理端点（/api/playertitle/admin/）

| 端点 | 方法 | 功能 | 请求参数 |
|------|------|------|----------|
| `/api/playertitle/admin/title/create` | POST | 创建称号 | `{ "id": "vip", "display": "&6[VIP]", "permission": "title.vip" }` |
| `/api/playertitle/admin/title/delete` | POST | 删除称号 | `{ "id": "称号ID" }` |
| `/api/playertitle/admin/title/set` | POST | 设置玩家当前称号 | `{ "player": "玩家名", "titleId": "称号ID" }` |
| `/api/playertitle/admin/title/revokeall` | POST | 移除所有玩家的指定称号 | `{ "id": "称号ID" }` |

---

## 6. SkinsRestorer 皮肤管理

> 需要安装 SkinsRestorer 插件（v14.x / v15.x）。

### POST /api/skins/set — 设置玩家皮肤

**描述**：为玩家设置皮肤（通过皮肤名称或 URL）。

**请求参数（JSON body）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| player | string | 是 | 玩家名称 |
| skinName | string | 否 | 皮肤名称（如 "Notch"） |
| skinUrl | string | 否 | 皮肤 URL（需插件支持） |

**请求示例：**
```bash
# 通过皮肤名称
curl -X POST -H "Content-Type: application/json" -H "X-API-Key: your-key" \
     -d '{"player":"Steve","skinName":"Notch"}' http://localhost:8080/api/skins/set

# 通过皮肤 URL
curl -X POST -H "Content-Type: application/json" -H "X-API-Key: your-key" \
     -d '{"player":"Steve","skinUrl":"https://example.com/skin.png"}' \
     http://localhost:8080/api/skins/set
```

**成功响应：**
```json
{ "success": true, "data": { "player": "Steve", "skinName": "Notch" } }
```

---

### SkinsRestorer 管理端点（/api/skins/admin/）

| 端点 | 方法 | 功能 | 请求参数 |
|------|------|------|----------|
| `/api/skins/admin/setforce` | POST | 强制设置皮肤（支持离线玩家） | `{ "player": "玩家名", "skin": "皮肤名" }` |
| `/api/skins/admin/clearcache` | POST | 清除玩家皮肤缓存 | `{ "player": "玩家名" }` |
| `/api/skins/admin/apply` | POST | 强制刷新玩家皮肤 | `{ "player": "玩家名" }` |

---

## 7. Residence 领地管理

> 需要安装 Residence 插件（v5.1.4+ / v6.x）。

### POST /api/residence/check — 检查位置是否在领地内

**描述**：检查指定坐标是否属于某个领地。

**请求参数（JSON body）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| world | string | 是 | 世界名称 |
| x | double | 是 | X 坐标 |
| y | double | 是 | Y 坐标 |
| z | double | 是 | Z 坐标 |

**请求示例：**
```bash
curl -X POST -H "Content-Type: application/json" -H "X-API-Key: your-key" \
     -d '{"world":"world","x":100,"y":64,"z":200}' http://localhost:8080/api/residence/check
```

**成功响应：**
```json
{ "success": true, "data": { "inResidence": true, "owner": "Steve", "residence": "MyHome" } }
```

---

### Residence 管理端点（/api/residence/admin/）

| 端点 | 方法 | 功能 | 请求参数 |
|------|------|------|----------|
| `/api/residence/admin/create` | POST | 创建领地 | `{ "name": "领地名", "owner": "玩家名", "loc1": {...}, "loc2": {...} }` |
| `/api/residence/admin/delete` | POST | 删除领地 | `{ "name": "领地名" }` |
| `/api/residence/admin/addmember` | POST | 添加领地成员 | `{ "name": "领地名", "player": "玩家名" }` |
| `/api/residence/admin/removemember` | POST | 移除领地成员 | `{ "name": "领地名", "player": "玩家名" }` |
| `/api/residence/admin/setflag` | POST | 设置领地权限标志 | `{ "name": "领地名", "flag": "build/destroy/use", "value": true/false }` |
| `/api/residence/admin/transfer` | POST | 转让领地所有权 | `{ "name": "领地名", "newOwner": "新所有者" }` |

**示例 —— 创建领地：**
```bash
curl -X POST -H "Content-Type: application/json" -H "X-API-Key: your-key" \
     -d '{"name":"MyHome","owner":"Steve","loc1":{"world":"world","x":100,"y":64,"z":100},"loc2":{"world":"world","x":200,"y":80,"z":200}}' \
     http://localhost:8080/api/residence/admin/create
```

---

## 8. AuthMe 认证管理

> 需要安装 AuthMe 插件（v5.6.0+）。

### GET /api/authme/registered — 检查玩家是否已注册

**描述**：查询玩家是否已在 AuthMe 中注册过账户。

**请求参数（Query）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| player | string | 是 | 玩家名称 |

**请求示例：**
```bash
curl -H "X-API-Key: your-key" "http://localhost:8080/api/authme/registered?player=Steve"
```

**成功响应：**
```json
{ "success": true, "data": { "player": "Steve", "registered": true } }
```

---

### GET /api/authme/authenticated — 检查玩家是否已登录

**描述**：查询在线玩家是否已通过 AuthMe 认证登录。

**请求参数（Query）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| player | string | 是 | 玩家名称（必须在线） |

**请求示例：**
```bash
curl -H "X-API-Key: your-key" "http://localhost:8080/api/authme/authenticated?player=Steve"
```

**成功响应：**
```json
{ "success": true, "data": { "player": "Steve", "authenticated": true } }
```

---

### POST /api/authme/register — 注册玩家账户

**描述**：为玩家注册 AuthMe 账户（管理员操作）。

**请求参数（JSON body）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| player | string | 是 | 玩家名称 |
| password | string | 是 | 密码 |

**请求示例：**
```bash
curl -X POST -H "Content-Type: application/json" -H "X-API-Key: your-key" \
     -d '{"player":"Steve","password":"secret123"}' http://localhost:8080/api/authme/register
```

**成功响应：**
```json
{ "success": true, "data": { "player": "Steve", "registered": true } }
```

---

### POST /api/authme/logout — 强制注销玩家

**描述**：强制注销玩家的登录状态。

**请求参数（JSON body）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| player | string | 是 | 玩家名称（必须在线） |

**请求示例：**
```bash
curl -X POST -H "Content-Type: application/json" -H "X-API-Key: your-key" \
     -d '{"player":"Steve"}' http://localhost:8080/api/authme/logout
```

**成功响应：**
```json
{ "success": true, "data": { "player": "Steve", "loggedOut": true } }
```

---

### AuthMe 管理端点（/api/authme/admin/）

| 端点 | 方法 | 功能 | 请求参数 |
|------|------|------|----------|
| `/api/authme/admin/unregister` | POST | 删除玩家注册账户 | `{ "player": "玩家名" }` |
| `/api/authme/admin/changepassword` | POST | 修改玩家密码 | `{ "player": "玩家名", "password": "新密码" }` |
| `/api/authme/admin/forcelogin` | POST | 强制登录（绕过密码） | `{ "player": "玩家名" }` |
| `/api/authme/admin/email` | POST | 设置玩家邮箱 | `{ "player": "玩家名", "email": "邮箱地址" }` |

**示例 —— 修改密码：**
```bash
curl -X POST -H "Content-Type: application/json" -H "X-API-Key: your-key" \
     -d '{"player":"Steve","password":"newPassword123"}' \
     http://localhost:8080/api/authme/admin/changepassword
```

---

## 端点配置对照表

| 端点路径 | 配置键 | 默认状态 |
|----------|--------|----------|
| `/api/execute` | `endpoints.execute` | true |
| `/api/ban` | `endpoints.ban` | true |
| `/api/unban` | `endpoints.unban` | true |
| `/api/status` | `endpoints.status` | true |
| `/api/players` | `endpoints.players` | true |
| `/api/kick` | `endpoints.kick` | true |
| `/api/broadcast` | `endpoints.broadcast` | true |
| `/api/stop` | `endpoints.stop` | true |
| `/api/restart` | `endpoints.restart` | true |
| LuckPerms 基础端点 | `endpoints.luckperms` | true |
| LuckPerms 管理端点 | `endpoints.luckperms_admin` | true |
| Multiverse 基础端点 | `endpoints.multiverse` | true |
| Multiverse 管理端点 | `endpoints.multiverse_admin` | true |
| PlayerTitle 基础端点 | `endpoints.playertitle` | true |
| PlayerTitle 管理端点 | `endpoints.playertitle_admin` | true |
| SkinsRestorer 端点 | `endpoints.skins` | true |
| SkinsRestorer 管理端点 | `endpoints.skins_admin` | true |
| Residence 端点 | `endpoints.residence` | true |
| Residence 管理端点 | `endpoints.residence_admin` | true |
| AuthMe 端点 | `endpoints.authme_*` | true |
| AuthMe 管理端点 | `endpoints.authme_admin` | true |

---

## 通用错误码

| HTTP 状态码 | 含义 |
|-------------|------|
| 200 | 请求成功 |
| 400 | 请求体 JSON 格式错误或缺少必填字段 |
| 401 | API Key 缺失或不正确 |
| 403 | Host 不在白名单中 |
| 404 | 目标不存在（玩家不在线/权限组不存在/世界不存在等） |
| 500 | 服务器内部错误（插件未加载/操作失败） |
| 503 | 该端点已被禁用 |
