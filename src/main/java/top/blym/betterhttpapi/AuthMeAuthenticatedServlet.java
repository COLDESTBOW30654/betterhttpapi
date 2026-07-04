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
 * GET /api/authme/authenticated - 检查玩家是否已认证（已登录）。
 *
 * <p>请求参数：player=玩家名</p>
 *
 * <p>响应示例：</p>
 * <pre>{@code {"success": true, "authenticated": true}}</pre>
 *
 * @author 白鹿原嚒
 */
public final class AuthMeAuthenticatedServlet extends BaseServlet {

    /**
     * 构造 AuthMeAuthenticated Servlet。
     *
     * @param plugin 主插件实例
     */
    public AuthMeAuthenticatedServlet(final BetterHTTPAPI plugin) {
        super(plugin, "authme_authenticated");
    }

    /**
     * GET /api/authme/authenticated - 检查玩家是否已通过 AuthMe 认证。
     *
     * <p>请求参数：</p>
     * <ul>
     *   <li>player - 玩家名（必填）</li>
     * </ul>
     *
     * <p>响应示例：</p>
     * <pre>{@code {"success": true, "authenticated": true}}</pre>
     */
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {

        // 检查 AuthMe 是否可用
        final AuthMeService authMe = AuthMeService.getInstance();
        if (!authMe.isAvailable()) {
            this.sendError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "AuthMe API is not available");
            return;
        }

        // 获取玩家名参数
        final String playerName = req.getParameter("player");
        if (playerName == null || playerName.isBlank()) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Missing or empty 'player' parameter");
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
            // 在主线程执行检查
            final boolean isAuthenticated = Bukkit.getScheduler()
                    .callSyncMethod(this.plugin, () -> authMe.isAuthenticated(player))
                    .get();

            this.sendJson(resp, HttpServletResponse.SC_OK,
                    new AuthenticatedResponse(true, isAuthenticated));

        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Thread interrupted");
        } catch (final ExecutionException e) {
            this.plugin.getLogger().log(Level.SEVERE,
                    "Error checking player authentication status: " + playerName, e);
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error checking authentication: " + e.getCause().getMessage());
        }
    }

    // ======================== 数据类型 ========================

    /**
     * 认证状态响应。
     */
    private record AuthenticatedResponse(
            @JsonProperty("success") boolean success,
            @JsonProperty("authenticated") boolean authenticated
    ) {
    }
}