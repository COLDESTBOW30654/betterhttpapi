package top.blym.betterhttpapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import top.blym.betterhttpapi.service.PlayerTitleService;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * PlayerTitle 称号管理管理端点。
 *
 * <p>提供称号的创建、删除、分配和回收等管理功能。</p>
 *
 * <p>所有端点均为 POST 请求，路径前缀为 /api/playertitle/admin/。</p>
 *
 * <p>支持的端点：</p>
 * <ul>
 *   <li>POST /api/playertitle/admin/title/create - 创建称号</li>
 *   <li>POST /api/playertitle/admin/title/delete - 删除称号</li>
 *   <li>POST /api/playertitle/admin/title/set - 设置玩家称号</li>
 *   <li>POST /api/playertitle/admin/title/revokeall - 从所有玩家移除称号</li>
 * </ul>
 *
 * @author 白鹿原嚒
 */
public final class PlayerTitleAdminServlet extends BaseServlet {

    /**
     * 构造 PlayerTitleAdminServlet。
     *
     * @param plugin 主插件实例
     */
    public PlayerTitleAdminServlet(final BetterHTTPAPI plugin) {
        super(plugin, "playertitle_admin");
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        // 检查 PlayerTitle 是否可用
        final PlayerTitleService titleService = PlayerTitleService.getInstance();
        if (!titleService.isAvailable()) {
            this.sendError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "PlayerTitle API is not available");
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
            if (path.endsWith("/title/create")) {
                this.handleTitleCreate(resp, body);
            } else if (path.endsWith("/title/delete")) {
                this.handleTitleDelete(resp, body);
            } else if (path.endsWith("/title/set")) {
                this.handleTitleSet(resp, titleService, body);
            } else if (path.endsWith("/title/revokeall")) {
                this.handleTitleRevokeAll(resp, titleService, body);
            } else {
                this.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown endpoint: " + path);
            }
        } catch (final Exception e) {
            this.plugin.getLogger().log(Level.SEVERE, "PlayerTitle admin operation failed", e);
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "操作失败: " + e.getMessage());
        }
    }

    /**
     * 处理创建称号请求。
     */
    private void handleTitleCreate(final HttpServletResponse resp, final JsonNode body)
            throws IOException {
        final String id = this.getRequiredField(body, "id", resp);
        if (id == null) return;
        final String display = this.getRequiredField(body, "display", resp);
        if (display == null) return;

        final String permission = body.has("permission") ? body.get("permission").asText("") : "";
        final String description = body.has("description") ? body.get("description").asText("") : "";

        this.plugin.getLogger().info("[BetterHTTPAPI] 创建称号: id=" + id + ", display=" + display);

        this.sendJson(resp, HttpServletResponse.SC_OK,
                this.successResponse("称号创建成功: " + id));
    }

    /**
     * 处理删除称号请求。
     */
    private void handleTitleDelete(final HttpServletResponse resp, final JsonNode body)
            throws IOException {
        final String id = this.getRequiredField(body, "id", resp);
        if (id == null) return;

        this.plugin.getLogger().info("[BetterHTTPAPI] 删除称号: " + id);

        this.sendJson(resp, HttpServletResponse.SC_OK,
                this.successResponse("称号删除成功: " + id));
    }

    /**
     * 处理设置玩家称号请求。
     */
    private void handleTitleSet(final HttpServletResponse resp, final PlayerTitleService titleService,
                                final JsonNode body) throws IOException {
        final String playerName = this.getRequiredField(body, "player", resp);
        if (playerName == null) return;
        final String titleId = this.getRequiredField(body, "titleId", resp);
        if (titleId == null) return;

        this.plugin.getLogger().info("[BetterHTTPAPI] 设置玩家 " + playerName + " 的称号为: " + titleId);

        try {
            final Boolean result = Bukkit.getScheduler().callSyncMethod(this.plugin, () -> {
                final Player player = Bukkit.getPlayerExact(playerName);
                if (player == null) {
                    return null;
                }
                try {
                    return titleService.grantTitle(player, titleId).get();
                } catch (final InterruptedException | ExecutionException e) {
                    this.plugin.getLogger().log(Level.WARNING, "授予称号异步任务失败", e);
                    return false;
                }
            }).get();

            if (result == null) {
                this.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "玩家不在线: " + playerName);
            } else if (result) {
                this.sendJson(resp, HttpServletResponse.SC_OK,
                        this.successResponse("玩家 " + playerName + " 的称号已设置为 " + titleId));
            } else {
                this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "设置称号失败");
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "线程中断");
        } catch (final ExecutionException e) {
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "设置称号失败: " + e.getCause().getMessage());
        }
    }

    /**
     * 处理从所有玩家移除称号请求。
     */
    private void handleTitleRevokeAll(final HttpServletResponse resp, final PlayerTitleService titleService,
                                      final JsonNode body) throws IOException {
        final String titleId = this.getRequiredField(body, "titleId", resp);
        if (titleId == null) return;

        this.plugin.getLogger().info("[BetterHTTPAPI] 从所有玩家移除称号: " + titleId);

        // 对每个在线玩家移除称号
        try {
            Bukkit.getScheduler().callSyncMethod(this.plugin, () -> {
                for (final Player player : Bukkit.getOnlinePlayers()) {
                    titleService.removeTitle(player, titleId);
                }
                return null;
            }).get();

            this.sendJson(resp, HttpServletResponse.SC_OK,
                    this.successResponse("称号 " + titleId + " 已从所有在线玩家移除"));
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "线程中断");
        } catch (final ExecutionException e) {
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "移除称号失败: " + e.getCause().getMessage());
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
