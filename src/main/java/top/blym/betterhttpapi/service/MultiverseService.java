package top.blym.betterhttpapi.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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

import top.blym.betterhttpapi.PluginAPIManager;

/**
 * Multiverse-Core 世界管理服务类。
 *
 * <p>提供与 Multiverse-Core 插件集成的世界管理功能，包括世界列表获取、世界信息查询、
 * 世界创建、删除、加载、卸载以及玩家传送等功能。所有方法均使用反射调用 Multiverse-Core API，
 * 确保在插件未安装时安全降级。</p>
 *
 * <p><b>重要说明</b>：所有涉及世界操作的 Bukkit API 调用（创建、删除、加载、卸载、传送等）
 * 均使用 {@link Bukkit#getScheduler()#runTask()} 在主线程执行，确保线程安全。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * MultiverseService service = MultiverseService.getInstance();
 *
 * // 获取所有世界名称
 * List<String> worlds = service.getWorldNames();
 *
 * // 获取世界详细信息
 * Map<String, Object> info = service.getWorldInfo("world_nether");
 *
 * // 异步创建世界
 * service.createWorld("myWorld", "NORMAL", 12345L, "SURVIVAL")
 *     .thenAccept(success -> {
 *         if (success) {
 *             System.out.println("世界创建成功");
 *         }
 *     });
 *
 * // 传送玩家到世界
 * service.teleportToWorld(player, "world_nether");
 *
 * // 克隆世界
 * boolean cloned = service.cloneWorld("world", "world_clone");
 *
 * // 设置世界游戏模式
 * boolean gmSet = service.setWorldGameMode("world", "CREATIVE");
 *
 * // 设置世界难度
 * boolean diffSet = service.setWorldDifficulty("world", "HARD");
 *
 * // 设置世界边界
 * boolean borderSet = service.setWorldBorder("world", 5000.0, 0.0, 0.0);
 *
 * // 设置世界重生点
 * Location spawnLoc = new Location(Bukkit.getWorld("world"), 0, 64, 0);
 * boolean spawnSet = service.setWorldSpawn("world", spawnLoc);
 *
 * // 获取世界高级信息
 * Map&lt;String, Object&gt; advInfo = service.getWorldAdvancedInfo("world");
 *
 * // 生成生物
 * Location mobLoc = new Location(Bukkit.getWorld("world"), 100, 64, 100);
 * boolean mobSpawned = service.spawnMob("world", "ZOMBIE", 10, mobLoc);
 * }</pre>
 *
 * @author 白鹿原嚒
 * @version 1.0.0
 */
public class MultiverseService {

    /** 单例实例 */
    private static volatile MultiverseService instance;

    /** 日志记录器 */
    private final Logger logger;

    /** 缓存的 Multiverse-Core API 类 */
    private Class<?> mvWorldClass;

    /** 缓存的世界管理器接口类 */
    private Class<?> worldManagerClass;

    /** 缓存的环境类型枚举类 */
    private Class<?> environmentClass;

    /** 缓存的游戏模式枚举类 */
    private Class<?> gameModeClass;

    /** 缓存的创建世界选项类 */
    private Class<?> createWorldOptionsClass;

    /** 缓存的删除世界选项类 */
    private Class<?> deleteWorldOptionsClass;

    /**
     * 私有构造器（单例模式）。
     *
     * <p>初始化 Multiverse-Core API 的反射类缓存，如果插件未安装则记录警告日志。</p>
     */
    private MultiverseService() {
        this.logger = Bukkit.getLogger();
        initializeClasses();
    }

    /**
     * 初始化 Multiverse-Core 相关类的反射缓存。
     *
     * <p>缓存常用类以避免重复的类查找开销。</p>
     */
    private void initializeClasses() {
        try {
            // 缓存 MVWorld 类
            this.mvWorldClass = Class.forName("com.onarandombox.MultiverseCore.api.MVWorld");
            // 缓存 WorldManager 接口
            this.worldManagerClass = Class.forName("com.onarandombox.MultiverseCore.api.WorldManager");
            // 缓存世界环境枚举
            this.environmentClass = Class.forName("org.bukkit.World$Environment");
            // 缓存游戏模式枚举
            this.gameModeClass = Class.forName("org.bukkit.GameMode");
            // 缓存创建世界选项类
            this.createWorldOptionsClass = Class.forName("com.onarandombox.MultiverseCore.api.CreateWorldOptions");
            // 缓存删除世界选项类
            this.deleteWorldOptionsClass = Class.forName("com.onarandombox.MultiverseCore.api.DeleteWorldOptions");
        } catch (ClassNotFoundException e) {
            this.logger.warning("[BetterHTTPAPI] Multiverse-Core 类未找到，世界管理功能将不可用: " + e.getMessage());
        }
    }

    /**
     * 获取单例实例。
     *
     * <p>使用双重检查锁定确保线程安全的单例初始化。</p>
     *
     * @return MultiverseService 实例
     */
    public static MultiverseService getInstance() {
        if (instance == null) {
            synchronized (MultiverseService.class) {
                if (instance == null) {
                    instance = new MultiverseService();
                }
            }
        }
        return instance;
    }

    /**
     * 获取所有 Multiverse 世界的名称列表。
     *
     * <p>从 Multiverse-Core 的 WorldManager 获取所有已加载的世界，
     * 提取世界名称并返回。如果 API 不可用或发生异常，返回空列表。</p>
     *
     * <p><b>线程要求</b>：此方法包含 Bukkit API 调用，应在主线程执行或结果会包含主线程时的世界状态。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * List<String> worlds = service.getWorldNames();
     * worlds.forEach(name -> System.out.println("世界: " + name));
     * }</pre>
     *
     * @return 世界名称列表，如果 API 不可用或发生异常，返回空列表
     */
    public List<String> getWorldNames() {
        final Optional<Object> mvApi = PluginAPIManager.getInstance().getMultiverseCore();
        if (mvApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] Multiverse-Core API 不可用，无法获取世界列表");
            return Collections.emptyList();
        }

        try {
            final Object api = mvApi.get();
            final Method getWorldManagerMethod = api.getClass().getMethod("getWorldManager");
            final Object worldManager = getWorldManagerMethod.invoke(api);

            // 获取世界列表
            final Method getWorldsMethod = this.worldManagerClass.getMethod("getWorlds");
            final Object worlds = getWorldsMethod.invoke(worldManager);

            if (worlds instanceof List) {
                @SuppressWarnings("unchecked")
                final List<?> worldList = (List<?>) worlds;
                return worldList.stream()
                    .map(world -> {
                        try {
                            final Method getNameMethod = this.mvWorldClass.getMethod("getName");
                            return (String) getNameMethod.invoke(world);
                        } catch (Exception e) {
                            this.logger.warning("[BetterHTTPAPI] 获取世界名称失败: " + e.getMessage());
                            return null;
                        }
                    })
                    .filter(name -> name != null)
                    .collect(java.util.stream.Collectors.toList());
            }

            return Collections.emptyList();
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 获取世界列表时发生异常: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 获取指定世界的详细信息。
     *
     * <p>返回的世界信息包括：</p>
     * <ul>
     *   <li>name - 世界名称</li>
     *   <li>alias - 世界别名</li>
     *   <li>environment - 世界环境（NORMAL, NETHER, THE_END）</li>
     *   <li>loaded - 是否已加载</li>
     *   <li>playerCount - 在线玩家数</li>
     *   <li>seed - 世界种子</li>
     *   <li>gameMode - 游戏模式</li>
     *   <li>difficulty - 难度</li>
     *   <li>pvp - 是否允许 PVP</li>
     *   <li>spawning - 是否允许生物生成</li>
     * </ul>
     *
     * <p><b>线程要求</b>：此方法包含 Bukkit API 调用，应在主线程执行。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * Map<String, Object> info = service.getWorldInfo("world");
     * System.out.println("世界名称: " + info.get("name"));
     * System.out.println("玩家数: " + info.get("playerCount"));
     * }</pre>
     *
     * @param worldName 世界名称，不能为 null
     * @return 包含世界信息的 Map，如果 API 不可用或世界不存在，返回空 Map
     */
    public Map<String, Object> getWorldInfo(final String worldName) {
        final Optional<Object> mvApi = PluginAPIManager.getInstance().getMultiverseCore();
        if (mvApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] Multiverse-Core API 不可用，无法获取世界信息");
            return Collections.emptyMap();
        }

        try {
            final Object api = mvApi.get();
            final Method getWorldManagerMethod = api.getClass().getMethod("getWorldManager");
            final Object worldManager = getWorldManagerMethod.invoke(api);

            // 使用反射获取世界（返回 vavr Option）
            final Method getWorldMethod = this.worldManagerClass.getMethod("getWorld", String.class);
            final Object worldOption = getWorldMethod.invoke(worldManager, worldName);

            // 检查 Option 是否有值
            final Class<?> optionClass = Class.forName("io.vavr.control.Option");
            final Method isDefinedMethod = optionClass.getMethod("isDefined");
            final boolean isDefined = (boolean) isDefinedMethod.invoke(worldOption);

            if (!isDefined) {
                this.logger.warning("[BetterHTTPAPI] 世界不存在: " + worldName);
                return Collections.emptyMap();
            }

            // 获取世界对象
            final Method getMethod = optionClass.getMethod("get");
            final Object world = getMethod.invoke(worldOption);

            // 构建世界信息 Map
            final Map<String, Object> info = new HashMap<>();
            info.put("name", invokeMethod(world, "getName"));
            info.put("alias", invokeMethod(world, "getAlias"));
            info.put("environment", invokeMethod(world, "getEnvironment"));

            final Object loadedObj = invokeMethod(world, "isLoaded");
            info.put("loaded", loadedObj != null ? loadedObj : false);

            final Object playerCountObj = invokeMethod(world, "getPlayerCount");
            info.put("playerCount", playerCountObj != null ? playerCountObj : 0);

            info.put("seed", invokeMethod(world, "getSeed"));
            info.put("gameMode", invokeMethod(world, "getGameMode"));
            info.put("difficulty", invokeMethod(world, "getDifficulty"));

            final Object pvpObj = invokeMethod(world, "isPvpEnabled");
            info.put("pvp", pvpObj != null ? pvpObj : false);

            final Object spawningObj = invokeMethod(world, "isAllowSpawn");
            info.put("spawning", spawningObj != null ? spawningObj : true);

            return info;
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 获取世界信息时发生异常: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 创建新世界。
     *
     * <p>使用 Multiverse-Core 的 CreateWorldOptions 配置世界创建参数，包括世界名称、环境类型、
     * 种子和游戏模式。创建操作异步执行，结果通过 CompletableFuture 返回。</p>
     *
     * <p><b>重要</b>：此方法会将实际的创建操作提交到主线程执行，确保线程安全。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * service.createWorld("testWorld", "NORMAL", 12345L, "SURVIVAL")
     *     .thenAccept(success -> {
     *         if (success) {
     *             System.out.println("世界创建成功");
     *         } else {
     *             System.out.println("世界创建失败");
     *         }
     *     });
     * }</pre>
     *
     * @param name 世界名称，不能为 null 或空
     * @param environment 环境类型（NORMAL, NETHER, THE_END），不区分大小写
     * @param seed 世界种子，用于生成地形
     * @param gamemode 游戏模式（SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR），不区分大小写
     * @return CompletableFuture，完成时返回 true 表示创建成功，false 表示失败
     */
    public CompletableFuture<Boolean> createWorld(final String name, final String environment, final long seed, final String gamemode) {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();

        // 检查 API 可用性
        final Optional<Object> mvApi = PluginAPIManager.getInstance().getMultiverseCore();
        if (mvApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] Multiverse-Core API 不可用，无法创建世界");
            future.complete(false);
            return future;
        }

        // 在主线程执行世界创建
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"), () -> {
            try {
                final Object api = mvApi.get();
                final Method getWorldManagerMethod = api.getClass().getMethod("getWorldManager");
                final Object worldManager = getWorldManagerMethod.invoke(api);

                // 解析环境类型
                final Object envValue = parseEnvironment(environment);

                // 解析游戏模式
                final Object gameModeValue = parseGameMode(gamemode);

                // 创建 CreateWorldOptions
                final Object options = createWorldOptions(name, envValue, seed, gameModeValue);

                // 调用 createWorld 方法
                final Method createWorldMethod = this.worldManagerClass.getMethod("createWorld", this.createWorldOptionsClass);
                final Object result = createWorldMethod.invoke(worldManager, options);

                // 处理返回的 Try 对象（vavr）
                final Class<?> tryClass = Class.forName("io.vavr.control.Try");
                final Method isSuccessMethod = tryClass.getMethod("isSuccess");
                final boolean isSuccess = (boolean) isSuccessMethod.invoke(result);

                if (isSuccess) {
                    this.logger.info("[BetterHTTPAPI] 世界创建成功: " + name);
                    future.complete(true);
                } else {
                    final Method getCauseMethod = tryClass.getMethod("getCause");
                    final Throwable cause = (Throwable) getCauseMethod.invoke(result);
                    this.logger.warning("[BetterHTTPAPI] 世界创建失败: " + (cause != null ? cause.getMessage() : "未知错误"));
                    future.complete(false);
                }
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 创建世界时发生异常: " + e.getMessage());
                future.complete(false);
            }
        });

        return future;
    }

    /**
     * 删除指定世界。
     *
     * <p>使用 Multiverse-Core 的 DeleteWorldOptions 配置删除参数，然后删除世界。
     * 删除操作异步执行，结果通过 CompletableFuture 返回。</p>
     *
     * <p><b>警告</b>：此操作将永久删除世界数据，请谨慎使用。</p>
     *
     * <p><b>重要</b>：此方法会将实际的删除操作提交到主线程执行，确保线程安全。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * service.deleteWorld("oldWorld")
     *     .thenAccept(success -> {
     *         if (success) {
     *             System.out.println("世界删除成功");
     *         }
     *     });
     * }</pre>
     *
     * @param name 世界名称，不能为 null 或空
     * @return CompletableFuture，完成时返回 true 表示删除成功，false 表示失败
     */
    public CompletableFuture<Boolean> deleteWorld(final String name) {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();

        // 检查 API 可用性
        final Optional<Object> mvApi = PluginAPIManager.getInstance().getMultiverseCore();
        if (mvApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] Multiverse-Core API 不可用，无法删除世界");
            future.complete(false);
            return future;
        }

        // 在主线程执行世界删除
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"), () -> {
            try {
                final Object api = mvApi.get();
                final Method getWorldManagerMethod = api.getClass().getMethod("getWorldManager");
                final Object worldManager = getWorldManagerMethod.invoke(api);

                // 创建 DeleteWorldOptions
                final Method worldMethod = this.deleteWorldOptionsClass.getMethod("world", String.class);
                final Object options = worldMethod.invoke(null, name);

                // 调用 deleteWorld 方法
                final Method deleteWorldMethod = this.worldManagerClass.getMethod("deleteWorld", this.deleteWorldOptionsClass);
                final Object result = deleteWorldMethod.invoke(worldManager, options);

                // 处理返回的 Try 对象（vavr）
                final Class<?> tryClass = Class.forName("io.vavr.control.Try");
                final Method isSuccessMethod = tryClass.getMethod("isSuccess");
                final boolean isSuccess = (boolean) isSuccessMethod.invoke(result);

                if (isSuccess) {
                    this.logger.info("[BetterHTTPAPI] 世界删除成功: " + name);
                    future.complete(true);
                } else {
                    final Method getCauseMethod = tryClass.getMethod("getCause");
                    final Throwable cause = (Throwable) getCauseMethod.invoke(result);
                    this.logger.warning("[BetterHTTPAPI] 世界删除失败: " + (cause != null ? cause.getMessage() : "未知错误"));
                    future.complete(false);
                }
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 删除世界时发生异常: " + e.getMessage());
                future.complete(false);
            }
        });

        return future;
    }

    /**
     * 卸载指定世界。
     *
     * <p>从 Multiverse-Core 中卸载指定世界，但不会删除世界数据。
     * 卸载操作异步执行，结果通过 CompletableFuture 返回。</p>
     *
     * <p><b>重要</b>：此方法会将实际的卸载操作提交到主线程执行，确保线程安全。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * service.unloadWorld("world_nether")
     *     .thenAccept(success -> {
     *         if (success) {
     *             System.out.println("世界卸载成功");
     *         }
     *     });
     * }</pre>
     *
     * @param name 世界名称，不能为 null 或空
     * @return CompletableFuture，完成时返回 true 表示卸载成功，false 表示失败
     */
    public CompletableFuture<Boolean> unloadWorld(final String name) {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();

        // 检查 API 可用性
        final Optional<Object> mvApi = PluginAPIManager.getInstance().getMultiverseCore();
        if (mvApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] Multiverse-Core API 不可用，无法卸载世界");
            future.complete(false);
            return future;
        }

        // 在主线程执行世界卸载
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"), () -> {
            try {
                final Object api = mvApi.get();
                final Method getWorldManagerMethod = api.getClass().getMethod("getWorldManager");
                final Object worldManager = getWorldManagerMethod.invoke(api);

                // 首先获取 MVWorld 对象
                final Method getWorldMethod = this.worldManagerClass.getMethod("getWorld", String.class);
                final Object worldOption = getWorldMethod.invoke(worldManager, name);

                // 检查 Option 是否有值
                final Class<?> optionClass = Class.forName("io.vavr.control.Option");
                final Method isDefinedMethod = optionClass.getMethod("isDefined");
                final boolean isDefined = (boolean) isDefinedMethod.invoke(worldOption);

                if (!isDefined) {
                    this.logger.warning("[BetterHTTPAPI] 世界不存在: " + name);
                    future.complete(false);
                    return;
                }

                // 获取世界对象
                final Method getMethod = optionClass.getMethod("get");
                final Object mvWorld = getMethod.invoke(worldOption);

                // 调用 unloadWorld 方法
                final Method unloadWorldMethod = this.worldManagerClass.getMethod("unloadWorld", this.mvWorldClass);
                final Object result = unloadWorldMethod.invoke(worldManager, mvWorld);

                // 处理返回的 Try 对象（vavr）
                final Class<?> tryClass = Class.forName("io.vavr.control.Try");
                final Method isSuccessMethod = tryClass.getMethod("isSuccess");
                final boolean isSuccess = (boolean) isSuccessMethod.invoke(result);

                if (isSuccess) {
                    this.logger.info("[BetterHTTPAPI] 世界卸载成功: " + name);
                    future.complete(true);
                } else {
                    final Method getCauseMethod = tryClass.getMethod("getCause");
                    final Throwable cause = (Throwable) getCauseMethod.invoke(result);
                    this.logger.warning("[BetterHTTPAPI] 世界卸载失败: " + (cause != null ? cause.getMessage() : "未知错误"));
                    future.complete(false);
                }
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 卸载世界时发生异常: " + e.getMessage());
                future.complete(false);
            }
        });

        return future;
    }

    /**
     * 加载指定世界。
     *
     * <p>使用 Multiverse-Core 加载已存在但未加载的世界。
     * 加载操作异步执行，结果通过 CompletableFuture 返回。</p>
     *
     * <p><b>重要</b>：此方法会将实际的加载操作提交到主线程执行，确保线程安全。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * service.loadWorld("world_nether")
     *     .thenAccept(success -> {
     *         if (success) {
     *             System.out.println("世界加载成功");
     *         }
     *     });
     * }</pre>
     *
     * @param name 世界名称，不能为 null 或空
     * @return CompletableFuture，完成时返回 true 表示加载成功，false 表示失败
     */
    public CompletableFuture<Boolean> loadWorld(final String name) {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();

        // 检查 API 可用性
        final Optional<Object> mvApi = PluginAPIManager.getInstance().getMultiverseCore();
        if (mvApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] Multiverse-Core API 不可用，无法加载世界");
            future.complete(false);
            return future;
        }

        // 在主线程执行世界加载
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"), () -> {
            try {
                final Object api = mvApi.get();
                final Method getWorldManagerMethod = api.getClass().getMethod("getWorldManager");
                final Object worldManager = getWorldManagerMethod.invoke(api);

                // 首先获取 MVWorld 对象
                final Method getWorldMethod = this.worldManagerClass.getMethod("getWorld", String.class);
                final Object worldOption = getWorldMethod.invoke(worldManager, name);

                // 检查 Option 是否有值
                final Class<?> optionClass = Class.forName("io.vavr.control.Option");
                final Method isDefinedMethod = optionClass.getMethod("isDefined");
                final boolean isDefined = (boolean) isDefinedMethod.invoke(worldOption);

                if (!isDefined) {
                    this.logger.warning("[BetterHTTPAPI] 世界不存在: " + name);
                    future.complete(false);
                    return;
                }

                // 获取世界对象
                final Method getMethod = optionClass.getMethod("get");
                final Object mvWorld = getMethod.invoke(worldOption);

                // 调用 loadWorld 方法
                final Method loadWorldMethod = this.worldManagerClass.getMethod("loadWorld", this.mvWorldClass);
                final Object result = loadWorldMethod.invoke(worldManager, mvWorld);

                // 处理返回的 Try 对象（vavr）
                final Class<?> tryClass = Class.forName("io.vavr.control.Try");
                final Method isSuccessMethod = tryClass.getMethod("isSuccess");
                final boolean isSuccess = (boolean) isSuccessMethod.invoke(result);

                if (isSuccess) {
                    this.logger.info("[BetterHTTPAPI] 世界加载成功: " + name);
                    future.complete(true);
                } else {
                    final Method getCauseMethod = tryClass.getMethod("getCause");
                    final Throwable cause = (Throwable) getCauseMethod.invoke(result);
                    this.logger.warning("[BetterHTTPAPI] 世界加载失败: " + (cause != null ? cause.getMessage() : "未知错误"));
                    future.complete(false);
                }
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 加载世界时发生异常: " + e.getMessage());
                future.complete(false);
            }
        });

        return future;
    }

    /**
     * 传送玩家到指定世界的重生点。
     *
     * <p>获取目标世界的重生点位置，并将玩家传送到该位置。
     * 传送操作在主线程执行，确保线程安全。</p>
     *
     * <p><b>重要</b>：此方法会将传送操作提交到主线程执行，确保线程安全。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * Player player = Bukkit.getPlayer("Steve");
     * if (player != null) {
     *     service.teleportToWorld(player, "world_nether");
     * }
     * }</pre>
     *
     * @param player 玩家对象，不能为 null
     * @param worldName 目标世界名称，不能为 null 或空
     */
    public void teleportToWorld(final Player player, final String worldName) {
        // 检查 API 可用性
        final Optional<Object> mvApi = PluginAPIManager.getInstance().getMultiverseCore();
        if (mvApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] Multiverse-Core API 不可用，无法传送玩家");
            return;
        }

        // 在主线程执行传送
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"), () -> {
            try {
                final Object api = mvApi.get();
                final Method getWorldManagerMethod = api.getClass().getMethod("getWorldManager");
                final Object worldManager = getWorldManagerMethod.invoke(api);

                // 获取目标世界
                final Method getWorldMethod = this.worldManagerClass.getMethod("getWorld", String.class);
                final Object worldOption = getWorldMethod.invoke(worldManager, worldName);

                // 检查 Option 是否有值
                final Class<?> optionClass = Class.forName("io.vavr.control.Option");
                final Method isDefinedMethod = optionClass.getMethod("isDefined");
                final boolean isDefined = (boolean) isDefinedMethod.invoke(worldOption);

                if (!isDefined) {
                    this.logger.warning("[BetterHTTPAPI] 目标世界不存在: " + worldName);
                    return;
                }

                // 获取世界对象
                final Method getMethod = optionClass.getMethod("get");
                final Object mvWorld = getMethod.invoke(worldOption);

                // 获取重生点
                final Method getSpawnLocationMethod = this.mvWorldClass.getMethod("getSpawnLocation");
                final Location spawnLocation = (Location) getSpawnLocationMethod.invoke(mvWorld);

                if (spawnLocation != null) {
                    player.teleport(spawnLocation);
                    this.logger.info("[BetterHTTPAPI] 玩家 " + player.getName() + " 已传送到世界 " + worldName);
                } else {
                    this.logger.warning("[BetterHTTPAPI] 世界 " + worldName + " 的重生点无效");
                }
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 传送玩家时发生异常: " + e.getMessage());
            }
        });
    }

    /**
     * 检查 Multiverse-Core API 是否可用。
     *
     * <p>用于在调用其他方法前检查 API 状态。</p>
     *
     * @return 如果 Multiverse-Core API 已加载且可用返回 true，否则返回 false
     */
    public boolean isAvailable() {
        return PluginAPIManager.getInstance().getMultiverseCore().isPresent();
    }

    /**
     * 克隆世界。
     *
     * <p>使用 Multiverse-Core API 克隆指定世界。此操作在主线程执行，使用 CountDownLatch 等待最多 10 秒。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * boolean cloned = service.cloneWorld("world", "world_clone");
     * }</pre>
     *
     * @param sourceWorld 源世界名称
     * @param targetWorld 目标世界名称
     * @return 克隆成功返回 true，否则返回 false
     */
    public boolean cloneWorld(final String sourceWorld, final String targetWorld) {
        final Optional<Object> mvApi = PluginAPIManager.getInstance().getMultiverseCore();
        if (mvApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] Multiverse-Core API 不可用，无法克隆世界");
            return false;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"), () -> {
            try {
                final Object api = mvApi.get();
                final Method getWorldManagerMethod = api.getClass().getMethod("getWorldManager");
                final Object worldManager = getWorldManagerMethod.invoke(api);

                // 使用反射调用 cloneWorld 方法
                final Method cloneWorldMethod = this.worldManagerClass.getMethod("cloneWorld", String.class, String.class);
                final Object cloneResult = cloneWorldMethod.invoke(worldManager, sourceWorld, targetWorld);

                // 处理返回的 Try 对象
                final Class<?> tryClass = Class.forName("io.vavr.control.Try");
                final Method isSuccessMethod = tryClass.getMethod("isSuccess");
                final boolean isSuccess = (boolean) isSuccessMethod.invoke(cloneResult);

                if (isSuccess) {
                    this.logger.info("[BetterHTTPAPI] 世界克隆成功: " + sourceWorld + " -> " + targetWorld);
                    result[0] = true;
                } else {
                    this.logger.warning("[BetterHTTPAPI] 世界克隆失败");
                    result[0] = false;
                }
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 克隆世界时发生异常: " + e.getMessage());
                result[0] = false;
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            this.logger.warning("[BetterHTTPAPI] 等待克隆世界任务完成时被中断");
            Thread.currentThread().interrupt();
            return false;
        }

        return result[0];
    }

    /**
     * 设置世界游戏模式。
     *
     * <p>使用 Multiverse-Core API 设置指定世界的游戏模式。此操作在主线程执行，使用 CountDownLatch 等待最多 10 秒。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * boolean success = service.setWorldGameMode("world", "CREATIVE");
     * }</pre>
     *
     * @param worldName 世界名称
     * @param gamemode 游戏模式（SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR）
     * @return 设置成功返回 true，否则返回 false
     */
    public boolean setWorldGameMode(final String worldName, final String gamemode) {
        final Optional<Object> mvApi = PluginAPIManager.getInstance().getMultiverseCore();
        if (mvApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] Multiverse-Core API 不可用，无法设置世界游戏模式");
            return false;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"), () -> {
            try {
                final Object api = mvApi.get();
                final Method getWorldManagerMethod = api.getClass().getMethod("getWorldManager");
                final Object worldManager = getWorldManagerMethod.invoke(api);

                final Method getWorldMethod = this.worldManagerClass.getMethod("getWorld", String.class);
                final Object worldOption = getWorldMethod.invoke(worldManager, worldName);

                final Class<?> optionClass = Class.forName("io.vavr.control.Option");
                final Method isDefinedMethod = optionClass.getMethod("isDefined");
                if (!(boolean) isDefinedMethod.invoke(worldOption)) {
                    this.logger.warning("[BetterHTTPAPI] 世界不存在: " + worldName);
                    result[0] = false;
                    return;
                }

                final Method getMethod = optionClass.getMethod("get");
                final Object mvWorld = getMethod.invoke(worldOption);

                final Object gameModeValue = parseGameMode(gamemode);
                final Method setGameModeMethod = this.mvWorldClass.getMethod("setGameMode", this.gameModeClass);
                setGameModeMethod.invoke(mvWorld, gameModeValue);

                this.logger.info("[BetterHTTPAPI] 世界游戏模式设置成功: " + worldName + " -> " + gamemode);
                result[0] = true;
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 设置世界游戏模式时发生异常: " + e.getMessage());
                result[0] = false;
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            this.logger.warning("[BetterHTTPAPI] 等待设置世界游戏模式任务完成时被中断");
            Thread.currentThread().interrupt();
            return false;
        }

        return result[0];
    }

    /**
     * 设置世界难度。
     *
     * <p>使用 Multiverse-Core API 设置指定世界的难度。此操作在主线程执行，使用 CountDownLatch 等待最多 10 秒。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * boolean success = service.setWorldDifficulty("world", "HARD");
     * }</pre>
     *
     * @param worldName 世界名称
     * @param difficulty 难度（PEACEFUL, EASY, NORMAL, HARD）
     * @return 设置成功返回 true，否则返回 false
     */
    public boolean setWorldDifficulty(final String worldName, final String difficulty) {
        final Optional<Object> mvApi = PluginAPIManager.getInstance().getMultiverseCore();
        if (mvApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] Multiverse-Core API 不可用，无法设置世界难度");
            return false;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"), () -> {
            try {
                final Object api = mvApi.get();
                final Method getWorldManagerMethod = api.getClass().getMethod("getWorldManager");
                final Object worldManager = getWorldManagerMethod.invoke(api);

                final Method getWorldMethod = this.worldManagerClass.getMethod("getWorld", String.class);
                final Object worldOption = getWorldMethod.invoke(worldManager, worldName);

                final Class<?> optionClass = Class.forName("io.vavr.control.Option");
                final Method isDefinedMethod = optionClass.getMethod("isDefined");
                if (!(boolean) isDefinedMethod.invoke(worldOption)) {
                    this.logger.warning("[BetterHTTPAPI] 世界不存在: " + worldName);
                    result[0] = false;
                    return;
                }

                final Method getMethod = optionClass.getMethod("get");
                final Object mvWorld = getMethod.invoke(worldOption);

                // 解析难度枚举值
                final Class<?> difficultyClass = Class.forName("org.bukkit.Difficulty");
                final Object difficultyValue = Enum.valueOf((Class<? extends Enum>) difficultyClass, difficulty.toUpperCase());
                final Method setDifficultyMethod = this.mvWorldClass.getMethod("setDifficulty", difficultyClass);
                setDifficultyMethod.invoke(mvWorld, difficultyValue);

                this.logger.info("[BetterHTTPAPI] 世界难度设置成功: " + worldName + " -> " + difficulty);
                result[0] = true;
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 设置世界难度时发生异常: " + e.getMessage());
                result[0] = false;
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            this.logger.warning("[BetterHTTPAPI] 等待设置世界难度任务完成时被中断");
            Thread.currentThread().interrupt();
            return false;
        }

        return result[0];
    }

    /**
     * 设置世界边界。
     *
     * <p>使用 Bukkit World API 设置指定世界的边界大小和中心位置。此操作在主线程执行，使用 CountDownLatch 等待最多 10 秒。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * boolean success = service.setWorldBorder("world", 5000.0, 0.0, 0.0);
     * }</pre>
     *
     * @param worldName 世界名称
     * @param size 边界大小（半径）
     * @param centerX 中心点 X 坐标
     * @param centerZ 中心点 Z 坐标
     * @return 设置成功返回 true，否则返回 false
     */
    public boolean setWorldBorder(final String worldName, final double size, final double centerX, final double centerZ) {
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"), () -> {
            try {
                final World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    this.logger.warning("[BetterHTTPAPI] 世界不存在: " + worldName);
                    result[0] = false;
                    return;
                }

                final org.bukkit.WorldBorder border = world.getWorldBorder();
                border.setSize(size);
                border.setCenter(centerX, centerZ);

                this.logger.info("[BetterHTTPAPI] 世界边界设置成功: " + worldName);
                result[0] = true;
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 设置世界边界时发生异常: " + e.getMessage());
                result[0] = false;
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            this.logger.warning("[BetterHTTPAPI] 等待设置世界边界任务完成时被中断");
            Thread.currentThread().interrupt();
            return false;
        }

        return result[0];
    }

    /**
     * 设置世界重生点。
     *
     * <p>使用 Bukkit World API 设置指定世界的重生点位置。此操作在主线程执行，使用 CountDownLatch 等待最多 10 秒。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * Location spawnLoc = new Location(Bukkit.getWorld("world"), 0, 64, 0);
     * boolean success = service.setWorldSpawn("world", spawnLoc);
     * }</pre>
     *
     * @param worldName 世界名称
     * @param location 重生点位置
     * @return 设置成功返回 true，否则返回 false
     */
    public boolean setWorldSpawn(final String worldName, final Location location) {
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"), () -> {
            try {
                final World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    this.logger.warning("[BetterHTTPAPI] 世界不存在: " + worldName);
                    result[0] = false;
                    return;
                }

                world.setSpawnLocation(location);
                this.logger.info("[BetterHTTPAPI] 世界重生点设置成功: " + worldName);
                result[0] = true;
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 设置世界重生点时发生异常: " + e.getMessage());
                result[0] = false;
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            this.logger.warning("[BetterHTTPAPI] 等待设置世界重生点任务完成时被中断");
            Thread.currentThread().interrupt();
            return false;
        }

        return result[0];
    }

    /**
     * 获取世界高级信息。
     *
     * <p>使用 Bukkit World API 获取指定世界的详细信息，包括种子、生成器、动物生成、难度、时间等高级信息。
     * 此操作在主线程执行，使用 CountDownLatch 等待最多 10 秒。</p>
     *
     * <p>返回的信息包括：</p>
     * <ul>
     *   <li>seed - 世界种子</li>
     *   <li>environment - 世界环境类型</li>
     *   <li>generator - 世界生成器</li>
     *   <li>allowAnimals - 是否允许动物生成</li>
     *   <li>allowMonsters - 是否允许怪物生成</li>
     *   <li>keepSpawnInMemory - 是否保持重生点区块加载</li>
     *   <li>autoSave - 是否自动保存</li>
     *   <li>difficulty - 难度</li>
     *   <li>fullTime - 总时间</li>
     *   <li>time - 当前时间</li>
     *   <li>weatherDuration - 天气持续时间</li>
     *   <li>thunderDuration - 雷暴持续时间</li>
     *   <li>thundering - 是否雷暴</li>
     *   <li>storm - 是否有暴风雨</li>
     *   <li>maxHeight - 最大高度</li>
     *   <li>minHeight - 最小高度</li>
     *   <li>seaLevel - 海平面</li>
     *   <li>loadedChunks - 已加载区块数</li>
     *   <li>entityCount - 实体数量</li>
     *   <li>tileEntityCount - 方块实体数量</li>
     * </ul>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * Map<String, Object> info = service.getWorldAdvancedInfo("world");
     * System.out.println("世界种子: " + info.get("seed"));
     * System.out.println("允许动物生成: " + info.get("allowAnimals"));
     * }</pre>
     *
     * @param worldName 世界名称
     * @return 包含世界高级信息的 Map，如果世界不存在或发生异常返回空 Map
     */
    public Map<String, Object> getWorldAdvancedInfo(final String worldName) {
        final CountDownLatch latch = new CountDownLatch(1);
        @SuppressWarnings("unchecked")
        final Map<String, Object>[] result = new Map[1];

        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"), () -> {
            try {
                final World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    this.logger.warning("[BetterHTTPAPI] 世界不存在: " + worldName);
                    result[0] = Collections.emptyMap();
                    return;
                }

                final Map<String, Object> info = new HashMap<>();
                info.put("seed", world.getSeed());
                info.put("environment", world.getEnvironment().name());
                info.put("generator", world.getGenerator() != null ? world.getGenerator().getClass().getName() : "default");
                info.put("allowAnimals", world.getAllowAnimals());
                info.put("allowMonsters", world.getAllowMonsters());
                info.put("keepSpawnInMemory", world.getKeepSpawnInMemory());
                info.put("autoSave", world.isAutoSave());
                info.put("difficulty", world.getDifficulty().name());
                info.put("fullTime", world.getFullTime());
                info.put("time", world.getTime());
                info.put("weatherDuration", world.getWeatherDuration());
                info.put("thunderDuration", world.getThunderDuration());
                info.put("thundering", world.isThundering());
                info.put("storm", world.hasStorm());
                info.put("maxHeight", world.getMaxHeight());
                info.put("minHeight", world.getMinHeight());
                info.put("seaLevel", world.getSeaLevel());
                info.put("loadedChunks", world.getLoadedChunks().length);
                info.put("entityCount", world.getEntityCount());
                info.put("tileEntityCount", world.getTileEntityCount());

                result[0] = info;
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 获取世界高级信息时发生异常: " + e.getMessage());
                result[0] = Collections.emptyMap();
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            this.logger.warning("[BetterHTTPAPI] 等待获取世界高级信息任务完成时被中断");
            Thread.currentThread().interrupt();
            return Collections.emptyMap();
        }

        return result[0];
    }

    /**
     * 在世界中生成生物。
     *
     * <p>使用 Bukkit World API 在指定世界的指定位置生成指定类型和数量的生物。
     * 此操作在主线程执行，使用 CountDownLatch 等待最多 10 秒。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * Location mobLoc = new Location(Bukkit.getWorld("world"), 100, 64, 100);
     * boolean success = service.spawnMob("world", "ZOMBIE", 10, mobLoc);
     * }</pre>
     *
     * @param worldName 世界名称
     * @param mobType 生物类型（如 ZOMBIE, SKELETON, CREEPER 等）
     * @param amount 生成数量
     * @param location 生成位置
     * @return 生成成功返回 true，否则返回 false
     */
    public boolean spawnMob(final String worldName, final String mobType, final int amount, final Location location) {
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"), () -> {
            try {
                final World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    this.logger.warning("[BetterHTTPAPI] 世界不存在: " + worldName);
                    result[0] = false;
                    return;
                }

                if (amount <= 0) {
                    this.logger.warning("[BetterHTTPAPI] 生成数量必须大于 0");
                    result[0] = false;
                    return;
                }

                final org.bukkit.entity.EntityType entityType = org.bukkit.entity.EntityType.valueOf(mobType.toUpperCase());
                for (int i = 0; i < amount; i++) {
                    world.spawnEntity(location, entityType);
                }

                this.logger.info("[BetterHTTPAPI] 生物生成成功: " + amount + " 个 " + mobType + " 在 " + worldName);
                result[0] = true;
            } catch (IllegalArgumentException e) {
                this.logger.warning("[BetterHTTPAPI] 无效的生物类型: " + mobType);
                result[0] = false;
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 生成生物时发生异常: " + e.getMessage());
                result[0] = false;
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            this.logger.warning("[BetterHTTPAPI] 等待生成生物任务完成时被中断");
            Thread.currentThread().interrupt();
            return false;
        }

        return result[0];
    }

    /**
     * 反射调用对象的指定方法。
     *
     * @param obj 对象实例
     * @param methodName 方法名称
     * @return 方法调用的结果，如果发生异常返回 null
     */
    private Object invokeMethod(final Object obj, final String methodName) {
        try {
            final Method method = obj.getClass().getMethod(methodName);
            return method.invoke(obj);
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 调用方法 " + methodName + " 失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 解析环境类型字符串为环境枚举值。
     *
     * @param environment 环境字符串（NORMAL, NETHER, THE_END）
     * @return 环境枚举值，如果无效则返回 NORMAL
     */
    private Object parseEnvironment(final String environment) {
        try {
            if (environment == null || environment.trim().isEmpty()) {
                return Enum.valueOf((Class<? extends Enum>) this.environmentClass, "NORMAL");
            }

            final String envName = environment.toUpperCase().replace(" ", "_");
            return Enum.valueOf((Class<? extends Enum>) this.environmentClass, envName);
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 无效的环境类型: " + environment + "，使用默认值 NORMAL");
            try {
                return Enum.valueOf((Class<? extends Enum>) this.environmentClass, "NORMAL");
            } catch (Exception ex) {
                return null;
            }
        }
    }

    /**
     * 解析游戏模式字符串为游戏模式枚举值。
     *
     * @param gamemode 游戏模式字符串（SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR）
     * @return 游戏模式枚举值，如果无效则返回 SURVIVAL
     */
    private Object parseGameMode(final String gamemode) {
        try {
            if (gamemode == null || gamemode.trim().isEmpty()) {
                return Enum.valueOf((Class<? extends Enum>) this.gameModeClass, "SURVIVAL");
            }

            final String modeName = gamemode.toUpperCase().replace(" ", "_");
            return Enum.valueOf((Class<? extends Enum>) this.gameModeClass, modeName);
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 无效的游戏模式: " + gamemode + "，使用默认值 SURVIVAL");
            try {
                return Enum.valueOf((Class<? extends Enum>) this.gameModeClass, "SURVIVAL");
            } catch (Exception ex) {
                return null;
            }
        }
    }

    /**
     * 创建 CreateWorldOptions 实例。
     *
     * <p>使用反射调用 CreateWorldOptions 的构造方法和设置方法。</p>
     *
     * @param name 世界名称
     * @param environment 环境类型
     * @param seed 种子
     * @param gamemode 游戏模式
     * @return CreateWorldOptions 实例
     */
    private Object createWorldOptions(final String name, final Object environment, final long seed, final Object gamemode) {
        try {
            // 使用静态工厂方法创建选项
            // CreateWorldOptions.worldName(name).environment(env).seed(seed).gameMode(mode)
            final Method worldNameMethod = this.createWorldOptionsClass.getMethod("worldName", String.class);
            final Object options = worldNameMethod.invoke(null, name);

            if (environment != null) {
                final Method envMethod = this.createWorldOptionsClass.getMethod("environment", this.environmentClass);
                envMethod.invoke(options, environment);
            }

            final Method seedMethod = this.createWorldOptionsClass.getMethod("seed", long.class);
            seedMethod.invoke(options, seed);

            if (gamemode != null) {
                final Method gmMethod = this.createWorldOptionsClass.getMethod("gameMode", this.gameModeClass);
                gmMethod.invoke(options, gamemode);
            }

            return options;
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 创建世界选项时发生异常: " + e.getMessage());
            return null;
        }
    }
}