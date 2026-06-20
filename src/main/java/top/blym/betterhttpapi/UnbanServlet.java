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
 * POST /api/unban —— 解封玩家。
 *
 * <p>请求体示例：</p>
 * <pre>{@code {"player": "Steve"}}</pre>
 *
 * @author 白鹿原嚒
 */
public final class UnbanServlet extends BaseServlet {

    public UnbanServlet(final BetterHTTPAPI plugin) {
        super(plugin, "unban");
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        final UnbanRequest request;
        try {
            request = OBJECT_MAPPER.readValue(req.getReader(), UnbanRequest.class);
        } catch (final IOException e) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid JSON body: " + e.getMessage());
            return;
        }

        if (request.player() == null || request.player().isBlank()) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Player name must not be empty");
            return;
        }

        try {
            Bukkit.getScheduler().callSyncMethod(this.plugin, () -> {
                Bukkit.getBanList(BanList.Type.NAME).pardon(request.player());
                return null;
            }).get();

            this.sendJson(resp, HttpServletResponse.SC_OK,
                    new UnbanResponse(true, "Player " + request.player() + " has been unbanned"));
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Thread interrupted");
        } catch (final ExecutionException e) {
            this.plugin.getLogger().log(Level.SEVERE,
                    "Error unbanning player: " + request.player(), e);
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unban failed: " + e.getCause().getMessage());
        }
    }

    // ======================== 数据类型 ========================

    private record UnbanRequest(@JsonProperty("player") String player) {
    }

    private record UnbanResponse(boolean success, String message) {
    }
}
