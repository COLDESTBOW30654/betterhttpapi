package top.blym.betterhttpapi;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * GET /api/players —— 获取在线玩家详细信息。
 *
 * <p>返回每个在线玩家的 UUID、坐标、血量、游戏模式、IP 和所在世界。</p>
 *
 * @author 白鹿原嚒
 */
public final class PlayersServlet extends BaseServlet {

    public PlayersServlet(final BetterHTTPAPI plugin) {
        super(plugin, "players");
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        try {
            final List<PlayerInfo> players = Bukkit.getScheduler()
                    .callSyncMethod(this.plugin, () -> {
                        // 在 Bukkit 主线程上收集所有玩家数据
                        return Bukkit.getOnlinePlayers().stream()
                                .map(PlayerInfo::fromPlayer)
                                .toList();
                    })
                    .get();

            this.sendJson(resp, HttpServletResponse.SC_OK,
                    new PlayersResponse(true, players.size(), players));
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Thread interrupted");
        } catch (final ExecutionException e) {
            this.plugin.getLogger().log(Level.SEVERE,
                    "Error gathering player data", e);
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to gather player data: " + e.getCause().getMessage());
        }
    }

    // ======================== 数据类型 ========================

    /**
     * 在线玩家响应包装。
     */
    private record PlayersResponse(boolean success, int count, List<PlayerInfo> players) {
    }

    /**
     * 单个在线玩家的详细信息。
     */
    private record PlayerInfo(
            String name,
            UUID uuid,
            String world,
            double x,
            double y,
            double z,
            double health,
            String gameMode,
            String ipAddress) {

        /**
         * 从 Bukkit Player 对象构建 PlayerInfo。必须在主线程调用。
         */
        static PlayerInfo fromPlayer(final Player player) {
            final Location loc = player.getLocation();
            final GameMode gm = player.getGameMode();
            // getAddress() 可能返回 null（极少情况）
            final String ip = player.getAddress() != null
                    ? player.getAddress().getAddress().getHostAddress()
                    : "unknown";

            return new PlayerInfo(
                    player.getName(),
                    player.getUniqueId(),
                    loc.getWorld() != null ? loc.getWorld().getName() : "unknown",
                    round(loc.getX()),
                    round(loc.getY()),
                    round(loc.getZ()),
                    round(player.getHealth()),
                    gm != null ? gm.name() : "UNKNOWN",
                    ip);
        }

        /** 保留两位小数 */
        private static double round(final double value) {
            return Math.round(value * 100.0) / 100.0;
        }
    }
}
