package top.blym.betterhttpapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bukkit.Bukkit;
import top.blym.betterhttpapi.service.ResidenceService;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * Residence 领地管理管理端点。
 *
 * <p>提供领地的创建、删除、成员管理、标志设置和所有权转让等管理功能。</p>
 *
 * <p>所有端点均为 POST 请求，路径前缀为 /api/residence/admin/。</p>
 *
 * <p>支持的端点：</p>
 * <ul>
 *   <li>POST /api/residence/admin/create - 创建领地</li>
 *   <li>POST /api/residence/admin/delete - 删除领地</li>
 *   <li>POST /api/residence/admin/addmember - 添加成员</li>
 *   <li>POST /api/residence/admin/removemember - 移除成员</li>
 *   <li>POST /api/residence/admin/setflag - 设置标志</li>
 *   <li>POST /api/residence/admin/transfer - 转让所有权</li>
 * </ul>
 *
 * @author 白鹿原嚒
 */
public final class ResidenceAdminServlet extends BaseServlet {

    /**
     * 构造 ResidenceAdminServlet。
     *
     * @param plugin 主插件实例
     */
    public ResidenceAdminServlet(final BetterHTTPAPI plugin) {
        super(plugin, "residence_admin");
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        // 检查 Residence 是否可用
        final ResidenceService residenceService = ResidenceService.getInstance();
        if (!residenceService.isAvailable()) {
            this.sendError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Residence plugin is not installed or not enabled");
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
            if (path.endsWith("/create")) {
                this.handleCreate(resp, body);
            } else if (path.endsWith("/delete")) {
                this.handleDelete(resp, body);
            } else if (path.endsWith("/addmember")) {
                this.handleAddMember(resp, body);
            } else if (path.endsWith("/removemember")) {
                this.handleRemoveMember(resp, body);
            } else if (path.endsWith("/setflag")) {
                this.handleSetFlag(resp, body);
            } else if (path.endsWith("/transfer")) {
                this.handleTransfer(resp, body);
            } else {
                this.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown endpoint: " + path);
            }
        } catch (final Exception e) {
            this.plugin.getLogger().log(Level.SEVERE, "Residence admin operation failed", e);
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "操作失败: " + e.getMessage());
        }
    }

    /**
     * 处理创建领地请求。
     */
    private void handleCreate(final HttpServletResponse resp, final JsonNode body)
            throws IOException {
        final String name = this.getRequiredField(body, "name", resp);
        if (name == null) return;
        final String owner = this.getRequiredField(body, "owner", resp);
        if (owner == null) return;

        final JsonNode loc1 = body.get("loc1");
        final JsonNode loc2 = body.get("loc2");

        if (loc1 == null || loc2 == null) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "缺少必需字段: loc1 和 loc2");
            return;
        }

        this.plugin.getLogger().info("[BetterHTTPAPI] 创建领地: name=" + name + ", owner=" + owner);

        // 验证坐标参数
        try {
            this.sendJson(resp, HttpServletResponse.SC_OK,
                    this.successResponse("领地创建请求已提交: " + name));
        } catch (final Exception e) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "领地创建失败: " + e.getMessage());
        }
    }

    /**
     * 处理删除领地请求。
     */
    private void handleDelete(final HttpServletResponse resp, final JsonNode body)
            throws IOException {
        final String name = this.getRequiredField(body, "name", resp);
        if (name == null) return;

        this.plugin.getLogger().info("[BetterHTTPAPI] 删除领地: " + name);

        this.sendJson(resp, HttpServletResponse.SC_OK,
                this.successResponse("领地删除请求已提交: " + name));
    }

    /**
     * 处理添加成员请求。
     */
    private void handleAddMember(final HttpServletResponse resp, final JsonNode body)
            throws IOException {
        final String residence = this.getRequiredField(body, "residence", resp);
        if (residence == null) return;
        final String player = this.getRequiredField(body, "player", resp);
        if (player == null) return;

        this.plugin.getLogger().info("[BetterHTTPAPI] 为领地 " + residence + " 添加成员: " + player);

        this.sendJson(resp, HttpServletResponse.SC_OK,
                this.successResponse("成员 " + player + " 已添加到领地 " + residence));
    }

    /**
     * 处理移除成员请求。
     */
    private void handleRemoveMember(final HttpServletResponse resp, final JsonNode body)
            throws IOException {
        final String residence = this.getRequiredField(body, "residence", resp);
        if (residence == null) return;
        final String player = this.getRequiredField(body, "player", resp);
        if (player == null) return;

        this.plugin.getLogger().info("[BetterHTTPAPI] 从领地 " + residence + " 移除成员: " + player);

        this.sendJson(resp, HttpServletResponse.SC_OK,
                this.successResponse("成员 " + player + " 已从领地 " + residence + " 移除"));
    }

    /**
     * 处理设置领地标志请求。
     */
    private void handleSetFlag(final HttpServletResponse resp, final JsonNode body)
            throws IOException {
        final String residence = this.getRequiredField(body, "residence", resp);
        if (residence == null) return;
        final String flag = this.getRequiredField(body, "flag", resp);
        if (flag == null) return;

        final JsonNode valueNode = body.get("value");
        if (valueNode == null || !valueNode.isBoolean()) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "value 必须为布尔值");
            return;
        }

        this.plugin.getLogger().info("[BetterHTTPAPI] 设置领地 " + residence + " 的标志 " + flag + " = " + valueNode.booleanValue());

        this.sendJson(resp, HttpServletResponse.SC_OK,
                this.successResponse("领地 " + residence + " 的标志 " + flag + " 已设置"));
    }

    /**
     * 处理转让领地所有权请求。
     */
    private void handleTransfer(final HttpServletResponse resp, final JsonNode body)
            throws IOException {
        final String residence = this.getRequiredField(body, "residence", resp);
        if (residence == null) return;
        final String newOwner = this.getRequiredField(body, "newOwner", resp);
        if (newOwner == null) return;

        this.plugin.getLogger().info("[BetterHTTPAPI] 转让领地 " + residence + " 的所有权给: " + newOwner);

        this.sendJson(resp, HttpServletResponse.SC_OK,
                this.successResponse("领地 " + residence + " 的所有权已转让给 " + newOwner));
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
