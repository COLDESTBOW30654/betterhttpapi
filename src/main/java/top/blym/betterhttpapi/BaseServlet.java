package top.blym.betterhttpapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;

/**
 * 所有 API Servlet 的抽象基类。
 *
 * <p>提供统一的认证、Host 白名单校验、CORS 处理和 JSON 响应工具。
 * 子类只需覆写 {@code doGet()} 或 {@code doPost()} 实现具体业务逻辑。</p>
 *
 * <p>安全流程（在 {@link #service} 中强制执行）：
 * <ol>
 *   <li>设置 CORS 响应头；对 OPTIONS 预检请求直接返回 200</li>
 *   <li>校验 Host 请求头是否在白名单中</li>
 *   <li>校验 X-API-Key 请求头</li>
 *   <li>检查该端点是否在 config.yml 中启用</li>
 *   <li>（可选）记录请求日志</li>
 * </ol>
 * </p>
 *
 * @author 白鹿原嚒
 */
public abstract class BaseServlet extends HttpServlet {

    /** 公用的 JSON 序列化/反序列化器 */
    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final DateTimeFormatter LOG_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 主插件实例 */
    protected final BetterHTTPAPI plugin;
    /** 配置管理器 */
    protected final ConfigManager config;
    /** 端点名称（如 "execute", "status"） */
    private final String endpointName;

    /**
     * 构造基类 Servlet。
     *
     * @param plugin       主插件实例
     * @param endpointName 端点名称，对应 config.yml 中 endpoints 下的键
     */
    protected BaseServlet(final BetterHTTPAPI plugin, final String endpointName) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.endpointName = endpointName;
    }

    /**
     * 获取当前端点名称。
     *
     * @return 端点名称（如 "execute", "status"）
     */
    protected String getEndpointName() {
        return this.endpointName;
    }

    // ======================== 安全流程 ========================

    /**
     * 拦截所有 HTTP 请求，在分发到 doGet/doPost 之前执行安全校验。
     * 此方法声明为 final，防止子类绕过认证。
     */
    @Override
    protected final void service(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {

        // 1. CORS 处理
        this.applyCors(req, resp);

        // OPTIONS 预检请求直接返回
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // 2. Host 白名单校验
        if (!this.isHostAllowed(req)) {
            this.sendError(resp, HttpServletResponse.SC_FORBIDDEN,
                    "Host not allowed: " + req.getHeader("Host"));
            return;
        }

        // 3. API Key 校验
        if (!this.isApiKeyValid(req)) {
            this.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or missing X-API-Key");
            return;
        }

        // 4. 端点开关检查
        if (!this.config.isEndpointEnabled(this.endpointName)) {
            this.sendError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Endpoint disabled: " + this.endpointName);
            return;
        }

        // 5. 日志记录
        if (this.config.isLoggingEnabled()) {
            this.logRequest(req);
        }

        // 通过所有检查后，分发到子类的 doGet/doPost
        super.service(req, resp);
    }

    // ======================== CORS ========================

    /**
     * 设置 CORS 响应头。
     */
    private void applyCors(final HttpServletRequest req, final HttpServletResponse resp) {
        if (!this.config.isCorsEnabled()) {
            return;
        }

        final List<String> allowedOrigins = this.config.getAllowedOrigins();
        final String origin = req.getHeader("Origin");

        if (origin != null && !origin.isEmpty()) {
            if (allowedOrigins.contains("*")) {
                resp.setHeader("Access-Control-Allow-Origin", "*");
            } else if (allowedOrigins.contains(origin)) {
                resp.setHeader("Access-Control-Allow-Origin", origin);
                resp.setHeader("Vary", "Origin");
            }
        }

        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, X-API-Key");
        resp.setHeader("Access-Control-Max-Age", "3600");
    }

    // ======================== 认证 ========================

    /**
     * 校验请求的 Host 头是否在白名单中。
     *
     * @param req HTTP 请求
     * @return true 表示允许
     */
    private boolean isHostAllowed(final HttpServletRequest req) {
        final List<String> allowedHosts = this.config.getAllowedHosts();
        // 白名单为空表示不限制
        if (allowedHosts == null || allowedHosts.isEmpty()) {
            return true;
        }

        final String host = req.getHeader("Host");
        if (host == null) {
            return false;
        }

        // 去除端口部分再比较
        final String hostWithoutPort = host.contains(":") ? host.substring(0, host.indexOf(':')) : host;
        return allowedHosts.contains(host) || allowedHosts.contains(hostWithoutPort);
    }

    /**
     * 校验 X-API-Key 是否匹配。
     *
     * @param req HTTP 请求
     * @return true 表示 API Key 正确
     */
    private boolean isApiKeyValid(final HttpServletRequest req) {
        final String apiKey = req.getHeader("X-API-Key");
        return apiKey != null && apiKey.equals(this.config.getApiKey());
    }

    // ======================== 日志 ========================

    /**
     * 向日志文件中追加一条请求记录。
     */
    private void logRequest(final HttpServletRequest req) {
        try {
            final String logFilePath = this.config.getLogFile();
            final File logFile = new File(logFilePath);
            // 确保父目录存在
            final File parentDir = logFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            final String timestamp = LocalDateTime.now().format(LOG_DATE_FORMAT);
            final String logLine = String.format("[%s] %s %s from %s%n",
                    timestamp,
                    req.getMethod(),
                    req.getRequestURI(),
                    req.getRemoteAddr());

            Files.writeString(logFile.toPath(), logLine,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (final IOException e) {
            this.plugin.getLogger().log(Level.WARNING,
                    "写入 API 日志失败: " + e.getMessage());
        }
    }

    // ======================== 响应工具 ========================

    /**
     * 发送 JSON 成功响应。
     *
     * @param resp   HTTP 响应对象
     * @param status HTTP 状态码
     * @param data   要序列化的对象
     * @throws IOException 如果写入失败
     */
    protected void sendJson(final HttpServletResponse resp, final int status, final Object data)
            throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json; charset=UTF-8");
        final String json = OBJECT_MAPPER.writeValueAsString(data);
        final PrintWriter writer = resp.getWriter();
        writer.print(json);
        writer.flush();
    }

    /**
     * 发送 JSON 错误响应。
     *
     * @param resp    HTTP 响应对象
     * @param status  HTTP 状态码
     * @param message 错误消息
     * @throws IOException 如果写入失败
     */
    protected void sendError(final HttpServletResponse resp, final int status, final String message)
            throws IOException {
        this.sendJson(resp, status, new ErrorResponse(false, message));
    }

    // ======================== 内部类型 ========================

    /**
     * 统一错误响应格式。
     */
    private record ErrorResponse(boolean success, String message) {
    }
}
