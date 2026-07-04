package top.blym.betterhttpapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import top.blym.betterhttpapi.service.SkinsRestorerService;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * SkinsRestorer 皮肤管理管理端点。
 *
 * <p>提供皮肤的强制设置、缓存清除和应用等管理功能。</p>
 *
 * <p>所有端点均为 POST 请求，路径前缀为 /api/skins/admin/。</p>
 *
 * <p>支持的端点：</p>
 * <ul>
 *   <li>POST /api/skins/admin/setforce - 强制设置皮肤</li>
 *   <li>POST /api/skins/admin/clearcache - 清除玩家皮肤缓存</li>
 *   <li>POST /api/skins/admin/apply - 应用皮肤到玩家</li>
 * </ul>
 *
 * @author 白鹿原嚒
 */
public final class SkinsAdminServlet extends BaseServlet {

    /**
     * 构造 SkinsAdminServlet。
     *
     * @param plugin 主插件实例
     */
    public SkinsAdminServlet(final BetterHTTPAPI plugin) {
        super(plugin, "skins_admin");
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        // 检查 SkinsRestorer 是否可用
        final SkinsRestorerService skinService = SkinsRestorerService.getInstance();
        if (!PluginAPIManager.getInstance().getSkinsRestorer().isPresent()) {
            this.sendError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "SkinsRestorer API is not available");
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
            if (path.endsWith("/setforce")) {
                this.handleSetForce(resp, skinService, body);
            } else if (path.endsWith("/clearcache")) {
                this.handleClearCache(resp, skinService, body);
            } else if (path.endsWith("/apply")) {
                this.handleApply(resp, skinService, body);
            } else {
                this.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown endpoint: " + path);
            }
        } catch (final Exception e) {
            this.plugin.getLogger().log(Level.SEVERE, "Skins admin operation failed", e);
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "操作失败: " + e.getMessage());
        }
    }

    /**
     * 处理强制设置皮肤请求。
     */
    private void handleSetForce(final HttpServletResponse resp, final SkinsRestorerService skinService,
                                final JsonNode body) throws IOException {
        final String player = this.getRequiredField(body, "player", resp);
        if (player == null) return;
        final String skin = this.getRequiredField(body, "skin", resp);
        if (skin == null) return;

        this.plugin.getLogger().info("[BetterHTTPAPI] 强制设置玩家 " + player + " 的皮肤为: " + skin);

        try {
            final String result = Bukkit.getScheduler().callSyncMethod(this.plugin, () -> {
                final Player bukkitPlayer = Bukkit.getPlayerExact(player);
                if (bukkitPlayer == null) {
                    return "Player not found or not online: " + player;
                }
                skinService.setSkin(bukkitPlayer, skin);
                return null;
            }).get();

            if (result != null) {
                this.sendError(resp, HttpServletResponse.SC_NOT_FOUND, result);
            } else {
                this.sendJson(resp, HttpServletResponse.SC_OK,
                        this.successResponse("玩家 " + player + " 皮肤已强制设置为 " + skin));
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "线程中断");
        } catch (final ExecutionException e) {
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "强制设置皮肤失败: " + e.getCause().getMessage());
        }
    }

    /**
     * 处理清除皮肤缓存请求。
     */
    private void handleClearCache(final HttpServletResponse resp, final SkinsRestorerService skinService,
                                  final JsonNode body) throws IOException {
        final String player = this.getRequiredField(body, "player", resp);
        if (player == null) return;

        this.plugin.getLogger().info("[BetterHTTPAPI] 清除玩家 " + player + " 的皮肤缓存");

        try {
            final String result = Bukkit.getScheduler().callSyncMethod(this.plugin, () -> {
                final Player bukkitPlayer = Bukkit.getPlayerExact(player);
                if (bukkitPlayer == null) {
                    return "Player not found or not online: " + player;
                }
                skinService.resetSkin(bukkitPlayer);
                return null;
            }).get();

            if (result != null) {
                this.sendError(resp, HttpServletResponse.SC_NOT_FOUND, result);
            } else {
                this.sendJson(resp, HttpServletResponse.SC_OK,
                        this.successResponse("玩家 " + player + " 皮肤缓存已清除"));
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "线程中断");
        } catch (final ExecutionException e) {
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "清除皮肤缓存失败: " + e.getCause().getMessage());
        }
    }

    /**
     * 处理应用皮肤请求。
     */
    private void handleApply(final HttpServletResponse resp, final SkinsRestorerService skinService,
                             final JsonNode body) throws IOException {
        final String player = this.getRequiredField(body, "player", resp);
        if (player == null) return;

        this.plugin.getLogger().info("[BetterHTTPAPI] 应用皮肤到玩家: " + player);

        try {
            final String result = Bukkit.getScheduler().callSyncMethod(this.plugin, () -> {
                final Player bukkitPlayer = Bukkit.getPlayerExact(player);
                if (bukkitPlayer == null) {
                    return "Player not found or not online: " + player;
                }
                // 应用皮肤：重新设置当前皮肤以触发应用
                final String currentSkin = skinService.getCurrentSkinName(bukkitPlayer);
                if (currentSkin != null) {
                    skinService.setSkin(bukkitPlayer, currentSkin);
                    return null;
                }
                return "No custom skin found for player: " + player;
            }).get();

            if (result != null) {
                this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, result);
            } else {
                this.sendJson(resp, HttpServletResponse.SC_OK,
                        this.successResponse("玩家 " + player + " 皮肤已应用"));
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "线程中断");
        } catch (final ExecutionException e) {
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "应用皮肤失败: " + e.getCause().getMessage());
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
