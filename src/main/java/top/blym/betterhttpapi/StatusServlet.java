package top.blym.betterhttpapi;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * GET /api/status —— 获取服务器运行状态。
 *
 * <p>返回 TPS、内存使用、在线人数、服务器版本等信息。</p>
 *
 * @author 白鹿原嚒
 */
public final class StatusServlet extends BaseServlet {

    public StatusServlet(final BetterHTTPAPI plugin) {
        super(plugin, "status");
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        try {
            final StatusData status = Bukkit.getScheduler()
                    .callSyncMethod(this.plugin, StatusData::gather)
                    .get();

            this.sendJson(resp, HttpServletResponse.SC_OK, status);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Thread interrupted");
        } catch (final ExecutionException e) {
            this.plugin.getLogger().log(Level.SEVERE,
                    "Error gathering server status", e);
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to gather status: " + e.getCause().getMessage());
        }
    }

    // ======================== 数据类型 ========================

    /**
     * 服务器状态数据对象。所有字段在构造时通过 Bukkit API 采集，
     * 因此构造必须在主线程完成。
     */
    private record StatusData(
            String version,
            int onlinePlayers,
            int maxPlayers,
            String motd,
            TpsData tps,
            MemoryData memory) {

        static StatusData gather() {
            final double[] tpsRaw = Bukkit.getTPS();
            final Runtime runtime = Runtime.getRuntime();
            final long usedMemory = runtime.totalMemory() - runtime.freeMemory();

            return new StatusData(
                    Bukkit.getMinecraftVersion(),
                    Bukkit.getOnlinePlayers().size(),
                    Bukkit.getMaxPlayers(),
                    Bukkit.getMotd(),
                    new TpsData(
                            Math.min(tpsRaw[0], 20.0),
                            Math.min(tpsRaw[1], 20.0),
                            Math.min(tpsRaw[2], 20.0)),
                    new MemoryData(
                            usedMemory / (1024 * 1024),
                            runtime.maxMemory() / (1024 * 1024),
                            runtime.totalMemory() / (1024 * 1024),
                            runtime.freeMemory() / (1024 * 1024)));
        }
    }

    private record TpsData(double tps1m, double tps5m, double tps15m) {
    }

    private record MemoryData(long usedMB, long maxMB, long totalMB, long freeMB) {
    }
}
