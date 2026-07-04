package top.blym.betterhttpapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import top.blym.betterhttpapi.service.ResidenceService;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * POST /api/residence/check - 检查位置是否在领地内。
 *
 * <p>请求体示例：</p>
 * <pre>{@code {"world": "world", "x": 100, "y": 64, "z": 200}}</pre>
 *
 * <p>返回示例：</p>
 * <pre>{@code {"success": true, "inResidence": true, "owner": "玩家名"}}</pre>
 *
 * @author 白鹿原嚒
 */
public final class ResidenceServlet extends BaseServlet {

    public ResidenceServlet(final BetterHTTPAPI plugin) {
        super(plugin, "residence");
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        // 解析请求体
        final ResidenceCheckRequest request;
        try {
            request = OBJECT_MAPPER.readValue(req.getReader(), ResidenceCheckRequest.class);
        } catch (final IOException e) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid JSON body: " + e.getMessage());
            return;
        }

        // 验证参数
        final String worldName = request.world();
        if (worldName == null || worldName.isBlank()) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "World name must not be empty");
            return;
        }

        // 在主线程执行领地检查
        try {
            final ResidenceCheckResponse result = Bukkit.getScheduler()
                    .callSyncMethod(this.plugin, () -> {
                        // 获取世界对象
                        final World world = Bukkit.getWorld(worldName);
                        if (world == null) {
                            return new ResidenceCheckResponse(false, false, null,
                                    "World not found: " + worldName);
                        }

                        // 创建位置对象
                        final Location location = new Location(world,
                                request.x(), request.y(), request.z());

                        // 获取 Residence 服务实例
                        final ResidenceService residenceService = ResidenceService.getInstance();

                        // 检查 Residence API 是否可用
                        if (!residenceService.isAvailable()) {
                            return new ResidenceCheckResponse(false, false, null,
                                    "Residence plugin not available");
                        }

                        // 检查位置是否在领地内
                        final boolean inResidence = residenceService.isInResidence(location);

                        // 如果在领地内，获取所有者
                        String owner = null;
                        if (inResidence) {
                            owner = residenceService.getResidenceOwner(location);
                        }

                        return new ResidenceCheckResponse(true, inResidence, owner, null);
                    })
                    .get();

            // 返回结果
            if (result.success()) {
                this.sendJson(resp, HttpServletResponse.SC_OK, result);
            } else {
                this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, result.error());
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Thread interrupted");
        } catch (final ExecutionException e) {
            this.plugin.getLogger().log(Level.SEVERE,
                    "Error checking residence", e);
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to check residence: " + e.getCause().getMessage());
        }
    }

    // ======================== 数据类型 ========================

    /**
     * 领地检查请求对象。
     */
    private record ResidenceCheckRequest(
            @JsonProperty("world") String world,
            @JsonProperty("x") double x,
            @JsonProperty("y") double y,
            @JsonProperty("z") double z) {
    }

    /**
     * 领地检查响应对象。
     */
    private record ResidenceCheckResponse(
            boolean success,
            boolean inResidence,
            String owner,
            String error) {
    }
}