package top.blym.betterhttpapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bukkit.Bukkit;
import top.blym.betterhttpapi.service.AuthMeService;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * GET /api/authme/registered - 检查玩家是否已注册。
 *
 * <p>请求参数：player=玩家名</p>
 *
 * <p>响应示例：</p>
 * <pre>{@code {"success": true, "registered": true}}</pre>
 *
 * @author 白鹿原嚒
 */
public final class AuthMeRegisteredServlet extends BaseServlet {

    /**
     * 构造 AuthMeRegistered Servlet。
     *
     * @param plugin 主插件实例
     */
    public AuthMeRegisteredServlet(final BetterHTTPAPI plugin) {
        super(plugin, "authme_registered");
    }

    /**
     * GET /api/authme/registered - 检查玩家是否在 AuthMe 中注册。
     *
     * <p>请求参数：</p>
     * <ul>
     *   <li>player - 玩家名（必填）</li>
     * </ul>
     *
     * <p>响应示例：</p>
     * <pre>{@code {"success": true, "registered": true}}</pre>
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

        try {
            // 在主线程执行检查
            final boolean isRegistered = Bukkit.getScheduler()
                    .callSyncMethod(this.plugin, () -> authMe.isRegistered(playerName))
                    .get();

            this.sendJson(resp, HttpServletResponse.SC_OK,
                    new RegisteredResponse(true, isRegistered));

        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Thread interrupted");
        } catch (final ExecutionException e) {
            this.plugin.getLogger().log(Level.SEVERE,
                    "Error checking player registration status: " + playerName, e);
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error checking registration: " + e.getCause().getMessage());
        }
    }

    // ======================== 数据类型 ========================

    /**
     * 注册状态响应。
     */
    private record RegisteredResponse(
            @JsonProperty("success") boolean success,
            @JsonProperty("registered") boolean registered
    ) {
    }
}