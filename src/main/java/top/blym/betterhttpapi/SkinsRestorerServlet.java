package top.blym.betterhttpapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import top.blym.betterhttpapi.service.SkinsRestorerService;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import net.skinsrestorer.api.SkinsRestorer;

/**
 * POST /api/skins/set —— 设置玩家皮肤。
 *
 * <p>请求体示例：</p>
 * <pre>{@code {"player": "Steve", "skinName": "Notch"}}</pre>
 * <pre>{@code {"player": "Steve", "skinUrl": "https://example.com/skin.png"}}</pre>
 *
 * <p>支持两种设置方式：</p>
 * <ul>
 *   <li>通过皮肤名称（skinName）：使用其他玩家的皮肤</li>
 *   <li>通过皮肤 URL（skinUrl）：使用自定义皮肤图片</li>
 * </ul>
 *
 * @author 白鹿原嚒
 */
public final class SkinsRestorerServlet extends BaseServlet {

    /**
     * 构造 SkinsRestorerServlet。
     *
     * @param plugin 主插件实例
     */
    public SkinsRestorerServlet(final BetterHTTPAPI plugin) {
        super(plugin, "skins");
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        final SkinsRequest request;
        try {
            request = OBJECT_MAPPER.readValue(req.getReader(), SkinsRequest.class);
        } catch (final IOException e) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid JSON body: " + e.getMessage());
            return;
        }

        // 验证必需字段
        if (request.player() == null || request.player().isBlank()) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Player name must not be empty");
            return;
        }

        // 检查是否提供了 skinName 或 skinUrl
        final boolean hasSkinName = request.skinName() != null && !request.skinName().isBlank();
        final boolean hasSkinUrl = request.skinUrl() != null && !request.skinUrl().isBlank();

        if (!hasSkinName && !hasSkinUrl) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Either 'skinName' or 'skinUrl' must be provided");
            return;
        }

        // 检查 SkinsRestorer API 是否可用
        final Optional<SkinsRestorer> skinApi = PluginAPIManager.getInstance().getSkinsRestorer();
        if (skinApi.isEmpty()) {
            this.sendError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "SkinsRestorer plugin is not installed or API unavailable");
            return;
        }

        try {
            final String resultMessage = Bukkit.getScheduler().callSyncMethod(this.plugin, () -> {
                // 检查玩家是否在线
                final Player player = Bukkit.getPlayerExact(request.player());
                if (player == null) {
                    return "Player not found or not online: " + request.player();
                }

                final SkinsRestorerService skinService = SkinsRestorerService.getInstance();

                if (hasSkinName) {
                    // 通过皮肤名称设置
                    skinService.setSkin(player, request.skinName());
                } else {
                    // 通过皮肤 URL 设置
                    skinService.setSkinUrl(player, request.skinUrl());
                }

                return null; // null means success
            }).get();

            if (resultMessage != null) {
                // 玩家不存在或 API 不可用
                this.sendError(resp, HttpServletResponse.SC_NOT_FOUND, resultMessage);
            } else {
                // 成功
                final String skinInfo = hasSkinName
                        ? "skin name: " + request.skinName()
                        : "skin URL: " + request.skinUrl();
                this.sendJson(resp, HttpServletResponse.SC_OK,
                        new SkinsResponse(true, "Skin set successfully for player " + request.player() + " using " + skinInfo));
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Thread interrupted");
        } catch (final ExecutionException e) {
            this.plugin.getLogger().log(Level.SEVERE,
                    "Error setting skin for player: " + request.player(), e);
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Skin set failed: " + e.getCause().getMessage());
        }
    }

    // ======================== 数据类型 ========================

    /**
     * 皮肤设置请求体。
     *
     * @param player  玩家名称
     * @param skinName 皮肤名称（与 skinUrl 二选一）
     * @param skinUrl  皮肤图片 URL（与 skinName 二选一）
     */
    private record SkinsRequest(
            @JsonProperty("player") String player,
            @JsonProperty("skinName") String skinName,
            @JsonProperty("skinUrl") String skinUrl) {
    }

    /**
     * 皮肤设置响应体。
     *
     * @param success 是否成功
     * @param message 响应消息
     */
    private record SkinsResponse(boolean success, String message) {
    }
}