package top.blym.betterhttpapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bukkit.Bukkit;
import top.blym.betterhttpapi.service.MultiverseService;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * Multiverse 世界管理端点。
 *
 * <p>提供两个 HTTP 端点：</p>
 * <ul>
 *   <li>GET /api/multiverse/worlds - 获取所有 Multiverse 世界列表</li>
 *   <li>POST /api/multiverse/world/create - 创建新世界</li>
 * </ul>
 *
 * <p><b>GET /api/multiverse/worlds</b></p>
 * <p>返回格式：</p>
 * <pre>{@code {"success": true, "worlds": ["world1", "world2"]}}</pre>
 *
 * <p><b>POST /api/multiverse/world/create</b></p>
 * <p>请求体示例：</p>
 * <pre>{@code {"name": "world_name", "environment": "NORMAL", "seed": 12345, "gamemode": "SURVIVAL"}}</pre>
 * <p>返回格式：</p>
 * <pre>{@code {"success": true/false, "message": "..."}}</pre>
 *
 * @author 白鹿原嚒
 */
public final class MultiverseServlet extends BaseServlet {

    /**
     * 构造 MultiverseServlet。
     *
     * @param plugin 主插件实例
     */
    public MultiverseServlet(final BetterHTTPAPI plugin) {
        super(plugin, "multiverse");
    }

    /**
     * GET /api/multiverse/worlds - 获取所有世界列表。
     *
     * <p>从 Multiverse-Core 获取所有已注册的世界名称列表。
     * 如果 Multiverse-Core 未安装或不可用，返回空列表。</p>
     *
     * @param req  HTTP 请求
     * @param resp HTTP 响应
     * @throws IOException 如果写入响应失败
     */
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        // 检查 Multiverse-Core 是否可用
        final MultiverseService service = MultiverseService.getInstance();
        if (!service.isAvailable()) {
            this.sendError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Multiverse-Core is not installed or not enabled");
            return;
        }

        try {
            // 在主线程获取世界列表
            final List<String> worlds = Bukkit.getScheduler()
                    .callSyncMethod(this.plugin, service::getWorldNames)
                    .get();

            this.sendJson(resp, HttpServletResponse.SC_OK,
                    new WorldsResponse(true, worlds));
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Thread interrupted");
        } catch (final ExecutionException e) {
            this.plugin.getLogger().log(Level.SEVERE,
                    "Error getting world list", e);
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to get world list: " + e.getCause().getMessage());
        }
    }

    /**
     * POST /api/multiverse/world/create - 创建新世界。
     *
     * <p>使用 Multiverse-Core 创建新世界。支持配置环境类型、种子和游戏模式。</p>
     *
     * <p>请求参数：</p>
     * <ul>
     *   <li>name - 世界名称（必填）</li>
     *   <li>environment - 环境类型（NORMAL, NETHER, THE_END），可选，默认 NORMAL</li>
     *   <li>seed - 世界种子，可选，默认随机</li>
     *   <li>gamemode - 游戏模式（SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR），可选，默认 SURVIVAL</li>
     * </ul>
     *
     * @param req  HTTP 请求
     * @param resp HTTP 响应
     * @throws IOException 如果写入响应失败
     */
    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        // 检查 Multiverse-Core 是否可用
        final MultiverseService service = MultiverseService.getInstance();
        if (!service.isAvailable()) {
            this.sendError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Multiverse-Core is not installed or not enabled");
            return;
        }

        // 解析请求体
        final CreateWorldRequest request;
        try {
            request = OBJECT_MAPPER.readValue(req.getReader(), CreateWorldRequest.class);
        } catch (final IOException e) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid JSON body: " + e.getMessage());
            return;
        }

        // 验证世界名称
        final String name = request.name();
        if (name == null || name.isBlank()) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "World name must not be empty");
            return;
        }

        // 获取可选参数，提供默认值
        final String environment = request.environment() != null ? request.environment() : "NORMAL";
        final long seed = request.seed() != null ? request.seed() : System.currentTimeMillis();
        final String gamemode = request.gamemode() != null ? request.gamemode() : "SURVIVAL";

        // 异步创建世界
        final CompletableFuture<Boolean> future = service.createWorld(name, environment, seed, gamemode);

        try {
            final Boolean success = future.get();
            if (success != null && success) {
                this.sendJson(resp, HttpServletResponse.SC_OK,
                        new CreateWorldResponse(true, "World created successfully: " + name));
            } else {
                this.sendJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        new CreateWorldResponse(false, "Failed to create world: " + name));
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Thread interrupted");
        } catch (final ExecutionException e) {
            this.plugin.getLogger().log(Level.SEVERE,
                    "Error creating world: " + name, e);
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "World creation failed: " + e.getCause().getMessage());
        }
    }

    // ======================== 数据类型 ========================

    /**
     * 世界列表响应。
     */
    private record WorldsResponse(boolean success, List<String> worlds) {
    }

    /**
     * 创建世界请求。
     */
    private record CreateWorldRequest(
            @JsonProperty("name") String name,
            @JsonProperty("environment") String environment,
            @JsonProperty("seed") Long seed,
            @JsonProperty("gamemode") String gamemode) {
    }

    /**
     * 创建世界响应。
     */
    private record CreateWorldResponse(boolean success, String message) {
    }
}