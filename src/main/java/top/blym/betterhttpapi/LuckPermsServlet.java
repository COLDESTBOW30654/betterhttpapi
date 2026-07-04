package top.blym.betterhttpapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import top.blym.betterhttpapi.service.LuckPermsService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * LuckPerms 权限管理 API 端点。
 *
 * <p>提供两个端点：</p>
 * <ul>
 *   <li>POST /api/luckperms/check - 检查玩家权限</li>
 *   <li>GET /api/luckperms/groups - 获取玩家权限组</li>
 * </ul>
 *
 * @author 白鹿原嚒
 */
public final class LuckPermsServlet extends BaseServlet {

    /**
     * 构造 LuckPerms Servlet。
     *
     * <p>注意：由于此 Servlet 处理多个端点，基础端点名称设置为 "luckperms"，
     * 但具体的端点开关检查在 doGet() 和 doPost() 方法中进行。</p>
     *
     * @param plugin 主插件实例
     */
    public LuckPermsServlet(final BetterHTTPAPI plugin) {
        super(plugin, "luckperms");
    }

    /**
     * POST /api/luckperms/check - 检查玩家权限。
     *
     * <p>请求体示例：</p>
     * <pre>{@code {"player": "Steve", "permission": "minecraft.command.gamemode"}}</pre>
     *
     * <p>响应示例：</p>
     * <pre>{@code {"success": true, "hasPermission": true}}</pre>
     */
    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        
        // 检查 luckperms_check 端点是否启用
        if (!this.config.isEndpointEnabled("luckperms_check")) {
            this.sendError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Endpoint disabled: luckperms_check");
            return;
        }

        // 检查 LuckPerms 是否可用
        final LuckPermsService luckPerms = LuckPermsService.getInstance();
        if (!luckPerms.isAvailable()) {
            this.sendError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "LuckPerms API is not available");
            return;
        }

        // 解析请求体
        final CheckPermissionRequest request;
        try {
            request = OBJECT_MAPPER.readValue(req.getReader(), CheckPermissionRequest.class);
        } catch (final IOException e) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid JSON body: " + e.getMessage());
            return;
        }

        // 验证参数
        final String playerName = request.player();
        final String permission = request.permission();
        if (playerName == null || playerName.isBlank()) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Player name must not be empty");
            return;
        }
        if (permission == null || permission.isBlank()) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Permission must not be empty");
            return;
        }

        // 获取玩家对象
        final Player player = Bukkit.getPlayerExact(playerName);
        
        try {
            if (player != null && player.isOnline()) {
                // 在线玩家：在主线程同步检查权限
                final boolean hasPermission = Bukkit.getScheduler()
                        .callSyncMethod(this.plugin, () -> luckPerms.hasPermission(player, permission))
                        .get();
                
                this.sendJson(resp, HttpServletResponse.SC_OK,
                        new CheckPermissionResponse(true, hasPermission));
            } else {
                // 离线玩家：异步检查权限
                final UUID uuid = this.getOfflinePlayerUUID(playerName);
                if (uuid == null) {
                    this.sendError(resp, HttpServletResponse.SC_NOT_FOUND,
                            "Player not found: " + playerName);
                    return;
                }

                luckPerms.hasPermissionOffline(uuid, permission)
                        .thenAccept(hasPermission -> {
                            try {
                                this.sendJson(resp, HttpServletResponse.SC_OK,
                                        new CheckPermissionResponse(true, hasPermission));
                            } catch (IOException e) {
                                this.plugin.getLogger().log(Level.WARNING,
                                        "Failed to send response", e);
                            }
                        })
                        .exceptionally(throwable -> {
                            this.plugin.getLogger().log(Level.SEVERE,
                                    "Error checking offline player permission", throwable);
                            try {
                                this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                        "Error checking permission: " + throwable.getMessage());
                            } catch (IOException e) {
                                // Ignore
                            }
                            return null;
                        });
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Thread interrupted");
        } catch (final ExecutionException e) {
            this.plugin.getLogger().log(Level.SEVERE,
                    "Error checking player permission", e);
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error checking permission: " + e.getCause().getMessage());
        }
    }

    /**
     * GET /api/luckperms/groups - 获取玩家权限组。
     *
     * <p>请求参数：player=玩家名</p>
     *
     * <p>响应示例：</p>
     * <pre>{@code {"success": true, "groups": ["default", "admin"]}}</pre>
     */
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        
        // 检查 luckperms_groups 端点是否启用
        if (!this.config.isEndpointEnabled("luckperms_groups")) {
            this.sendError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Endpoint disabled: luckperms_groups");
            return;
        }

        // 检查 LuckPerms 是否可用
        final LuckPermsService luckPerms = LuckPermsService.getInstance();
        if (!luckPerms.isAvailable()) {
            this.sendError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "LuckPerms API is not available");
            return;
        }

        // 获取玩家名参数
        final String playerName = req.getParameter("player");
        if (playerName == null || playerName.isBlank()) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, 
                    "Missing or empty 'player' parameter");
            return;
        }

        // 获取玩家 UUID
        final Player player = Bukkit.getPlayerExact(playerName);
        final UUID uuid;
        
        if (player != null && player.isOnline()) {
            uuid = player.getUniqueId();
        } else {
            uuid = this.getOfflinePlayerUUID(playerName);
            if (uuid == null) {
                this.sendError(resp, HttpServletResponse.SC_NOT_FOUND,
                        "Player not found: " + playerName);
                return;
            }
        }

        // 异步获取权限组
        luckPerms.getPlayerGroups(uuid)
                .thenAccept(groups -> {
                    try {
                        this.sendJson(resp, HttpServletResponse.SC_OK,
                                new PlayerGroupsResponse(true, groups));
                    } catch (IOException e) {
                        this.plugin.getLogger().log(Level.WARNING,
                                "Failed to send response", e);
                    }
                })
                .exceptionally(throwable -> {
                    this.plugin.getLogger().log(Level.SEVERE,
                            "Error getting player groups", throwable);
                    try {
                        this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                "Error getting groups: " + throwable.getMessage());
                    } catch (IOException e) {
                        // Ignore
                    }
                    return null;
                });
    }

    /**
     * 获取离线玩家的 UUID。
     *
     * @param playerName 玩家名
     * @return UUID，如果玩家不存在则返回 null
     */
    private UUID getOfflinePlayerUUID(final String playerName) {
        try {
            return Bukkit.getScheduler()
                    .callSyncMethod(this.plugin, () -> {
                        final org.bukkit.OfflinePlayer offlinePlayer = 
                                Bukkit.getOfflinePlayer(playerName);
                        return offlinePlayer.hasPlayedBefore() ? offlinePlayer.getUniqueId() : null;
                    })
                    .get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            this.plugin.getLogger().log(Level.WARNING,
                    "Error getting offline player UUID", e);
            return null;
        }
    }

    // ======================== 数据类型 ========================

    /**
     * 检查权限请求体。
     */
    private record CheckPermissionRequest(
            @JsonProperty("player") String player,
            @JsonProperty("permission") String permission
    ) {
    }

    /**
     * 检查权限响应。
     */
    private record CheckPermissionResponse(
            @JsonProperty("success") boolean success,
            @JsonProperty("hasPermission") boolean hasPermission
    ) {
    }

    /**
     * 玩家权限组响应。
     */
    private record PlayerGroupsResponse(
            @JsonProperty("success") boolean success,
            @JsonProperty("groups") List<String> groups
    ) {
    }
}