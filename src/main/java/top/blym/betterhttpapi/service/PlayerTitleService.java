package top.blym.betterhttpapi.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import top.blym.betterhttpapi.PluginAPIManager;

/**
 * PlayerTitle 称号管理服务类。
 *
 * <p>提供与 PlayerTitle 插件集成的称号管理功能，包括获取玩家当前称号、
 * 获取玩家拥有的称号列表、授予称号、移除称号、获取所有可用称号等功能。
 * 所有方法均使用反射调用 PlayerTitle API，确保在插件未安装时安全降级。</p>
 *
 * <p><b>重要说明</b>：</p>
 * <ul>
 *   <li>本服务使用反射调用 PlayerTitle API，因为 PlayerTitle API 类可能在编译时不存在。</li>
 *   <li><b>TODO: 需要确认 PlayerTitle API 的实际方法名和返回值类型。</b></li>
 *   <li>授予称号（grantTitle）和移除称号（removeTitle）操作会在主线程执行，确保线程安全。</li>
 *   <li>其他方法默认为同步调用，如果 PlayerTitle API 需要异步调用，后续可以调整。</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * PlayerTitleService service = PlayerTitleService.getInstance();
 *
 * // 获取玩家当前称号
 * String currentTitle = service.getCurrentTitle(player);
 *
 * // 获取玩家拥有的所有称号
 * List<String> titles = service.getOwnedTitles(player);
 *
 * // 授予玩家称号
 * service.grantTitle(player, "vip_title")
 *     .thenAccept(success -> {
 *         if (success) {
 *             System.out.println("称号授予成功");
 *         }
 *     });
 *
 * // 移除玩家称号
 * service.removeTitle(player, "vip_title");
 *
 * // 获取所有可用称号
 * List<String> allTitles = service.getAllTitles();
 * }</pre>
 *
 * @author 白鹿原嚒
 * @version 1.0.0
 */
public class PlayerTitleService {

    /** 单例实例 */
    private static volatile PlayerTitleService instance;

    /** 日志记录器 */
    private final Logger logger;

    /**
     * 私有构造器（单例模式）。
     *
     * <p>初始化日志记录器。PlayerTitle API 的获取延迟到方法调用时进行。</p>
     */
    private PlayerTitleService() {
        this.logger = Bukkit.getLogger();
    }

    /**
     * 获取单例实例。
     *
     * <p>使用双重检查锁定确保线程安全的单例初始化。</p>
     *
     * @return PlayerTitleService 实例
     */
    public static PlayerTitleService getInstance() {
        if (instance == null) {
            synchronized (PlayerTitleService.class) {
                if (instance == null) {
                    instance = new PlayerTitleService();
                }
            }
        }
        return instance;
    }

    /**
     * 获取玩家当前使用的称号。
     *
     * <p>使用反射调用 PlayerTitle API 的 getCurrentTitle 方法。
     * 如果 API 不可用或发生异常，返回 null。</p>
     *
     * <p><b>TODO: 确认实际方法名和返回值类型</b></p>
     * <p>假设 API 方法签名为：{@code String getCurrentTitle(Player player)}</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * String title = service.getCurrentTitle(player);
     * if (title != null) {
     *     player.sendMessage("你当前的称号: " + title);
     * } else {
     *     player.sendMessage("你还没有设置称号");
     * }
     * }</pre>
     *
     * @param player 玩家对象，不能为 null
     * @return 玩家当前使用的称号名称，如果玩家没有称号或 API 不可用返回 null
     */
    public String getCurrentTitle(final Player player) {
        final Optional<Object> titleApi = PluginAPIManager.getInstance().getPlayerTitleAPI();
        if (titleApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] PlayerTitle API 不可用，无法获取当前称号");
            return null;
        }

        try {
            final Object api = titleApi.get();
            // TODO: 确认实际方法名和返回值类型
            final Method getCurrentTitleMethod = api.getClass().getMethod("getCurrentTitle", Player.class);
            final Object result = getCurrentTitleMethod.invoke(api, player);

            return result != null ? result.toString() : null;
        } catch (NoSuchMethodException e) {
            this.logger.warning("[BetterHTTPAPI] PlayerTitle API 中未找到 getCurrentTitle 方法: " + e.getMessage());
            return null;
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 获取玩家当前称号时发生异常: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取玩家拥有的所有称号。
     *
     * <p>使用反射调用 PlayerTitle API 的 getOwnedTitles 方法。
     * 如果 API 不可用或发生异常，返回空列表。</p>
     *
     * <p><b>TODO: 确认实际方法名和返回值类型</b></p>
     * <p>假设 API 方法签名为：{@code List<String> getOwnedTitles(Player player)}</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * List<String> titles = service.getOwnedTitles(player);
     * player.sendMessage("你拥有 " + titles.size() + " 个称号");
     * titles.forEach(title -> player.sendMessage("- " + title));
     * }</pre>
     *
     * @param player 玩家对象，不能为 null
     * @return 玩家拥有的称号列表，如果 API 不可用或发生异常返回空列表
     */
    public List<String> getOwnedTitles(final Player player) {
        final Optional<Object> titleApi = PluginAPIManager.getInstance().getPlayerTitleAPI();
        if (titleApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] PlayerTitle API 不可用，无法获取玩家称号列表");
            return Collections.emptyList();
        }

        try {
            final Object api = titleApi.get();
            // TODO: 确认实际方法名和返回值类型
            final Method getOwnedTitlesMethod = api.getClass().getMethod("getOwnedTitles", Player.class);
            final Object result = getOwnedTitlesMethod.invoke(api, player);

            if (result instanceof List) {
                @SuppressWarnings("unchecked")
                final List<?> list = (List<?>) result;
                return list.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
            }

            return Collections.emptyList();
        } catch (NoSuchMethodException e) {
            this.logger.warning("[BetterHTTPAPI] PlayerTitle API 中未找到 getOwnedTitles 方法: " + e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 获取玩家称号列表时发生异常: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 授予玩家指定称号。
     *
     * <p>使用反射调用 PlayerTitle API 的 grantTitle 方法。
     * <b>此操作会在主线程执行</b>，以确保线程安全。</p>
     *
     * <p><b>TODO: 确认实际方法名和参数类型</b></p>
     * <p>假设 API 方法签名为：{@code boolean grantTitle(Player player, String titleId)}</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * service.grantTitle(player, "vip_title")
     *     .thenAccept(success -> {
     *         if (success) {
     *             player.sendMessage("称号授予成功！");
     *         } else {
     *             player.sendMessage("称号授予失败！");
     *         }
     *     });
     * }</pre>
     *
     * @param player 玩家对象，不能为 null
     * @param titleId 称号 ID，不能为 null 或空
     * @return CompletableFuture，完成时返回 true 表示授予成功，false 表示失败或 API 不可用
     */
    public CompletableFuture<Boolean> grantTitle(final Player player, final String titleId) {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();

        final Optional<Object> titleApi = PluginAPIManager.getInstance().getPlayerTitleAPI();
        if (titleApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] PlayerTitle API 不可用，无法授予称号");
            future.complete(false);
            return future;
        }

        // 在主线程执行称号授予操作
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"), () -> {
            try {
                final Object api = titleApi.get();
                // TODO: 确认实际方法名和参数类型
                final Method grantTitleMethod = api.getClass().getMethod("grantTitle", Player.class, String.class);
                final Object result = grantTitleMethod.invoke(api, player, titleId);

                if (result instanceof Boolean) {
                    final boolean success = (Boolean) result;
                    if (success) {
                        this.logger.info("[BetterHTTPAPI] 成功为玩家 " + player.getName() + " 授予称号: " + titleId);
                    } else {
                        this.logger.warning("[BetterHTTPAPI] 为玩家 " + player.getName() + " 授予称号失败: " + titleId);
                    }
                    future.complete(success);
                } else {
                    // 如果返回类型不是 boolean，假设操作成功
                    this.logger.info("[BetterHTTPAPI] 为玩家 " + player.getName() + " 授予称号: " + titleId);
                    future.complete(true);
                }
            } catch (NoSuchMethodException e) {
                this.logger.warning("[BetterHTTPAPI] PlayerTitle API 中未找到 grantTitle 方法: " + e.getMessage());
                future.complete(false);
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 授予玩家称号时发生异常: " + e.getMessage());
                future.complete(false);
            }
        });

        return future;
    }

    /**
     * 移除玩家的指定称号。
     *
     * <p>使用反射调用 PlayerTitle API 的 removeTitle 方法。
     * <b>此操作会在主线程执行</b>，以确保线程安全。</p>
     *
     * <p><b>TODO: 确认实际方法名和参数类型</b></p>
     * <p>假设 API 方法签名为：{@code boolean removeTitle(Player player, String titleId)}</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * service.removeTitle(player, "vip_title")
     *     .thenAccept(success -> {
     *         if (success) {
     *             player.sendMessage("称号已移除！");
     *         } else {
     *             player.sendMessage("称号移除失败！");
     *         }
     *     });
     * }</pre>
     *
     * @param player 玩家对象，不能为 null
     * @param titleId 称号 ID，不能为 null 或空
     * @return CompletableFuture，完成时返回 true 表示移除成功，false 表示失败或 API 不可用
     */
    public CompletableFuture<Boolean> removeTitle(final Player player, final String titleId) {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();

        final Optional<Object> titleApi = PluginAPIManager.getInstance().getPlayerTitleAPI();
        if (titleApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] PlayerTitle API 不可用，无法移除称号");
            future.complete(false);
            return future;
        }

        // 在主线程执行称号移除操作
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"), () -> {
            try {
                final Object api = titleApi.get();
                // TODO: 确认实际方法名和参数类型
                final Method removeTitleMethod = api.getClass().getMethod("removeTitle", Player.class, String.class);
                final Object result = removeTitleMethod.invoke(api, player, titleId);

                if (result instanceof Boolean) {
                    final boolean success = (Boolean) result;
                    if (success) {
                        this.logger.info("[BetterHTTPAPI] 成功移除玩家 " + player.getName() + " 的称号: " + titleId);
                    } else {
                        this.logger.warning("[BetterHTTPAPI] 移除玩家 " + player.getName() + " 的称号失败: " + titleId);
                    }
                    future.complete(success);
                } else {
                    // 如果返回类型不是 boolean，假设操作成功
                    this.logger.info("[BetterHTTPAPI] 移除玩家 " + player.getName() + " 的称号: " + titleId);
                    future.complete(true);
                }
            } catch (NoSuchMethodException e) {
                this.logger.warning("[BetterHTTPAPI] PlayerTitle API 中未找到 removeTitle 方法: " + e.getMessage());
                future.complete(false);
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 移除玩家称号时发生异常: " + e.getMessage());
                future.complete(false);
            }
        });

        return future;
    }

    /**
     * 获取所有可用的称号列表。
     *
     * <p>使用反射调用 PlayerTitle API 的 getAllTitles 方法。
     * 如果 API 不可用或发生异常，返回空列表。</p>
     *
     * <p><b>TODO: 确认实际方法名和返回值类型</b></p>
     * <p>假设 API 方法签名为：{@code List<String> getAllTitles()}</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * List<String> allTitles = service.getAllTitles();
     * System.out.println("服务器共有 " + allTitles.size() + " 个称号");
     * allTitles.forEach(title -> System.out.println("- " + title));
     * }</pre>
     *
     * @return 所有可用称号的列表，如果 API 不可用或发生异常返回空列表
     */
    public List<String> getAllTitles() {
        final Optional<Object> titleApi = PluginAPIManager.getInstance().getPlayerTitleAPI();
        if (titleApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] PlayerTitle API 不可用，无法获取称号列表");
            return Collections.emptyList();
        }

        try {
            final Object api = titleApi.get();
            // TODO: 确认实际方法名和返回值类型
            final Method getAllTitlesMethod = api.getClass().getMethod("getAllTitles");
            final Object result = getAllTitlesMethod.invoke(api);

            if (result instanceof List) {
                @SuppressWarnings("unchecked")
                final List<?> list = (List<?>) result;
                return list.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
            }

            return Collections.emptyList();
        } catch (NoSuchMethodException e) {
            this.logger.warning("[BetterHTTPAPI] PlayerTitle API 中未找到 getAllTitles 方法: " + e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 获取所有称号时发生异常: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 检查 PlayerTitle API 是否可用。
     *
     * <p>用于在调用其他方法前检查 API 状态。</p>
     *
     * @return 如果 PlayerTitle API 已加载且可用返回 true，否则返回 false
     */
    public boolean isAvailable() {
        return PluginAPIManager.getInstance().getPlayerTitleAPI().isPresent();
    }

    /**
     * 创建新称号。
     *
     * <p>使用反射调用 PlayerTitle API 的 createTitle 方法。
     * <b>此操作会在主线程执行</b>，以确保线程安全。</p>
     *
     * <p><b>TODO: 确认实际方法名和参数类型</b></p>
     * <p>假设 API 方法签名为：{@code boolean createTitle(String titleId, String displayName, String permission, String description)}</p>
     *
     * @param titleId 称号 ID，不能为 null 或空
     * @param displayName 称号显示名称，不能为 null 或空
     * @param permission 称号所需的权限节点
     * @param description 称号描述
     * @return 如果称号创建成功返回 true，否则返回 false
     */
    public boolean createTitle(final String titleId, final String displayName, final String permission, final String description) {
        final Optional<Object> titleApi = PluginAPIManager.getInstance().getPlayerTitleAPI();
        if (titleApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] PlayerTitle API 不可用，无法创建称号");
            return false;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"), () -> {
            try {
                final Object api = titleApi.get();
                // TODO: 确认实际方法名和参数
                final Method createTitleMethod = api.getClass().getMethod("createTitle", String.class, String.class, String.class, String.class);
                final Object ret = createTitleMethod.invoke(api, titleId, displayName, permission, description);

                if (ret instanceof Boolean) {
                    result[0] = (Boolean) ret;
                } else {
                    result[0] = true;
                }

                if (result[0]) {
                    this.logger.info("[BetterHTTPAPI] 称号创建成功: " + titleId);
                }
            } catch (NoSuchMethodException e) {
                this.logger.warning("[BetterHTTPAPI] PlayerTitle API 未提供 createTitle 方法: " + e.getMessage());
                result[0] = false;
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 创建称号时发生异常: " + e.getMessage());
                result[0] = false;
            } finally {
                latch.countDown();
            }
        });

        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
        return result[0];
    }

    /**
     * 删除指定称号。
     *
     * <p>使用反射调用 PlayerTitle API 的 deleteTitle 方法。
     * <b>此操作会在主线程执行</b>，以确保线程安全。</p>
     *
     * <p><b>TODO: 确认实际方法名和参数类型</b></p>
     * <p>假设 API 方法签名为：{@code boolean deleteTitle(String titleId)}</p>
     *
     * @param titleId 要删除的称号 ID，不能为 null 或空
     * @return 如果称号删除成功返回 true，否则返回 false
     */
    public boolean deleteTitle(final String titleId) {
        final Optional<Object> titleApi = PluginAPIManager.getInstance().getPlayerTitleAPI();
        if (titleApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] PlayerTitle API 不可用，无法删除称号");
            return false;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"), () -> {
            try {
                final Object api = titleApi.get();
                // TODO: 确认实际方法名和参数
                final Method deleteTitleMethod = api.getClass().getMethod("deleteTitle", String.class);
                final Object ret = deleteTitleMethod.invoke(api, titleId);

                if (ret instanceof Boolean) {
                    result[0] = (Boolean) ret;
                } else {
                    result[0] = true;
                }

                if (result[0]) {
                    this.logger.info("[BetterHTTPAPI] 称号删除成功: " + titleId);
                }
            } catch (NoSuchMethodException e) {
                this.logger.warning("[BetterHTTPAPI] PlayerTitle API 未提供 deleteTitle 方法: " + e.getMessage());
                result[0] = false;
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 删除称号时发生异常: " + e.getMessage());
                result[0] = false;
            } finally {
                latch.countDown();
            }
        });

        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
        return result[0];
    }

    /**
     * 获取所有称号及其显示名称的映射。
     *
     * <p>使用反射调用 PlayerTitle API 的方法来获取称号信息映射。
     * <b>此操作会在主线程执行</b>，以确保线程安全。</p>
     *
     * <p><b>TODO: 确认实际方法名和返回值类型</b></p>
     * <p>假设 API 方法签名为：{@code Map<String, String> getAllTitlesMap()}</p>
     *
     * @return Map<String, String>（称号 ID -> 显示名称），如果 API 不可用或发生异常返回空 Map
     */
    public Map<String, String> getAllTitlesWithInfo() {
        final Optional<Object> titleApi = PluginAPIManager.getInstance().getPlayerTitleAPI();
        if (titleApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] PlayerTitle API 不可用，无法获取称号信息");
            return Collections.emptyMap();
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final Map<String, String>[] result = new Map[]{null};

        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"), () -> {
            try {
                final Object api = titleApi.get();
                // TODO: 确认实际方法名和返回值类型
                final Method getAllTitlesMapMethod = api.getClass().getMethod("getAllTitlesMap");
                final Object ret = getAllTitlesMapMethod.invoke(api);

                if (ret instanceof Map) {
                    @SuppressWarnings("unchecked")
                    final Map<String, String> map = (Map<String, String>) ret;
                    result[0] = map;
                } else {
                    result[0] = Collections.emptyMap();
                }
            } catch (NoSuchMethodException e) {
                this.logger.warning("[BetterHTTPAPI] PlayerTitle API 未提供 getAllTitlesMap 方法: " + e.getMessage());
                result[0] = Collections.emptyMap();
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 获取称号信息时发生异常: " + e.getMessage());
                result[0] = Collections.emptyMap();
            } finally {
                latch.countDown();
            }
        });

        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return Collections.emptyMap(); }
        return result[0] != null ? result[0] : Collections.emptyMap();
    }

    /**
     * 从所有玩家撤消指定称号。
     *
     * <p>使用反射调用 PlayerTitle API 的 revokeTitleFromAllPlayers 方法。
     * <b>此操作会在主线程执行</b>，以确保线程安全。</p>
     *
     * <p><b>TODO: 确认实际方法名和参数类型</b></p>
     * <p>假设 API 方法签名为：{@code boolean revokeTitleFromAllPlayers(String titleId)}</p>
     *
     * @param titleId 要撤消的称号 ID，不能为 null 或空
     * @return 如果操作成功返回 true，否则返回 false
     */
    public boolean revokeTitleFromAllPlayers(final String titleId) {
        final Optional<Object> titleApi = PluginAPIManager.getInstance().getPlayerTitleAPI();
        if (titleApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] PlayerTitle API 不可用，无法撤消称号");
            return false;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"), () -> {
            try {
                final Object api = titleApi.get();
                // TODO: 确认实际方法名和参数
                final Method revokeTitleMethod = api.getClass().getMethod("revokeTitleFromAllPlayers", String.class);
                final Object ret = revokeTitleMethod.invoke(api, titleId);

                if (ret instanceof Boolean) {
                    result[0] = (Boolean) ret;
                } else {
                    result[0] = true;
                }

                if (result[0]) {
                    this.logger.info("[BetterHTTPAPI] 成功从所有玩家撤消称号: " + titleId);
                }
            } catch (NoSuchMethodException e) {
                this.logger.warning("[BetterHTTPAPI] PlayerTitle API 未提供 revokeTitleFromAllPlayers 方法: " + e.getMessage());
                result[0] = false;
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 撤消称号时发生异常: " + e.getMessage());
                result[0] = false;
            } finally {
                latch.countDown();
            }
        });

        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
        return result[0];
    }

    /**
     * 设置玩家的当前激活称号。
     *
     * <p>使用反射调用 PlayerTitle API 的 setPlayerTitle 方法。
     * <b>此操作会在主线程执行</b>，以确保线程安全。</p>
     *
     * <p><b>TODO: 确认实际方法名和参数类型</b></p>
     * <p>假设 API 方法签名为：{@code boolean setPlayerTitle(Player player, String titleId)}</p>
     *
     * @param player 玩家对象，不能为 null
     * @param titleId 称号 ID，不能为 null 或空
     * @return 如果设置成功返回 true，否则返回 false
     */
    public boolean setPlayerActiveTitle(final Player player, final String titleId) {
        final Optional<Object> titleApi = PluginAPIManager.getInstance().getPlayerTitleAPI();
        if (titleApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] PlayerTitle API 不可用，无法设置玩家称号");
            return false;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"), () -> {
            try {
                final Object api = titleApi.get();
                // TODO: 确认实际方法名和参数
                final Method setPlayerTitleMethod = api.getClass().getMethod("setPlayerTitle", Player.class, String.class);
                final Object ret = setPlayerTitleMethod.invoke(api, player, titleId);

                if (ret instanceof Boolean) {
                    result[0] = (Boolean) ret;
                } else {
                    result[0] = true;
                }

                if (result[0]) {
                    this.logger.info("[BetterHTTPAPI] 成功为玩家 " + player.getName() + " 设置称号: " + titleId);
                }
            } catch (NoSuchMethodException e) {
                this.logger.warning("[BetterHTTPAPI] PlayerTitle API 未提供 setPlayerTitle 方法: " + e.getMessage());
                result[0] = false;
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 设置玩家称号时发生异常: " + e.getMessage());
                result[0] = false;
            } finally {
                latch.countDown();
            }
        });

        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
        return result[0];
    }
}