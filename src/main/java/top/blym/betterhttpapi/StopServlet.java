package top.blym.betterhttpapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * POST /api/stop —— 关闭 Minecraft 服务器。
 *
 * <p>可选的请求体示例：</p>
 * <pre>{@code {"message": "服务器即将关闭，请做好准备"}}</pre>
 *
 * <p>操作会先在主线程广播消息（如果提供），然后关闭服务器。</p>
 *
 * @author 白鹿原嚒
 */
public final class StopServlet extends BaseServlet {

    public StopServlet(final BetterHTTPAPI plugin) {
        super(plugin, "stop");
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        final StopRequest request;
        try {
            request = OBJECT_MAPPER.readValue(req.getReader(), StopRequest.class);
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
                Bukkit.shutdown();
                return null;
            }).get();

            // 注意：shutdown() 是异步的，HTTP 响应可能在服务器完全关闭前就发出
            this.sendJson(resp, HttpServletResponse.SC_OK,
                    new StopResponse(true, "Server shutdown initiated"));
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Thread interrupted");
        } catch (final ExecutionException e) {
            this.plugin.getLogger().log(Level.SEVERE,
                    "Error shutting down server", e);
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Shutdown failed: " + e.getCause().getMessage());
        }
    }

    // ======================== 数据类型 ========================

    private record StopRequest(@JsonProperty("message") String message) {
    }

    private record StopResponse(boolean success, String message) {
    }
}
