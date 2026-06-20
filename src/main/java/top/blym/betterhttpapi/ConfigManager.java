package top.blym.betterhttpapi;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.logging.Level;

/**
 * 配置管理器 —— 封装对 config.yml 的读写访问。
 *
 * <p>所有功能开关、端口、API Key 等均通过此类获取，
 * 确保代码中只读配置、不直接操作 {@link FileConfiguration}。</p>
 *
 * @author 白鹿原嚒
 */
public final class ConfigManager {

    private final BetterHTTPAPI plugin;

    /**
     * 构造配置管理器。
     *
     * @param plugin 主插件实例
     */
    public ConfigManager(final BetterHTTPAPI plugin) {
        this.plugin = plugin;
    }

    /** 重新加载配置文件。 */
    public void reload() {
        this.plugin.reloadConfig();
        this.plugin.getLogger().log(Level.INFO, "配置文件已重新加载");
    }

    // ---------- 安全 ----------

    /** @return API Key，默认 "change-me-default" */
    public String getApiKey() {
        return this.getConfig().getString("api-key", "change-me-default");
    }

    /** @return 允许访问的 Host 白名单列表 */
    @SuppressWarnings("unchecked")
    public List<String> getAllowedHosts() {
        return (List<String>) this.getConfig().getList("allowed-hosts", List.of());
    }

    // ---------- 服务器 ----------

    /** @return HTTP 监听端口，默认 8080 */
    public int getPort() {
        return this.getConfig().getInt("port", 8080);
    }

    // ---------- 端点开关 ----------

    /**
     * 检查指定端点是否启用。
     *
     * @param endpointName 端点名称（如 "execute", "ban", "status"）
     * @return true 表示该端点已启用
     */
    public boolean isEndpointEnabled(final String endpointName) {
        return this.getConfig().getBoolean("endpoints." + endpointName, false);
    }

    // ---------- 跨域 ----------

    /** @return 是否启用 CORS */
    public boolean isCorsEnabled() {
        return this.getConfig().getBoolean("cors.enabled", false);
    }

    /** @return CORS 允许的源列表 */
    @SuppressWarnings("unchecked")
    public List<String> getAllowedOrigins() {
        return (List<String>) this.getConfig().getList("cors.allowed-origins", List.of());
    }

    // ---------- 日志 ----------

    /** @return 是否启用 API 调用日志 */
    public boolean isLoggingEnabled() {
        return this.getConfig().getBoolean("logging.enabled", true);
    }

    /** @return 日志文件路径 */
    public String getLogFile() {
        return this.getConfig().getString("logging.log-file", "plugins/BetterHTTPAPI/api.log");
    }

    // ---------- 内部 ----------

    private FileConfiguration getConfig() {
        return this.plugin.getConfig();
    }
}
