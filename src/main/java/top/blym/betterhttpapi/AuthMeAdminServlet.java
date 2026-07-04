package top.blym.betterhttpapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bukkit.Bukkit;
import top.blym.betterhttpapi.service.AuthMeService;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * AuthMe 认证管理端点。
 *
 * <p>提供 AuthMe 玩家账户的管理功能，包括删除账户、修改密码、强制登录和设置邮箱等操作。</p>
 *
 * <p>所有端点均为 POST 请求，路径前缀为 /api/authme/admin/。</p>
 *
 * <p>支持的端点：</p>
 * <ul>
 *   <li>POST /api/authme/admin/unregister - 删除玩家账户</li>
 *   <li>POST /api/authme/admin/changepassword - 修改玩家密码</li>
 *   <li>POST /api/authme/admin/forcelogin - 强制玩家登录</li>
 *   <li>POST /api/authme/admin/email - 设置玩家邮箱</li>
 * </ul>
 *
 * @author 白鹿原嚒
 */
public final class AuthMeAdminServlet extends BaseServlet {

    /**
     * 构造 AuthMeAdminServlet。
     *
     * @param plugin 主插件实例
     */
    public AuthMeAdminServlet(final BetterHTTPAPI plugin) {
        super(plugin, "authme_admin");
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        // 检查 AuthMe 是否可用
        final AuthMeService authMe = AuthMeService.getInstance();
        if (!authMe.isAvailable()) {
            this.sendError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "AuthMe API is not available");
            return;
        }

        // 解析请求体
        final JsonNode body;
        try {
            body = OBJECT_MAPPER.readTree(req.getReader());
        } catch (final IOException e) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid JSON body: " + e.getMessage());
            return;
        }

        final String path = req.getRequestURI();

        try {
            if (path.endsWith("/unregister")) {
                this.handleUnregister(resp, body);
            } else if (path.endsWith("/changepassword")) {
                this.handleChangePassword(resp, body);
            } else if (path.endsWith("/forcelogin")) {
                this.handleForceLogin(resp, body);
            } else if (path.endsWith("/email")) {
                this.handleEmail(resp, body);
            } else {
                this.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown endpoint: " + path);
            }
        } catch (final Exception e) {
            this.plugin.getLogger().log(Level.SEVERE, "AuthMe admin operation failed", e);
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "操作失败: " + e.getMessage());
        }
    }

    /**
     * 处理删除玩家账户请求。
     */
    private void handleUnregister(final HttpServletResponse resp, final JsonNode body)
            throws IOException {
        final String player = this.getRequiredField(body, "player", resp);
        if (player == null) return;

        this.plugin.getLogger().info("[BetterHTTPAPI] 删除玩家账户: " + player);

        try {
            final boolean success = Bukkit.getScheduler().callSyncMethod(this.plugin, () -> {
                // 使用反射调用 AuthMe API 的 unregister 方法
                final AuthMeService service = AuthMeService.getInstance();
                try {
                    final java.lang.reflect.Method unregisterMethod =
                            service.getClass().getMethod("unregisterPlayer", String.class);
                    return (boolean) unregisterMethod.invoke(service, player);
                } catch (final NoSuchMethodException e) {
                    this.plugin.getLogger().warning("[BetterHTTPAPI] AuthMe API 未提供 unregisterPlayer 方法");
                    return false;
                } catch (final Exception e) {
                    this.plugin.getLogger().warning("[BetterHTTPAPI] 调用 unregisterPlayer 失败: " + e.getMessage());
                    return false;
                }
            }).get();

            if (success) {
                this.sendJson(resp, HttpServletResponse.SC_OK,
                        this.successResponse("玩家 " + player + " 账户已删除"));
            } else {
                this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "删除玩家账户失败: " + player);
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "线程中断");
        } catch (final ExecutionException e) {
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "删除账户失败: " + e.getCause().getMessage());
        }
    }

    /**
     * 处理修改密码请求。
     */
    private void handleChangePassword(final HttpServletResponse resp, final JsonNode body)
            throws IOException {
        final String player = this.getRequiredField(body, "player", resp);
        if (player == null) return;
        final String password = this.getRequiredField(body, "password", resp);
        if (password == null) return;

        this.plugin.getLogger().info("[BetterHTTPAPI] 修改玩家 " + player + " 的密码");

        try {
            final boolean success = Bukkit.getScheduler().callSyncMethod(this.plugin, () -> {
                final AuthMeService service = AuthMeService.getInstance();
                try {
                    final java.lang.reflect.Method changePasswordMethod =
                            service.getClass().getMethod("changePassword", String.class, String.class);
                    return (boolean) changePasswordMethod.invoke(service, player, password);
                } catch (final NoSuchMethodException e) {
                    this.plugin.getLogger().warning("[BetterHTTPAPI] AuthMe API 未提供 changePassword 方法");
                    return false;
                } catch (final Exception e) {
                    this.plugin.getLogger().warning("[BetterHTTPAPI] 调用 changePassword 失败: " + e.getMessage());
                    return false;
                }
            }).get();

            if (success) {
                this.sendJson(resp, HttpServletResponse.SC_OK,
                        this.successResponse("玩家 " + player + " 密码修改成功"));
            } else {
                this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "修改密码失败: " + player);
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "线程中断");
        } catch (final ExecutionException e) {
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "修改密码失败: " + e.getCause().getMessage());
        }
    }

    /**
     * 处理强制登录请求。
     */
    private void handleForceLogin(final HttpServletResponse resp, final JsonNode body)
            throws IOException {
        final String player = this.getRequiredField(body, "player", resp);
        if (player == null) return;

        this.plugin.getLogger().info("[BetterHTTPAPI] 强制玩家登录: " + player);

        try {
            final boolean success = Bukkit.getScheduler().callSyncMethod(this.plugin, () -> {
                final org.bukkit.entity.Player bukkitPlayer = Bukkit.getPlayerExact(player);
                if (bukkitPlayer == null) {
                    return false;
                }
                try {
                    final java.lang.reflect.Method forceLoginMethod =
                            fr.xephi.authme.api.v3.AuthMeApi.class.getMethod("forceLogin", org.bukkit.entity.Player.class);
                    final fr.xephi.authme.api.v3.AuthMeApi api =
                            fr.xephi.authme.api.v3.AuthMeApi.getInstance();
                    forceLoginMethod.invoke(api, bukkitPlayer);
                    return true;
                } catch (final Exception e) {
                    this.plugin.getLogger().warning("[BetterHTTPAPI] 强制登录失败: " + e.getMessage());
                    return false;
                }
            }).get();

            if (success) {
                this.sendJson(resp, HttpServletResponse.SC_OK,
                        this.successResponse("玩家 " + player + " 已强制登录"));
            } else {
                this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "强制登录失败，玩家不在线或 API 不可用");
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "线程中断");
        } catch (final ExecutionException e) {
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "强制登录失败: " + e.getCause().getMessage());
        }
    }

    /**
     * 处理设置邮箱请求。
     */
    private void handleEmail(final HttpServletResponse resp, final JsonNode body)
            throws IOException {
        final String player = this.getRequiredField(body, "player", resp);
        if (player == null) return;
        final String email = this.getRequiredField(body, "email", resp);
        if (email == null) return;

        this.plugin.getLogger().info("[BetterHTTPAPI] 设置玩家 " + player + " 的邮箱为: " + email);

        try {
            final boolean success = Bukkit.getScheduler().callSyncMethod(this.plugin, () -> {
                final AuthMeService service = AuthMeService.getInstance();
                try {
                    final java.lang.reflect.Method setEmailMethod =
                            service.getClass().getMethod("setEmail", String.class, String.class);
                    return (boolean) setEmailMethod.invoke(service, player, email);
                } catch (final NoSuchMethodException e) {
                    this.plugin.getLogger().warning("[BetterHTTPAPI] AuthMe API 未提供 setEmail 方法");
                    return false;
                } catch (final Exception e) {
                    this.plugin.getLogger().warning("[BetterHTTPAPI] 调用 setEmail 失败: " + e.getMessage());
                    return false;
                }
            }).get();

            if (success) {
                this.sendJson(resp, HttpServletResponse.SC_OK,
                        this.successResponse("玩家 " + player + " 邮箱设置成功"));
            } else {
                this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "设置邮箱失败: " + player);
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "线程中断");
        } catch (final ExecutionException e) {
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "设置邮箱失败: " + e.getCause().getMessage());
        }
    }

    /**
     * 从 JsonNode 中获取必需的字符串字段。
     *
     * @param body  JSON 节点
     * @param field 字段名
     * @param resp  HTTP 响应
     * @return 字段值，如果字段无效则返回 null
     * @throws IOException 如果写入错误响应失败
     */
    private String getRequiredField(final JsonNode body, final String field,
                                    final HttpServletResponse resp) throws IOException {
        final JsonNode node = body.get(field);
        if (node == null || node.isNull() || node.asText().isBlank()) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "缺少必需字段: " + field);
            return null;
        }
        return node.asText();
    }

    /**
     * 创建成功响应对象。
     *
     * @param message 成功消息
     * @return 响应 JSON 节点
     */
    private ObjectNode successResponse(final String message) {
        final ObjectNode node = OBJECT_MAPPER.createObjectNode();
        node.put("success", true);
        node.put("message", message);
        return node;
    }
}
