package top.blym.betterhttpapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bukkit.BanList;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * POST /api/ban —— 封禁玩家。
 *
 * <p>请求体示例：</p>
 * <pre>{@code {"player": "Steve", "reason": "Griefing"}}</pre>
 *
 * @author 白鹿原嚒
 */
public final class BanServlet extends BaseServlet {

    public BanServlet(final BetterHTTPAPI plugin) {
        super(plugin, "ban");
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        final BanRequest request;
        try {
            request = OBJECT_MAPPER.readValue(req.getReader(), BanRequest.class);
        } catch (final IOException e) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid JSON body: " + e.getMessage());
            return;
        }

        if (request.player() == null || request.player().isBlank()) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Player name must not be empty");
            return;
        }

        final String reason = request.reason() != null ? request.reason() : "Banned via API";

        try {
            Bukkit.getScheduler().callSyncMethod(this.plugin, () -> {
                Bukkit.getBanList(BanList.Type.NAME).addBan(request.player(), reason, null, null);
                return null;
            }).get();

            this.sendJson(resp, HttpServletResponse.SC_OK,
                    new BanResponse(true, "Player " + request.player() + " has been banned"));
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Thread interrupted");
        } catch (final ExecutionException e) {
            this.plugin.getLogger().log(Level.SEVERE,
                    "Error banning player: " + request.player(), e);
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Ban failed: " + e.getCause().getMessage());
        }
    }

    // ======================== 数据类型 ========================

    private record BanRequest(
            @JsonProperty("player") String player,
            @JsonProperty("reason") String reason) {
    }

    private record BanResponse(boolean success, String message) {
    }
}
