package top.blym.betterhttpapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import top.blym.betterhttpapi.service.PlayerTitleService;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * 称号管理 API 端点。
 *
 * <p>提供两个接口：</p>
 * <ul>
 *   <li>GET /api/playertitle/get - 获取玩家当前称号</li>
 *   <li>POST /api/playertitle/grant - 授予玩家称号</li>
 * </ul>
 *
 * <p>所有操作需要通过 API Key 认证，并受端点开关控制。</p>
 *
 * @author 白鹿原嚒
 */
public final class PlayerTitleServlet extends BaseServlet {

    /**
     * 构造 PlayerTitleServlet。
     *
     * @param plugin 主插件实例
     */
    public PlayerTitleServlet(final BetterHTTPAPI plugin) {
        super(plugin, "playertitle");
    }

    /**
     * GET /api/playertitle/get —— 获取玩家当前称号。
     *
     * <p>Query 参数：</p>
     * <ul>
     *   <li>player - 玩家名称（必填）</li>
     * </ul>
     *
     * <p>响应示例：</p>
     * <pre>{@code {"success": true, "title": "VIP玩家"}}</pre>
     * <pre>{@code {"success": true, "title": null}}</pre>
     */
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        final String playerName = req.getParameter("player");

        if (playerName == null || playerName.isBlank()) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing parameter: player");
            return;
        }

        // 检查 PlayerTitle 服务是否可用
        final PlayerTitleService titleService = PlayerTitleService.getInstance();
        if (!titleService.isAvailable()) {
            this.sendError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "PlayerTitle API is not available");
            return;
        }

        try {
            final TitleGetResponse response = Bukkit.getScheduler()
                    .callSyncMethod(this.plugin, () -> {
                        final Player player = Bukkit.getPlayerExact(playerName);
                        if (player == null) {
                            return new TitleGetResponse(false, "Player not found: " + playerName, null);
                        }

                        final String currentTitle = titleService.getCurrentTitle(player);
                        return new TitleGetResponse(true, null, currentTitle);
                    })
                    .get();

            if (response.success()) {
                this.sendJson(resp, HttpServletResponse.SC_OK,
                        new TitleSuccessResponse(true, response.title()));
            } else {
                this.sendJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                        new TitleErrorResponse(false, response.message()));
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Thread interrupted");
        } catch (final ExecutionException e) {
            this.plugin.getLogger().log(Level.SEVERE,
                    "Error getting player title: " + playerName, e);
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to get title: " + e.getCause().getMessage());
        }
    }

    /**
     * POST /api/playertitle/grant —— 授予玩家称号。
     *
     * <p>请求体示例：</p>
     * <pre>{@code {"player": "Steve", "titleId": "vip_title"}}</pre>
     *
     * <p>响应示例：</p>
     * <pre>{@code {"success": true, "message": "Title granted successfully"}}</pre>
     */
    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        final GrantRequest request;
        try {
            request = OBJECT_MAPPER.readValue(req.getReader(), GrantRequest.class);
        } catch (final IOException e) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid JSON body: " + e.getMessage());
            return;
        }

        if (request.player() == null || request.player().isBlank()) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Player name must not be empty");
            return;
        }

        if (request.titleId() == null || request.titleId().isBlank()) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "titleId must not be empty");
            return;
        }

        // 检查 PlayerTitle 服务是否可用
        final PlayerTitleService titleService = PlayerTitleService.getInstance();
        if (!titleService.isAvailable()) {
            this.sendError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "PlayerTitle API is not available");
            return;
        }

        try {
            final Boolean grantResult = Bukkit.getScheduler()
                    .callSyncMethod(this.plugin, () -> {
                        final Player player = Bukkit.getPlayerExact(request.player());
                        if (player == null) {
                            return null; // 玩家不存在
                        }
                        // grantTitle 返回 CompletableFuture，这里同步等待结果
                        try {
                            return titleService.grantTitle(player, request.titleId()).get();
                        } catch (InterruptedException | ExecutionException e) {
                            this.plugin.getLogger().log(Level.WARNING,
                                    "Error in grantTitle future", e);
                            return false;
                        }
                    })
                    .get();

            if (grantResult == null) {
                this.sendJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                        new GrantResponse(false, "Player not found: " + request.player()));
            } else if (grantResult) {
                this.sendJson(resp, HttpServletResponse.SC_OK,
                        new GrantResponse(true, "Title granted successfully"));
            } else {
                this.sendJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        new GrantResponse(false, "Failed to grant title"));
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Thread interrupted");
        } catch (final ExecutionException e) {
            this.plugin.getLogger().log(Level.SEVERE,
                    "Error granting title to player: " + request.player(), e);
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Grant failed: " + e.getCause().getMessage());
        }
    }

    // ======================== 数据类型 ========================

    /**
     * 授予称号请求体。
     */
    private record GrantRequest(
            @JsonProperty("player") String player,
            @JsonProperty("titleId") String titleId) {
    }

    /**
     * 授予称号响应。
     */
    private record GrantResponse(boolean success, String message) {
    }

    /**
     * 获取称号内部响应（包含 success、message、title）。
     */
    private record TitleGetResponse(boolean success, String message, String title) {
    }

    /**
     * 获取称号成功响应（对外）。
     */
    private record TitleSuccessResponse(boolean success, String title) {
    }

    /**
     * 获取称号失败响应。
     */
    private record TitleErrorResponse(boolean success, String message) {
    }
}