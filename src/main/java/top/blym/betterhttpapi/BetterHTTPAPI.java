package top.blym.betterhttpapi;

import org.bukkit.plugin.java.JavaPlugin;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * BetterHTTPAPI 主插件类。
 * <p>
 * 在 PaperMC 服务器上启动嵌入式 Jetty HTTP 服务，提供 RESTful API
 * 用于远程管理服务器（执行命令、封禁/解封、踢人、广播等）。
 * </p>
 *
 * <p>所有端点均受 API Key 认证和 Host 白名单保护。</p>
 *
 * @author 白鹿原嚒
 * @version 1.0.0
 */
public final class BetterHTTPAPI extends JavaPlugin {

    private Server server;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.configManager = new ConfigManager(this);

        // 启动 Jetty 内嵌服务器
        try {
            this.startServer();
        } catch (final Exception e) {
            this.getLogger().severe("无法启动 HTTP 服务器: " + e.getMessage());
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 打印启动成功横幅
        final String version = this.getDescription().getVersion();
        final int port = this.configManager.getPort();
        final String accessUrl = "http://localhost:" + port + "/api/status";

        this.getServer().getConsoleSender().sendMessage("§a=========================================");
        this.getServer().getConsoleSender().sendMessage("§a  BetterHTTPAPI v" + version + " 已启动！");
        this.getServer().getConsoleSender().sendMessage("§a  作者: 白鹿原嚒");
        this.getServer().getConsoleSender().sendMessage("§a  HTTP API 服务运行在端口: " + port);
        this.getServer().getConsoleSender().sendMessage("§a  访问地址: " + accessUrl);
        this.getServer().getConsoleSender().sendMessage("§a=========================================");
    }

    @Override
    public void onDisable() {
        if (this.server != null) {
            try {
                this.server.stop();
                this.getLogger().info("HTTP API 服务器已停止");
            } catch (final Exception e) {
                this.getLogger().warning("停止 HTTP 服务器时出错: " + e.getMessage());
            }
        }
        this.getLogger().info("BetterHTTPAPI 已禁用");
    }

    /**
     * 获取配置管理器。
     *
     * @return 当前 ConfigManager 实例
     */
    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    /**
     * 创建并启动 Jetty 服务器，注册所有 API Servlet。
     *
     * @throws Exception 如果服务器启动失败
     */
    private void startServer() throws Exception {
        this.server = new Server(this.configManager.getPort());

        final ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");

        // 注册所有端点 Servlet
        this.registerServlet(context, "/api/execute", new ExecuteCommandServlet(this));
        this.registerServlet(context, "/api/ban", new BanServlet(this));
        this.registerServlet(context, "/api/unban", new UnbanServlet(this));
        this.registerServlet(context, "/api/status", new StatusServlet(this));
        this.registerServlet(context, "/api/players", new PlayersServlet(this));
        this.registerServlet(context, "/api/kick", new KickServlet(this));
        this.registerServlet(context, "/api/broadcast", new BroadcastServlet(this));
        this.registerServlet(context, "/api/stop", new StopServlet(this));
        this.registerServlet(context, "/api/restart", new RestartServlet(this));

        this.server.setHandler(context);
        this.server.start();
    }

    /**
     * 向指定上下文注册 Servlet。
     *
     * @param context ServletContextHandler
     * @param path    路径，如 "/api/execute"
     * @param servlet BaseServlet 子类实例
     */
    private void registerServlet(final ServletContextHandler context, final String path,
                                 final BaseServlet servlet) {
        final ServletHolder holder = new ServletHolder(servlet);
        context.addServlet(holder, path);
    }
}
