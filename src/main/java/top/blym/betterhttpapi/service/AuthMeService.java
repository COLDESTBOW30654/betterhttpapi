package top.blym.betterhttpapi.service;

import fr.xephi.authme.api.v3.AuthMeApi;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * AuthMe 认证服务类
 *
 * <p>提供与 AuthMe 插件集成的玩家认证管理功能,包括检查玩家注册状态、
 * 认证状态、注册新玩家、强制注销等功能。所有方法均处理异常情况,
 * 在 AuthMe API 不可用时返回安全的默认值。</p>
 *
 * <p><b>线程安全</b>:registerPlayer 和 forceLogout 方法必须在主线程执行,
 * 已通过 Bukkit.getScheduler().runTask() 进行包装。</p>
 *
 * <p>使用示例:</p>
 * <pre>{@code
 * AuthMeService service = AuthMeService.getInstance();
 *
 * // 检查玩家是否已注册
 * boolean registered = service.isRegistered("Steve");
 *
 * // 检查玩家是否已登录
 * boolean authenticated = service.isAuthenticated(player);
 *
 * // 注册新玩家
 * boolean success = service.registerPlayer("Steve", "password123");
 *
 * // 强制玩家注销
 * boolean loggedOut = service.forceLogout(player);
 * }</pre>
 *
 * @author 白鹿原嚒
 * @version 1.0.0
 */
public class AuthMeService {

    /** 单例实例 */
    private static volatile AuthMeService instance;

    /** AuthMe API 实例 */
    private final AuthMeApi authMeApi;

    /** 日志记录器 */
    private final Logger logger;

    /**
     * 私有构造器（单例模式）
     *
     * <p>初始化 AuthMe API 实例,如果 API 不可用则设置为 null。</p>
     */
    private AuthMeService() {
        this.logger = Bukkit.getLogger();
        AuthMeApi api = null;

        try {
            // 使用反射获取 AuthMeApi 单例实例
            final Class<?> authMeApiClass = Class.forName("fr.xephi.authme.api.v3.AuthMeApi");
            final java.lang.reflect.Method getInstanceMethod = authMeApiClass.getMethod("getInstance");
            final Object apiInstance = getInstanceMethod.invoke(null);
            if (apiInstance instanceof AuthMeApi) {
                api = (AuthMeApi) apiInstance;
                this.logger.info("[BetterHTTPAPI] AuthMe API 已成功加载");
            }
        } catch (ClassNotFoundException e) {
            this.logger.warning("[BetterHTTPAPI] AuthMe 插件未安装,认证功能将返回默认值");
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] AuthMe API 不可用,认证功能将返回默认值: " + e.getMessage());
        }

        this.authMeApi = api;
    }

    /**
     * 获取单例实例
     *
     * <p>使用双重检查锁定确保线程安全的单例初始化。</p>
     *
     * @return AuthMeService 实例
     */
    public static AuthMeService getInstance() {
        if (instance == null) {
            synchronized (AuthMeService.class) {
                if (instance == null) {
                    instance = new AuthMeService();
                }
            }
        }
        return instance;
    }

    /**
     * 检查玩家是否已注册（通过玩家名称）
     *
     * <p>同步方法,检查指定名称的玩家是否在 AuthMe 数据库中注册。</p>
     *
     * <p>使用示例:</p>
     * <pre>{@code
     * boolean registered = service.isRegistered("Steve");
     * if (registered) {
     *     System.out.println("该玩家已注册");
     * }
     * }</pre>
     *
     * @param playerName 玩家名称,不能为 null
     * @return 如果玩家已注册返回 true,否则返回 false。
     *         如果 AuthMe API 不可用或发生异常,返回 false
     */
    public boolean isRegistered(final String playerName) {
        if (this.authMeApi == null) {
            return false;
        }

        try {
            return this.authMeApi.isRegistered(playerName);
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 检查玩家注册状态时发生异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 检查玩家是否已注册（通过 UUID）
     *
     * <p>同步方法,先通过 UUID 获取玩家名称,再检查是否注册。
     * 如果玩家当前在线,则从在线玩家列表获取名称;
     * 如果玩家不在线,则尝试从缓存获取。</p>
     *
     * <p>使用示例:</p>
     * <pre>{@code
     * boolean registered = service.isRegistered(playerUUID);
     * if (registered) {
     *     System.out.println("该玩家已注册");
     * }
     * }</pre>
     *
     * @param uuid 玩家的 UUID,不能为 null
     * @return 如果玩家已注册返回 true,否则返回 false。
     *         如果 AuthMe API 不可用或发生异常,返回 false
     */
    public boolean isRegistered(final UUID uuid) {
        if (this.authMeApi == null) {
            return false;
        }

        try {
            // 尝试从在线玩家获取名称
            final Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                return isRegistered(player.getName());
            }

            // 如果玩家不在线,从缓存获取名称
            // 注意: AuthMe API 可能不提供通过 UUID 直接查询的方法
            // 这里使用反射或假设有相关方法
            final org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if (offlinePlayer.getName() != null) {
                return isRegistered(offlinePlayer.getName());
            }

            return false;
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 检查玩家注册状态(UUID)时发生异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 检查玩家是否已认证（已登录）
     *
     * <p>同步方法,检查指定玩家是否已完成登录认证。</p>
     *
     * <p>使用示例:</p>
     * <pre>{@code
     * boolean authenticated = service.isAuthenticated(player);
     * if (authenticated) {
     *     player.sendMessage("你已经登录");
     * }
     * }</pre>
     *
     * @param player 玩家对象,不能为 null
     * @return 如果玩家已认证返回 true,否则返回 false。
     *         如果 AuthMe API 不可用或发生异常,返回 false
     */
    public boolean isAuthenticated(final Player player) {
        if (this.authMeApi == null) {
            return false;
        }

        try {
            return this.authMeApi.isAuthenticated(player);
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 检查玩家认证状态时发生异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 注册新玩家
     *
     * <p><b>必须在主线程执行</b>:此方法已通过 Bukkit.getScheduler().runTask() 包装,
     * 确保在主线程执行注册操作。此方法会同步等待主线程任务完成（最多 5 秒）。</p>
     *
     * <p>使用反射调用 AuthMe API 的 registerPlayer 方法。</p>
     *
     * <p><b>TODO</b>: 确认 AuthMe API 的 registerPlayer 方法签名和返回值</p>
     *
     * <p>使用示例:</p>
     * <pre>{@code
     * boolean success = service.registerPlayer("Steve", "password123");
     * if (success) {
     *     System.out.println("注册成功");
     * }
     * }</pre>
     *
     * @param playerName 玩家名称,不能为 null
     * @param password 密码,不能为 null
     * @return 如果注册成功返回 true,否则返回 false。
     *         如果 AuthMe API 不可用或注册失败,返回 false
     */
    public boolean registerPlayer(final String playerName, final String password) {
        if (this.authMeApi == null) {
            return false;
        }

        // 使用 CountDownLatch 等待主线程任务完成
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        // 必须在主线程执行
        Bukkit.getScheduler().runTask(
            Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"),
            () -> {
                try {
                    // TODO: 确认 AuthMe API 的 registerPlayer 方法签名和返回值
                    // 使用反射调用 registerPlayer 方法
                    final java.lang.reflect.Method registerMethod = this.authMeApi.getClass()
                        .getMethod("registerPlayer", String.class, String.class);
                    final Object returnValue = registerMethod.invoke(this.authMeApi, playerName, password);

                    // 假设返回 boolean
                    if (returnValue instanceof Boolean) {
                        result[0] = (Boolean) returnValue;
                    } else {
                        // 如果返回值不是 boolean,假设注册成功(没有抛出异常)
                        result[0] = true;
                    }
                } catch (NoSuchMethodException e) {
                    this.logger.warning("[BetterHTTPAPI] AuthMe API 未提供 registerPlayer 方法: " + e.getMessage());
                    result[0] = false;
                } catch (Exception e) {
                    this.logger.warning("[BetterHTTPAPI] 注册玩家时发生异常: " + e.getMessage());
                    result[0] = false;
                } finally {
                    latch.countDown();
                }
            }
        );

        // 等待主线程任务完成（最多 5 秒）
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            this.logger.warning("[BetterHTTPAPI] 等待注册任务完成时被中断: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }

        return result[0];
    }

    /**
     * 强制玩家注销
     *
     * <p><b>必须在主线程执行</b>:此方法已通过 Bukkit.getScheduler().runTask() 包装,
     * 确保在主线程执行注销操作。此方法会同步等待主线程任务完成（最多 5 秒）。</p>
     *
     * <p>使用 AuthMe API 的 forceLogout 方法强制玩家登出。</p>
     *
     * <p>使用示例:</p>
     * <pre>{@code
     * boolean success = service.forceLogout(player);
     * if (success) {
     *     System.out.println("玩家已被强制注销");
     * }
     * }</pre>
     *
     * @param player 玩家对象,不能为 null
     * @return 如果注销成功返回 true,否则返回 false。
     *         如果 AuthMe API 不可用或注销失败,返回 false
     */
    public boolean forceLogout(final Player player) {
        if (this.authMeApi == null) {
            return false;
        }

        // 使用 CountDownLatch 等待主线程任务完成
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        // 必须在主线程执行
        Bukkit.getScheduler().runTask(
            Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"),
            () -> {
                try {
                    this.authMeApi.forceLogout(player);
                    result[0] = true;
                } catch (Exception e) {
                    this.logger.warning("[BetterHTTPAPI] 强制玩家注销时发生异常: " + e.getMessage());
                    result[0] = false;
                } finally {
                    latch.countDown();
                }
            }
        );

        // 等待主线程任务完成（最多 5 秒）
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            this.logger.warning("[BetterHTTPAPI] 等待注销任务完成时被中断: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }

        return result[0];
    }

    /**
     * 获取玩家注册时间
     *
     * <p>获取玩家在 AuthMe 数据库中的注册时间戳。</p>
     *
     * <p><b>TODO</b>: 确认 AuthMe API 是否提供获取注册时间的方法</p>
     *
     * <p>使用示例:</p>
     * <pre>{@code
     * Optional<Long> timestamp = service.getRegistrationDate("Steve");
     * if (timestamp.isPresent()) {
     *     System.out.println("注册时间: " + new Date(timestamp.get()));
     * }
     * }</pre>
     *
     * @param playerName 玩家名称,不能为 null
     * @return 包含注册时间戳的 Optional（毫秒）,如果无法获取则返回 Optional.empty()。
     *         如果 AuthMe API 不可用或方法不存在,返回 Optional.empty()
     */
    public Optional<Long> getRegistrationDate(final String playerName) {
        if (this.authMeApi == null) {
            return Optional.empty();
        }

        try {
            // TODO: 确认 AuthMe API 是否提供获取注册时间的方法
            // 使用反射尝试调用 getRegistrationTimestamp 或类似方法
            final java.lang.reflect.Method getTimestampMethod = this.authMeApi.getClass()
                .getMethod("getRegistrationTimestamp", String.class);
            final Object returnValue = getTimestampMethod.invoke(this.authMeApi, playerName);

            if (returnValue instanceof Long) {
                return Optional.of((Long) returnValue);
            }

            return Optional.empty();
        } catch (NoSuchMethodException e) {
            this.logger.warning("[BetterHTTPAPI] AuthMe API 未提供获取注册时间的方法: " + e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 获取玩家注册时间时发生异常: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 检查 AuthMe API 是否可用
     *
     * <p>用于在调用其他方法前检查 API 状态。</p>
     *
     * <p>使用示例:</p>
     * <pre>{@code
     * if (service.isAvailable()) {
     *     boolean registered = service.isRegistered("Steve");
     * }
     * }</pre>
     *
     * @return 如果 AuthMe API 已加载且可用返回 true,否则返回 false
     */
    public boolean isAvailable() {
        return this.authMeApi != null;
    }

    /**
     * 删除玩家的注册账户。
     *
     * <p><b>必须在主线程执行</b>: 此方法已通过 Bukkit.getScheduler().runTask() 包装，
     * 确保在主线程执行注销操作。此方法会同步等待主线程任务完成（最多 5 秒）。</p>
     *
     * <p>使用 AuthMe API 的 forceUnregister 或类似方法删除玩家账户。</p>
     *
     * <p><b>TODO</b>: 确认 AuthMe API 中删除玩家账户的方法名</p>
     *
     * <p>使用示例:</p>
     * <pre>{@code
     * boolean success = service.unregisterPlayer("Steve");
     * if (success) {
     *     System.out.println("玩家账户已删除");
     * }
     * }</pre>
     *
     * @param playerName 玩家名称, 不能为 null
     * @return 如果删除成功返回 true, 否则返回 false。
     *         如果 AuthMe API 不可用或删除失败, 返回 false
     */
    public boolean unregisterPlayer(final String playerName) {
        if (this.authMeApi == null) {
            return false;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        Bukkit.getScheduler().runTask(
            Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"),
            () -> {
                try {
                    // TODO: 确认 AuthMe API 中删除玩家账户的方法名
                    // 可能为 forceUnregister(Player) 或 unregister(String)
                    final java.lang.reflect.Method unregisterMethod = this.authMeApi.getClass()
                        .getMethod("forceUnregister", String.class);
                    unregisterMethod.invoke(this.authMeApi, playerName);
                    result[0] = true;
                    this.logger.info("[BetterHTTPAPI] 玩家账户已删除: " + playerName);
                } catch (NoSuchMethodException e) {
                    // 尝试其他方法名
                    try {
                        final java.lang.reflect.Method unregisterMethod2 = this.authMeApi.getClass()
                            .getMethod("unregister", String.class);
                        unregisterMethod2.invoke(this.authMeApi, playerName);
                        result[0] = true;
                        this.logger.info("[BetterHTTPAPI] 玩家账户已删除: " + playerName);
                    } catch (Exception ex) {
                        this.logger.warning("[BetterHTTPAPI] AuthMe API 未提供 unregister 方法: " + e.getMessage());
                        result[0] = false;
                    }
                } catch (Exception e) {
                    this.logger.warning("[BetterHTTPAPI] 删除玩家账户时发生异常: " + e.getMessage());
                    result[0] = false;
                } finally {
                    latch.countDown();
                }
            }
        );

        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
        return result[0];
    }

    /**
     * 修改玩家密码。
     *
     * <p><b>必须在主线程执行</b>: 此方法已通过 Bukkit.getScheduler().runTask() 包装，
     * 确保在主线程执行修改密码操作。此方法会同步等待主线程任务完成（最多 5 秒）。</p>
     *
     * <p>使用反射调用 AuthMe API 的 changePassword 或类似方法。</p>
     *
     * <p><b>TODO</b>: 确认 AuthMe API 中修改密码的方法名和签名</p>
     *
     * <p>使用示例:</p>
     * <pre>{@code
     * boolean success = service.changePassword("Steve", "newPassword123");
     * if (success) {
     *     System.out.println("密码已修改");
     * }
     * }</pre>
     *
     * @param playerName 玩家名称, 不能为 null
     * @param newPassword 新密码, 不能为 null
     * @return 如果修改成功返回 true, 否则返回 false。
     *         如果 AuthMe API 不可用或修改失败, 返回 false
     */
    public boolean changePassword(final String playerName, final String newPassword) {
        if (this.authMeApi == null) {
            return false;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        Bukkit.getScheduler().runTask(
            Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"),
            () -> {
                try {
                    // TODO: 确认 AuthMe API 中修改密码的方法名
                    // 可能为 changePassword(String, String) 或 setPassword(String, String)
                    final java.lang.reflect.Method changeMethod = this.authMeApi.getClass()
                        .getMethod("changePassword", String.class, String.class);
                    changeMethod.invoke(this.authMeApi, playerName, newPassword);
                    result[0] = true;
                    this.logger.info("[BetterHTTPAPI] 玩家密码已修改: " + playerName);
                } catch (NoSuchMethodException e) {
                    // 尝试其他方法名
                    try {
                        final java.lang.reflect.Method changeMethod2 = this.authMeApi.getClass()
                            .getMethod("setPassword", String.class, String.class);
                        changeMethod2.invoke(this.authMeApi, playerName, newPassword);
                        result[0] = true;
                        this.logger.info("[BetterHTTPAPI] 玩家密码已修改: " + playerName);
                    } catch (Exception ex) {
                        this.logger.warning("[BetterHTTPAPI] AuthMe API 未提供 changePassword/setPassword 方法: " + e.getMessage());
                        result[0] = false;
                    }
                } catch (Exception e) {
                    this.logger.warning("[BetterHTTPAPI] 修改玩家密码时发生异常: " + e.getMessage());
                    result[0] = false;
                } finally {
                    latch.countDown();
                }
            }
        );

        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
        return result[0];
    }

    /**
     * 获取所有已注册玩家的列表。
     *
     * <p><b>必须在主线程执行</b>: 此方法已通过 Bukkit.getScheduler().runTask() 包装，
     * 确保在主线程执行查询操作。此方法会同步等待主线程任务完成（最多 5 秒）。</p>
     *
     * <p>使用反射调用 AuthMe API 的 getRegisteredNames 或类似方法获取已注册玩家列表。</p>
     *
     * <p><b>TODO</b>: 确认 AuthMe API 中获取已注册玩家列表的方法名</p>
     *
     * <p>使用示例:</p>
     * <pre>{@code
     * List<String> players = service.getRegisteredPlayers();
     * System.out.println("已注册玩家: " + players);
     * }</pre>
     *
     * @return 包含所有已注册玩家名称的 List。如果 AuthMe API 不可用或方法不存在, 返回空列表
     */
    public List<String> getRegisteredPlayers() {
        if (this.authMeApi == null) {
            return new ArrayList<>();
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final List<String> result = new ArrayList<>();

        Bukkit.getScheduler().runTask(
            Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"),
            () -> {
                try {
                    // TODO: 确认 AuthMe API 中获取已注册玩家列表的方法名
                    // 可能为 getRegisteredNames() 或 getRegisteredPlayers()
                    final java.lang.reflect.Method listMethod = this.authMeApi.getClass()
                        .getMethod("getRegisteredNames");
                    final Object returnValue = listMethod.invoke(this.authMeApi);

                    if (returnValue instanceof List) {
                        //noinspection unchecked
                        result.addAll((List<String>) returnValue);
                    }
                } catch (NoSuchMethodException e) {
                    this.logger.warning("[BetterHTTPAPI] AuthMe API 未提供 getRegisteredNames 方法: " + e.getMessage());
                } catch (Exception e) {
                    this.logger.warning("[BetterHTTPAPI] 获取已注册玩家列表时发生异常: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }
        );

        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return result; }
        return result;
    }

    /**
     * 强制登录玩家。
     *
     * <p><b>必须在主线程执行</b>: 此方法已通过 Bukkit.getScheduler().runTask() 包装，
     * 确保在主线程执行强制登录操作。此方法会同步等待主线程任务完成（最多 5 秒）。</p>
     *
     * <p>使用 AuthMe API 的 forceLogin 方法强制玩家登录。</p>
     *
     * <p>使用示例:</p>
     * <pre>{@code
     * boolean success = service.forceLogin(player);
     * if (success) {
     *     System.out.println("玩家已被强制登录");
     * }
     * }</pre>
     *
     * @param player 玩家对象, 不能为 null
     * @return 如果强制登录成功返回 true, 否则返回 false。
     *         如果 AuthMe API 不可用或强制登录失败, 返回 false
     */
    public boolean forceLogin(final Player player) {
        if (this.authMeApi == null) {
            return false;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        Bukkit.getScheduler().runTask(
            Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"),
            () -> {
                try {
                    this.authMeApi.forceLogin(player);
                    result[0] = true;
                } catch (Exception e) {
                    this.logger.warning("[BetterHTTPAPI] 强制玩家登录时发生异常: " + e.getMessage());
                    result[0] = false;
                } finally {
                    latch.countDown();
                }
            }
        );

        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
        return result[0];
    }

    /**
     * 设置玩家邮箱。
     *
     * <p><b>必须在主线程执行</b>: 此方法已通过 Bukkit.getScheduler().runTask() 包装，
     * 确保在主线程执行设置邮箱操作。此方法会同步等待主线程任务完成（最多 5 秒）。</p>
     *
     * <p>使用反射调用 AuthMe API 的 setEmail 或类似方法。</p>
     *
     * <p><b>TODO</b>: 确认 AuthMe API 中设置玩家邮箱的方法名</p>
     *
     * <p>使用示例:</p>
     * <pre>{@code
     * boolean success = service.setPlayerEmail("Steve", "steve@example.com");
     * if (success) {
     *     System.out.println("邮箱已设置");
     * }
     * }</pre>
     *
     * @param playerName 玩家名称, 不能为 null
     * @param email 邮箱地址, 不能为 null
     * @return 如果设置成功返回 true, 否则返回 false。
     *         如果 AuthMe API 不可用或设置失败, 返回 false
     */
    public boolean setPlayerEmail(final String playerName, final String email) {
        if (this.authMeApi == null) {
            return false;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        Bukkit.getScheduler().runTask(
            Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"),
            () -> {
                try {
                    // TODO: 确认 AuthMe API 中设置玩家邮箱的方法名
                    // 可能为 setEmail(String, String) 或 setPlayerEmail(String, String)
                    final java.lang.reflect.Method emailMethod = this.authMeApi.getClass()
                        .getMethod("setEmail", String.class, String.class);
                    emailMethod.invoke(this.authMeApi, playerName, email);
                    result[0] = true;
                    this.logger.info("[BetterHTTPAPI] 玩家邮箱已设置: " + playerName);
                } catch (NoSuchMethodException e) {
                    // 尝试其他方法名
                    try {
                        final java.lang.reflect.Method emailMethod2 = this.authMeApi.getClass()
                            .getMethod("setPlayerEmail", String.class, String.class);
                        emailMethod2.invoke(this.authMeApi, playerName, email);
                        result[0] = true;
                        this.logger.info("[BetterHTTPAPI] 玩家邮箱已设置: " + playerName);
                    } catch (Exception ex) {
                        this.logger.warning("[BetterHTTPAPI] AuthMe API 未提供 setEmail/setPlayerEmail 方法: " + e.getMessage());
                        result[0] = false;
                    }
                } catch (Exception e) {
                    this.logger.warning("[BetterHTTPAPI] 设置玩家邮箱时发生异常: " + e.getMessage());
                    result[0] = false;
                } finally {
                    latch.countDown();
                }
            }
        );

        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
        return result[0];
    }
}