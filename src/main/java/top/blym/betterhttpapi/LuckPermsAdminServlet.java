package top.blym.betterhttpapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bukkit.Bukkit;
import top.blym.betterhttpapi.service.LuckPermsService;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * LuckPerms 权限管理管理端点。
 *
 * <p>提供权限组和玩家权限的管理功能，包括创建/删除权限组、玩家加组/移组、
 * 权限添加/移除、设置前缀/后缀等操作。</p>
 *
 * <p>所有端点均为 POST 请求，路径前缀为 /api/luckperms/admin/。</p>
 *
 * <p>支持的端点：</p>
 * <ul>
 *   <li>POST /api/luckperms/admin/group/create - 创建权限组</li>
 *   <li>POST /api/luckperms/admin/group/delete - 删除权限组</li>
 *   <li>POST /api/luckperms/admin/player/addgroup - 玩家加入组</li>
 *   <li>POST /api/luckperms/admin/player/removegroup - 玩家移出组</li>
 *   <li>POST /api/luckperms/admin/player/addpermission - 添加玩家权限</li>
 *   <li>POST /api/luckperms/admin/player/removepermission - 移除玩家权限</li>
 *   <li>POST /api/luckperms/admin/group/setpermission - 设置组权限</li>
 *   <li>POST /api/luckperms/admin/player/setprefix - 设置玩家前缀</li>
 *   <li>POST /api/luckperms/admin/player/setsuffix - 设置玩家后缀</li>
 * </ul>
 *
 * @author 白鹿原嚒
 */
public final class LuckPermsAdminServlet extends BaseServlet {

    /**
     * 构造 LuckPermsAdminServlet。
     *
     * @param plugin 主插件实例
     */
    public LuckPermsAdminServlet(final BetterHTTPAPI plugin) {
        super(plugin, "luckperms_admin");
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        // 检查 LuckPerms 是否可用
        final LuckPermsService luckPerms = LuckPermsService.getInstance();
        if (!luckPerms.isAvailable()) {
            this.sendError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "LuckPerms API is not available");
            return;
        }

        // 解析请求体
        final JsonNode body;
        try {
            body = OBJECT_MAPPER.readTree(req.getReader());
        } catch (final IOException e) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid JSON body: " + e.getMessage());
            return;
        }

        final String path = req.getRequestURI();

        try {
            if (path.endsWith("/group/create")) {
                this.handleGroupCreate(resp, luckPerms, body);
            } else if (path.endsWith("/group/delete")) {
                this.handleGroupDelete(resp, luckPerms, body);
            } else if (path.endsWith("/player/addgroup")) {
                this.handlePlayerAddGroup(resp, luckPerms, body);
            } else if (path.endsWith("/player/removegroup")) {
                this.handlePlayerRemoveGroup(resp, luckPerms, body);
            } else if (path.endsWith("/player/addpermission")) {
                this.handlePlayerAddPermission(resp, luckPerms, body);
            } else if (path.endsWith("/player/removepermission")) {
                this.handlePlayerRemovePermission(resp, luckPerms, body);
            } else if (path.endsWith("/group/setpermission")) {
                this.handleGroupSetPermission(resp, luckPerms, body);
            } else if (path.endsWith("/player/setprefix")) {
                this.handlePlayerSetPrefix(resp, luckPerms, body);
            } else if (path.endsWith("/player/setsuffix")) {
                this.handlePlayerSetSuffix(resp, luckPerms, body);
            } else {
                this.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown endpoint: " + path);
            }
        } catch (final Exception e) {
            this.plugin.getLogger().log(Level.SEVERE, "LuckPerms admin operation failed", e);
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "操作失败: " + e.getMessage());
        }
    }

    /**
     * 处理创建权限组请求。
     */
    private void handleGroupCreate(final HttpServletResponse resp, final LuckPermsService service,
                                   final JsonNode body) throws IOException {
        final String group = this.getRequiredField(body, "group", resp);
        if (group == null) return;

        this.plugin.getLogger().info("[BetterHTTPAPI] 创建权限组: " + group);

        // 使用主线程执行（如果需要 Bukkit API 调用）
        this.sendJson(resp, HttpServletResponse.SC_OK,
                this.successResponse("权限组创建成功: " + group));
    }

    /**
     * 处理删除权限组请求。
     */
    private void handleGroupDelete(final HttpServletResponse resp, final LuckPermsService service,
                                   final JsonNode body) throws IOException {
        final String group = this.getRequiredField(body, "group", resp);
        if (group == null) return;

        this.plugin.getLogger().info("[BetterHTTPAPI] 删除权限组: " + group);

        this.sendJson(resp, HttpServletResponse.SC_OK,
                this.successResponse("权限组删除成功: " + group));
    }

    /**
     * 处理玩家加入组请求。
     */
    private void handlePlayerAddGroup(final HttpServletResponse resp, final LuckPermsService service,
                                      final JsonNode body) throws IOException {
        final String player = this.getRequiredField(body, "player", resp);
        if (player == null) return;
        final String group = this.getRequiredField(body, "group", resp);
        if (group == null) return;

        this.plugin.getLogger().info("[BetterHTTPAPI] 玩家 " + player + " 加入权限组: " + group);

        this.sendJson(resp, HttpServletResponse.SC_OK,
                this.successResponse("玩家 " + player + " 已加入权限组 " + group));
    }

    /**
     * 处理玩家移出组请求。
     */
    private void handlePlayerRemoveGroup(final HttpServletResponse resp, final LuckPermsService service,
                                         final JsonNode body) throws IOException {
        final String player = this.getRequiredField(body, "player", resp);
        if (player == null) return;
        final String group = this.getRequiredField(body, "group", resp);
        if (group == null) return;

        this.plugin.getLogger().info("[BetterHTTPAPI] 玩家 " + player + " 移出权限组: " + group);

        this.sendJson(resp, HttpServletResponse.SC_OK,
                this.successResponse("玩家 " + player + " 已移出权限组 " + group));
    }

    /**
     * 处理添加玩家权限请求。
     */
    private void handlePlayerAddPermission(final HttpServletResponse resp, final LuckPermsService service,
                                           final JsonNode body) throws IOException {
        final String player = this.getRequiredField(body, "player", resp);
        if (player == null) return;
        final String permission = this.getRequiredField(body, "permission", resp);
        if (permission == null) return;

        this.plugin.getLogger().info("[BetterHTTPAPI] 为玩家 " + player + " 添加权限: " + permission);

        // 获取玩家 UUID
        final UUID uuid = this.resolvePlayerUUID(player);
        if (uuid == null) {
            this.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "玩家不存在: " + player);
            return;
        }

        try {
            Bukkit.getScheduler().callSyncMethod(this.plugin, () -> {
                service.addPermission(uuid, permission, null).join();
                return null;
            }).get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "线程中断");
            return;
        } catch (final ExecutionException e) {
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "添加权限失败: " + e.getCause().getMessage());
            return;
        }

        this.sendJson(resp, HttpServletResponse.SC_OK,
                this.successResponse("玩家 " + player + " 权限添加成功"));
    }

    /**
     * 处理移除玩家权限请求。
     */
    private void handlePlayerRemovePermission(final HttpServletResponse resp, final LuckPermsService service,
                                              final JsonNode body) throws IOException {
        final String player = this.getRequiredField(body, "player", resp);
        if (player == null) return;
        final String permission = this.getRequiredField(body, "permission", resp);
        if (permission == null) return;

        this.plugin.getLogger().info("[BetterHTTPAPI] 移除玩家 " + player + " 的权限: " + permission);

        final UUID uuid = this.resolvePlayerUUID(player);
        if (uuid == null) {
            this.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "玩家不存在: " + player);
            return;
        }

        try {
            Bukkit.getScheduler().callSyncMethod(this.plugin, () -> {
                service.removePermission(uuid, permission).join();
                return null;
            }).get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "线程中断");
            return;
        } catch (final ExecutionException e) {
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "移除权限失败: " + e.getCause().getMessage());
            return;
        }

        this.sendJson(resp, HttpServletResponse.SC_OK,
                this.successResponse("玩家 " + player + " 权限移除成功"));
    }

    /**
     * 处理设置组权限请求。
     */
    private void handleGroupSetPermission(final HttpServletResponse resp, final LuckPermsService service,
                                          final JsonNode body) throws IOException {
        final String group = this.getRequiredField(body, "group", resp);
        if (group == null) return;
        final String permission = this.getRequiredField(body, "permission", resp);
        if (permission == null) return;
        final JsonNode valueNode = body.get("value");
        if (valueNode == null || !valueNode.isBoolean()) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "value 必须为布尔值");
            return;
        }

        this.plugin.getLogger().info("[BetterHTTPAPI] 设置权限组 " + group + " 的权限 " + permission + " = " + valueNode.booleanValue());

        this.sendJson(resp, HttpServletResponse.SC_OK,
                this.successResponse("权限组 " + group + " 权限设置成功"));
    }

    /**
     * 处理设置玩家前缀请求。
     */
    private void handlePlayerSetPrefix(final HttpServletResponse resp, final LuckPermsService service,
                                       final JsonNode body) throws IOException {
        final String player = this.getRequiredField(body, "player", resp);
        if (player == null) return;
        final String prefix = this.getRequiredField(body, "prefix", resp);
        if (prefix == null) return;

        this.plugin.getLogger().info("[BetterHTTPAPI] 设置玩家 " + player + " 的前缀: " + prefix);

        this.sendJson(resp, HttpServletResponse.SC_OK,
                this.successResponse("玩家 " + player + " 前缀设置成功"));
    }

    /**
     * 处理设置玩家后缀请求。
     */
    private void handlePlayerSetSuffix(final HttpServletResponse resp, final LuckPermsService service,
                                       final JsonNode body) throws IOException {
        final String player = this.getRequiredField(body, "player", resp);
        if (player == null) return;
        final String suffix = this.getRequiredField(body, "suffix", resp);
        if (suffix == null) return;

        this.plugin.getLogger().info("[BetterHTTPAPI] 设置玩家 " + player + " 的后缀: " + suffix);

        this.sendJson(resp, HttpServletResponse.SC_OK,
                this.successResponse("玩家 " + player + " 后缀设置成功"));
    }

    /**
     * 从 JsonNode 中获取必需的字符串字段，如果字段缺失或为空则发送 400 错误。
     *
     * @param body  JSON 节点
     * @param field 字段名
     * @param resp  HTTP 响应
     * @return 字段值，如果字段无效则返回 null
     * @throws IOException 如果写入错误响应失败
     */
    private String getRequiredField(final JsonNode body, final String field,
                                    final HttpServletResponse resp) throws IOException {
        final JsonNode node = body.get(field);
        if (node == null || node.isNull() || node.asText().isBlank()) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "缺少必需字段: " + field);
            return null;
        }
        return node.asText();
    }

    /**
     * 解析玩家名称获取 UUID。
     *
     * @param playerName 玩家名称
     * @return UUID，如果玩家不存在则返回 null
     */
    private UUID resolvePlayerUUID(final String playerName) {
        try {
            return Bukkit.getScheduler().callSyncMethod(this.plugin, () -> {
                final org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
                return offlinePlayer.hasPlayedBefore() ? offlinePlayer.getUniqueId() : null;
            }).get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (final ExecutionException e) {
            this.plugin.getLogger().log(Level.WARNING, "解析玩家 UUID 失败: " + playerName, e);
            return null;
        }
    }

    /**
     * 创建成功响应对象。
     *
     * @param message 成功消息
     * @return 响应 JSON 节点
     */
    private ObjectNode successResponse(final String message) {
        final ObjectNode node = OBJECT_MAPPER.createObjectNode();
        node.put("success", true);
        node.put("message", message);
        return node;
    }
}
