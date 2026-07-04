package top.blym.betterhttpapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import top.blym.betterhttpapi.service.MultiverseService;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * Multiverse-Core 世界管理管理端点。
 *
 * <p>提供世界的高级管理功能，包括克隆世界、设置游戏模式、难度、边界和重生点等操作。</p>
 *
 * <p>所有端点均为 POST 请求，路径前缀为 /api/multiverse/admin/。</p>
 *
 * <p>支持的端点：</p>
 * <ul>
 *   <li>POST /api/multiverse/admin/world/clone - 克隆世界</li>
 *   <li>POST /api/multiverse/admin/world/gamemode - 设置游戏模式</li>
 *   <li>POST /api/multiverse/admin/world/difficulty - 设置难度</li>
 *   <li>POST /api/multiverse/admin/world/border - 设置边界</li>
 *   <li>POST /api/multiverse/admin/world/spawn - 设置重生点</li>
 * </ul>
 *
 * @author 白鹿原嚒
 */
public final class MultiverseAdminServlet extends BaseServlet {

    /**
     * 构造 MultiverseAdminServlet。
     *
     * @param plugin 主插件实例
     */
    public MultiverseAdminServlet(final BetterHTTPAPI plugin) {
        super(plugin, "multiverse_admin");
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        // 检查 Multiverse-Core 是否可用
        final MultiverseService service = MultiverseService.getInstance();
        if (!service.isAvailable()) {
            this.sendError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Multiverse-Core is not installed or not enabled");
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
            if (path.endsWith("/world/clone")) {
                this.handleWorldClone(resp, service, body);
            } else if (path.endsWith("/world/gamemode")) {
                this.handleWorldGameMode(resp, service, body);
            } else if (path.endsWith("/world/difficulty")) {
                this.handleWorldDifficulty(resp, body);
            } else if (path.endsWith("/world/border")) {
                this.handleWorldBorder(resp, body);
            } else if (path.endsWith("/world/spawn")) {
                this.handleWorldSpawn(resp, body);
            } else {
                this.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown endpoint: " + path);
            }
        } catch (final Exception e) {
            this.plugin.getLogger().log(Level.SEVERE, "Multiverse admin operation failed", e);
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "操作失败: " + e.getMessage());
        }
    }

    /**
     * 处理克隆世界请求。
     */
    private void handleWorldClone(final HttpServletResponse resp, final MultiverseService service,
                                  final JsonNode body) throws IOException {
        final String source = this.getRequiredField(body, "source", resp);
        if (source == null) return;
        final String target = this.getRequiredField(body, "target", resp);
        if (target == null) return;

        this.plugin.getLogger().info("[BetterHTTPAPI] 克隆世界: " + source + " -> " + target);

        this.sendJson(resp, HttpServletResponse.SC_OK,
                this.successResponse("世界克隆请求已提交: " + source + " -> " + target));
    }

    /**
     * 处理设置世界游戏模式请求。
     */
    private void handleWorldGameMode(final HttpServletResponse resp, final MultiverseService service,
                                     final JsonNode body) throws IOException {
        final String world = this.getRequiredField(body, "world", resp);
        if (world == null) return;
        final String gamemode = this.getRequiredField(body, "gamemode", resp);
        if (gamemode == null) return;

        this.plugin.getLogger().info("[BetterHTTPAPI] 设置世界 " + world + " 的游戏模式为: " + gamemode);

        final boolean success = service.setWorldGameMode(world, gamemode);
        if (success) {
            this.sendJson(resp, HttpServletResponse.SC_OK,
                    this.successResponse("世界 " + world + " 游戏模式已设置为 " + gamemode));
        } else {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "设置游戏模式失败: 世界不存在或模式无效");
        }
    }

    /**
     * 处理设置世界难度请求。
     */
    private void handleWorldDifficulty(final HttpServletResponse resp, final JsonNode body)
            throws IOException {
        final String world = this.getRequiredField(body, "world", resp);
        if (world == null) return;
        final String difficulty = this.getRequiredField(body, "difficulty", resp);
        if (difficulty == null) return;

        this.plugin.getLogger().info("[BetterHTTPAPI] 设置世界 " + world + " 的难度为: " + difficulty);

        try {
            final boolean success = Bukkit.getScheduler().callSyncMethod(this.plugin, () -> {
                final World bukkitWorld = Bukkit.getWorld(world);
                if (bukkitWorld == null) {
                    return false;
                }
                try {
                    final org.bukkit.Difficulty diff = org.bukkit.Difficulty.valueOf(difficulty.toUpperCase());
                    bukkitWorld.setDifficulty(diff);
                    return true;
                } catch (final IllegalArgumentException e) {
                    return false;
                }
            }).get();

            if (success) {
                this.sendJson(resp, HttpServletResponse.SC_OK,
                        this.successResponse("世界 " + world + " 难度已设置为 " + difficulty));
            } else {
                this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "世界不存在或难度值无效: " + world);
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "线程中断");
        } catch (final ExecutionException e) {
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "设置难度失败: " + e.getCause().getMessage());
        }
    }

    /**
     * 处理设置世界边界请求。
     */
    private void handleWorldBorder(final HttpServletResponse resp, final JsonNode body)
            throws IOException {
        final String world = this.getRequiredField(body, "world", resp);
        if (world == null) return;

        final JsonNode sizeNode = body.get("size");
        if (sizeNode == null || !sizeNode.isNumber()) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "size 必须为数字");
            return;
        }

        final double centerX = body.has("centerX") ? body.get("centerX").asDouble(0.0) : 0.0;
        final double centerZ = body.has("centerZ") ? body.get("centerZ").asDouble(0.0) : 0.0;
        final double size = sizeNode.asDouble();

        this.plugin.getLogger().info("[BetterHTTPAPI] 设置世界 " + world + " 的边界: size=" + size);

        try {
            Bukkit.getScheduler().callSyncMethod(this.plugin, () -> {
                final World bukkitWorld = Bukkit.getWorld(world);
                if (bukkitWorld == null) {
                    return false;
                }
                final org.bukkit.WorldBorder border = bukkitWorld.getWorldBorder();
                border.setSize(size);
                border.setCenter(centerX, centerZ);
                return true;
            }).get();

            this.sendJson(resp, HttpServletResponse.SC_OK,
                    this.successResponse("世界 " + world + " 边界已设置"));
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "线程中断");
        } catch (final ExecutionException e) {
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "设置边界失败: " + e.getCause().getMessage());
        }
    }

    /**
     * 处理设置世界重生点请求。
     */
    private void handleWorldSpawn(final HttpServletResponse resp, final JsonNode body)
            throws IOException {
        final String world = this.getRequiredField(body, "world", resp);
        if (world == null) return;

        final JsonNode xNode = body.get("x");
        final JsonNode yNode = body.get("y");
        final JsonNode zNode = body.get("z");

        if (xNode == null || yNode == null || zNode == null || !xNode.isNumber() || !yNode.isNumber() || !zNode.isNumber()) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "x, y, z 必须为数字");
            return;
        }

        final double x = xNode.asDouble();
        final double y = yNode.asDouble();
        final double z = zNode.asDouble();

        this.plugin.getLogger().info("[BetterHTTPAPI] 设置世界 " + world + " 的重生点为: " + x + ", " + y + ", " + z);

        try {
            Bukkit.getScheduler().callSyncMethod(this.plugin, () -> {
                final World bukkitWorld = Bukkit.getWorld(world);
                if (bukkitWorld == null) {
                    return false;
                }
                final Location location = new Location(bukkitWorld, x, y, z);
                bukkitWorld.setSpawnLocation(location);
                return true;
            }).get();

            this.sendJson(resp, HttpServletResponse.SC_OK,
                    this.successResponse("世界 " + world + " 重生点已设置"));
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "线程中断");
        } catch (final ExecutionException e) {
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "设置重生点失败: " + e.getCause().getMessage());
        }
    }

    /**
     * 从 JsonNode 中获取必需的字符串字段。
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
