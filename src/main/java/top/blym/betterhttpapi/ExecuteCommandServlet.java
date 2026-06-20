package top.blym.betterhttpapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * POST /api/execute —— 以控制台身份执行任意后台命令。
 *
 * <p>请求体示例：</p>
 * <pre>{@code {"command": "say Hello, world!"}}</pre>
 *
 * @author 白鹿原嚒
 */
public final class ExecuteCommandServlet extends BaseServlet {

    public ExecuteCommandServlet(final BetterHTTPAPI plugin) {
        super(plugin, "execute");
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        final ExecuteRequest request;
        try {
            request = OBJECT_MAPPER.readValue(req.getReader(), ExecuteRequest.class);
        } catch (final IOException e) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid JSON body: " + e.getMessage());
            return;
        }

        final String command = request.command();
        if (command == null || command.isBlank()) {
            this.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Command must not be empty");
            return;
        }

        // 在主线程执行命令
        try {
            final boolean success = Bukkit.getScheduler()
                    .callSyncMethod(this.plugin,
                            () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command))
                    .get();

            this.sendJson(resp, HttpServletResponse.SC_OK,
                    new ExecuteResponse(true, success, "Command dispatched"));
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Thread interrupted");
        } catch (final ExecutionException e) {
            this.plugin.getLogger().log(Level.SEVERE,
                    "Error executing command: " + command, e);
            this.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Command execution failed: " + e.getCause().getMessage());
        }
    }

    // ======================== 数据类型 ========================

    private record ExecuteRequest(@JsonProperty("command") String command) {
    }

    private record ExecuteResponse(boolean success, boolean commandSuccess, String message) {
    }
}
