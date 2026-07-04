# Changelog

本文档记录 BetterHTTPAPI 项目的所有重要变更。

格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。

## [1.1.0] - 2026-07-04

### Added
- 新增 `PluginAPIManager` 统一管理第三方插件 API
- 接入 **LuckPerms API**：权限检查、组管理、权限修改、前缀/后缀设置
- 接入 **Multiverse-Core API**：世界列表、创建/删除/克隆、游戏模式/难度/边界/重生点设置
- 接入 **PlayerTitle API**：称号获取/授予/创建/删除/撤销、设置当前称号
- 接入 **SkinsRestorer API**：皮肤设置/重置/强制应用/缓存清除
- 接入 **Residence API**：领地检查/创建/删除、成员管理、权限标志、所有权转让
- 接入 **AuthMe API**：注册/登录检查、账户注册/注销/强制登录、密码修改/邮箱设置
- 新增 30+ HTTP 管理端点（`/api/*/admin/` 路径）
- 新增 `LuckPermsService`、`MultiverseService`、`PlayerTitleService`、`SkinsRestorerService`、`ResidenceService`、`AuthMeService` 服务层类

### Fixed
- 修复 shadowJar 打包时依赖文件重复导致的 `IllegalStateException`
- 修复 Maven 依赖坐标和仓库配置错误（SkinsRestorer、Residence、AuthMe）

### Changed
- 更新 `plugin.yml` 添加所有第三方插件的 `softdepend` 声明
- 扩展 `config.yml` 添加所有新端点的启用/禁用开关
- 更新项目文档（README.md、API.md、AGENTS.md）

## [1.0.0] - 2026-06-20

### Added
- 初始版本，包含基础 HTTP API
- RESTful API 端点：execute、ban、unban、status、players、kick、broadcast、stop、restart
- Jetty 11 嵌入式 HTTP 服务器
- API Key 认证（Header `X-API-Key`）
- Host 白名单安全校验
- CORS 跨域支持
- 可配置的 API 调用日志记录
- 每个端点独立的启用/禁用开关
- 所有 Bukkit 操作在主线程安全执行
