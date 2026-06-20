package top.blym.betterhttpapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * POST /api/kick —— 踢出在线玩家。
 *
 * <p>请求体示例：</p>
 * <pre>{@code {"player": "Steve", "reason": "You were kicked by admin"}}</pre>
 *
 * @author 白鹿原嚒
 */
public final class KickServlet extends BaseServlet {

    public KickServlet(final BetterHTTPAPI plugin) {
        super(plugin, "kick");
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        final KickRequest request;
        try {
            request = OBJECT_MAPPER.readValue(req.getReader(), KickRequest.class);
        } catch (final IOException e) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid JSON body: " + e.getMessage());
            return;
        }

        if (request.player() == null || request.player().isBlank()) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Player name must not be empty");
            return;
        }

        final String reason = request.reason() != null ? request.reason() : "Kicked via API";

        try {
            final String message = Bukkit.getScheduler().callSyncMethod(this.plugin, () -> {
                final Player player = Bukkit.getPlayerExact(request.player());
                if (player == null) {
                    return "Player not found or not online: " + request.player();
                }
                player.kickPlayer(reason);
                return null; // null means success
            }).get();

            if (message != null) {
                // Player not found
                this.sendError(resp, HttpServletResponse.SC_NOT_FOUND, message);
            } else {
                this.sendJson(resp, HttpServletResponse.SC_OK,
                        new KickResponse(true, "Player " + request.player() + " has been kicked"));
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Thread interrupted");
        } catch (final ExecutionException e) {
            this.plugin.getLogger().log(Level.SEVERE,
                    "Error kicking player: " + request.player(), e);
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Kick failed: " + e.getCause().getMessage());
        }
    }

    // ======================== 数据类型 ========================

    private record KickRequest(
            @JsonProperty("player") String player,
            @JsonProperty("reason") String reason) {
    }

    private record KickResponse(boolean success, String message) {
    }
}
