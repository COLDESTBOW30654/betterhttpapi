package top.blym.betterhttpapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import top.blym.betterhttpapi.service.AuthMeService;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * POST /api/authme/logout - 强制登出玩家。
 *
 * <p>请求体示例：</p>
 * <pre>{@code {"player": "Steve"}}</pre>
 *
 * <p>响应示例：</p>
 * <pre>{@code {"success": true, "message": "Player logged out successfully"}}</pre>
 *
 * @author 白鹿原嚒
 */
public final class AuthMeLogoutServlet extends BaseServlet {

    /**
     * 构造 AuthMeLogout Servlet。
     *
     * @param plugin 主插件实例
     */
    public AuthMeLogoutServlet(final BetterHTTPAPI plugin) {
        super(plugin, "authme_logout");
    }

    /**
     * POST /api/authme/logout - 强制登出玩家。
     *
     * <p>请求体：</p>
     * <ul>
     *   <li>player - 玩家名（必填）</li>
     * </ul>
     *
     * <p>响应示例：</p>
     * <pre>{@code {"success": true, "message": "Player logged out successfully"}}</pre>
     */
    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {

        // 检查 AuthMe 是否可用
        final AuthMeService authMe = AuthMeService.getInstance();
        if (!authMe.isAvailable()) {
            this.sendError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "AuthMe API is not available");
            return;
        }

        // 解析请求体
        final LogoutRequest request;
        try {
            request = OBJECT_MAPPER.readValue(req.getReader(), LogoutRequest.class);
        } catch (final IOException e) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid JSON body: " + e.getMessage());
            return;
        }

        // 验证参数
        final String playerName = request.player();

        if (playerName == null || playerName.isBlank()) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Player name must not be empty");
            return;
        }

        // 检查玩家是否在线
        final Player player = Bukkit.getPlayerExact(playerName);
        if (player == null || !player.isOnline()) {
            this.sendError(resp, HttpServletResponse.SC_NOT_FOUND,
                    "Player is not online: " + playerName);
            return;
        }

        try {
            // 在主线程执行登出
            final boolean success = Bukkit.getScheduler()
                    .callSyncMethod(this.plugin, () -> authMe.forceLogout(player))
                    .get();

            if (success) {
                this.sendJson(resp, HttpServletResponse.SC_OK,
                        new LogoutResponse(true, "Player logged out successfully"));
            } else {
                this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Failed to logout player");
            }

        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Thread interrupted");
        } catch (final ExecutionException e) {
            this.plugin.getLogger().log(Level.SEVERE,
                    "Error logging out player: " + playerName, e);
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error logging out player: " + e.getCause().getMessage());
        }
    }

    // ======================== 数据类型 ========================

    /**
     * 登出请求体。
     */
    private record LogoutRequest(
            @JsonProperty("player") String player
    ) {
    }

    /**
     * 登出响应。
     */
    private record LogoutResponse(
            @JsonProperty("success") boolean success,
            @JsonProperty("message") String message
    ) {
    }
}