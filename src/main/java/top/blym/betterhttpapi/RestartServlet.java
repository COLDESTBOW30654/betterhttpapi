package top.blym.betterhttpapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * POST /api/restart —— 重启 Minecraft 服务器。
 *
 * <p>可选的请求体示例：</p>
 * <pre>{@code {"message": "服务器即将重启，请稍候重新连接"}}</pre>
 *
 * <p>操作会先在主线程广播消息（如果提供），然后触发 Paper/Spigot 重启。</p>
 *
 * @author 白鹿原嚒
 */
public final class RestartServlet extends BaseServlet {

    public RestartServlet(final BetterHTTPAPI plugin) {
        super(plugin, "restart");
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        final RestartRequest request;
        try {
            request = OBJECT_MAPPER.readValue(req.getReader(), RestartRequest.class);
        } catch (final IOException e) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid JSON body: " + e.getMessage());
            return;
        }

        final String message = request.message() != null && !request.message().isBlank()
                ? request.message() : null;

        try {
            Bukkit.getScheduler().callSyncMethod(this.plugin, () -> {
                if (message != null) {
                    Bukkit.broadcastMessage("§c[BetterHTTPAPI] " + message);
                }
                Bukkit.spigot().restart();
                return null;
            }).get();

            this.sendJson(resp, HttpServletResponse.SC_OK,
                    new RestartResponse(true, "Server restart initiated"));
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Thread interrupted");
        } catch (final ExecutionException e) {
            this.plugin.getLogger().log(Level.SEVERE,
                    "Error restarting server", e);
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Restart failed: " + e.getCause().getMessage());
        }
    }

    // ======================== 数据类型 ========================

    private record RestartRequest(@JsonProperty("message") String message) {
    }

    private record RestartResponse(boolean success, String message) {
    }
}
