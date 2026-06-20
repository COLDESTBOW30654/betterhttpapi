package top.blym.betterhttpapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * POST /api/broadcast —— 向全体玩家广播消息。
 *
 * <p>请求体示例：</p>
 * <pre>{@code {"message": "服务器将在 5 分钟后重启！"}}</pre>
 *
 * @author 白鹿原嚒
 */
public final class BroadcastServlet extends BaseServlet {

    public BroadcastServlet(final BetterHTTPAPI plugin) {
        super(plugin, "broadcast");
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        final BroadcastRequest request;
        try {
            request = OBJECT_MAPPER.readValue(req.getReader(), BroadcastRequest.class);
        } catch (final IOException e) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid JSON body: " + e.getMessage());
            return;
        }

        if (request.message() == null || request.message().isBlank()) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Message must not be empty");
            return;
        }

        try {
            final int playerCount = Bukkit.getScheduler().callSyncMethod(this.plugin, () -> {
                Bukkit.broadcastMessage(request.message());
                return Bukkit.getOnlinePlayers().size();
            }).get();

            this.sendJson(resp, HttpServletResponse.SC_OK,
                    new BroadcastResponse(true, "Message broadcast to " + playerCount + " players"));
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Thread interrupted");
        } catch (final ExecutionException e) {
            this.plugin.getLogger().log(Level.SEVERE,
                    "Error broadcasting message", e);
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Broadcast failed: " + e.getCause().getMessage());
        }
    }

    // ======================== 数据类型 ========================

    private record BroadcastRequest(@JsonProperty("message") String message) {
    }

    private record BroadcastResponse(boolean success, String message) {
    }
}
