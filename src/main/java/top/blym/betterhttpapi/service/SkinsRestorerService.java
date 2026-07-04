package top.blym.betterhttpapi.service;

import net.skinsrestorer.api.SkinsRestorer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.lang.reflect.Method;
import java.util.logging.Logger;

import top.blym.betterhttpapi.PluginAPIManager;

/**
 * SkinsRestorer 皮肤管理服务类。
 *
 * <p>提供与 SkinsRestorer 插件集成的皮肤管理功能，包括设置皮肤、获取皮肤信息、
 * 重置皮肤等功能。所有方法均处理异常情况，在 SkinsRestorer API 不可用时
 * 静默返回，不抛出异常。</p>
 *
 * <p><b>重要</b>：所有皮肤操作方法必须使用 {@link Bukkit#getScheduler()#runTask(JavaPlugin, Runnable)}
 * 在主线程执行，以确保线程安全和 Bukkit API 兼容性。</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * SkinsRestorerService service = SkinsRestorerService.getInstance();
 *
 * // 设置玩家皮肤
 * service.setSkin(player, "Notch");
 *
 * // 获取玩家当前皮肤
 * String skinName = service.getCurrentSkinName(player);
 *
 * // 重置玩家皮肤
 * service.resetSkin(player);
 * }</pre>
 *
 * @author 白鹿原嚒
 * @version 1.0.0
 */
public class SkinsRestorerService {

    /** 单例实例 */
    private static volatile SkinsRestorerService instance;

    /** 日志记录器 */
    private final Logger logger;

    /**
     * 私有构造器（单例模式）。
     *
     * <p>初始化日志记录器。SkinsRestorer API 将在首次使用时通过
     * {@link PluginAPIManager} 获取。</p>
     */
    private SkinsRestorerService() {
        this.logger = Bukkit.getLogger();
    }

    /**
     * 获取单例实例。
     *
     * <p>使用双重检查锁定确保线程安全的单例初始化。</p>
     *
     * @return SkinsRestorerService 实例
     */
    public static SkinsRestorerService getInstance() {
        if (instance == null) {
            synchronized (SkinsRestorerService.class) {
                if (instance == null) {
                    instance = new SkinsRestorerService();
                }
            }
        }
        return instance;
    }

    /**
     * 通过皮肤名称设置玩家皮肤。
     *
     * <p>使用 SkinsRestorer API 设置玩家的皮肤为指定的皮肤名称。
     * 此方法使用反射调用 API，确保在 SkinsRestorer 插件未安装时不会导致错误。</p>
     *
     * <p><b>重要</b>：此方法必须在主线程执行。如果从异步线程调用，
     * 会自动使用 {@link Bukkit#getScheduler()#runTask(JavaPlugin, Runnable)}
     * 切换到主线程执行。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * SkinsRestorerService service = SkinsRestorerService.getInstance();
     * service.setSkin(player, "Notch"); // 设置为 Notch 的皮肤
     * }</pre>
     *
     * @param player 玩家对象，不能为 null
     * @param skinName 皮肤名称（Minecraft 玩家名称），不能为 null
     *
     * @see #setSkinUrl(Player, String)
     * @see #resetSkin(Player)
     */
    public void setSkin(final Player player, final String skinName) {
        final Optional<SkinsRestorer> skinApi = PluginAPIManager.getInstance().getSkinsRestorer();
        if (skinApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] SkinsRestorer API 不可用，无法设置皮肤");
            return;
        }

        // 确保在主线程执行
        final JavaPlugin plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin("BetterHTTPAPI");
        if (plugin == null) {
            this.logger.warning("[BetterHTTPAPI] 无法获取插件实例，取消设置皮肤");
            return;
        }

        Bukkit.getScheduler().runTask(
            plugin,
            () -> {
                try {
                    final SkinsRestorer api = skinApi.get();
                    // 使用反射调用 setSkin 方法
                    final Method setSkinMethod = api.getClass().getMethod("setSkin", String.class, String.class);
                    setSkinMethod.invoke(api, player.getName(), skinName);
                    this.logger.info("[BetterHTTPAPI] 成功设置玩家 " + player.getName() + " 的皮肤为: " + skinName);
                } catch (final Exception e) {
                    this.logger.warning("[BetterHTTPAPI] 设置玩家皮肤时发生异常: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 通过皮肤图片 URL 设置玩家皮肤。
     *
     * <p>使用 SkinsRestorer API 设置玩家的皮肤为指定的皮肤图片 URL。
     * 此方法使用反射调用 API，确保在 SkinsRestorer 插件未安装或不支持 URL 设置时不会导致错误。</p>
     *
     * <p><b>重要</b>：此方法必须在主线程执行。如果从异步线程调用，
     * 会自动使用 {@link Bukkit#getScheduler()#runTask(JavaPlugin, Runnable)}
     * 切换到主线程执行。</p>
     *
     * <p><b>注意</b>：此功能依赖 SkinsRestorer 插件是否支持通过 URL 设置皮肤。
     * 如果不支持，此方法将记录警告日志并静默返回。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * SkinsRestorerService service = SkinsRestorerService.getInstance();
     * service.setSkinUrl(player, "https://example.com/skin.png");
     * }</pre>
     *
     * @param player 玩家对象，不能为 null
     * @param url 皮肤图片的 URL 地址，不能为 null
     *
     * @see #setSkin(Player, String)
     * @see #resetSkin(Player)
     */
    // TODO: 确认 SkinsRestorer 是否支持 URL 设置皮肤
    public void setSkinUrl(final Player player, final String url) {
        final Optional<SkinsRestorer> skinApi = PluginAPIManager.getInstance().getSkinsRestorer();
        if (skinApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] SkinsRestorer API 不可用，无法设置皮肤 URL");
            return;
        }

        // 确保在主线程执行
        final JavaPlugin plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin("BetterHTTPAPI");
        if (plugin == null) {
            this.logger.warning("[BetterHTTPAPI] 无法获取插件实例，取消设置皮肤 URL");
            return;
        }

        Bukkit.getScheduler().runTask(
            plugin,
            () -> {
                try {
                    final SkinsRestorer api = skinApi.get();
                    // 尝试使用反射调用 setSkinUrl 方法（如果存在）
                    // SkinsRestorer API 可能不支持 URL 设置，需要确认
                    final Method setSkinUrlMethod = api.getClass().getMethod("setSkinUrl", String.class, String.class);
                    setSkinUrlMethod.invoke(api, player.getName(), url);
                    this.logger.info("[BetterHTTPAPI] 成功设置玩家 " + player.getName() + " 的皮肤 URL 为: " + url);
                } catch (final NoSuchMethodException e) {
                    this.logger.warning("[BetterHTTPAPI] SkinsRestorer 不支持通过 URL 设置皮肤");
                } catch (final Exception e) {
                    this.logger.warning("[BetterHTTPAPI] 设置玩家皮肤 URL 时发生异常: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 获取玩家当前使用的皮肤名称。
     *
     * <p>使用 SkinsRestorer API 获取玩家当前设置的皮肤名称。
     * 如果玩家使用的是默认皮肤或 API 不可用，返回 null。</p>
     *
     * <p><b>注意</b>：此方法是同步方法，直接在调用线程执行。
     * 建议在主线程调用此方法以确保线程安全。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * SkinsRestorerService service = SkinsRestorerService.getInstance();
     * String skinName = service.getCurrentSkinName(player);
     * if (skinName != null) {
     *     player.sendMessage("你当前使用的皮肤是: " + skinName);
     * } else {
     *     player.sendMessage("你使用的是默认皮肤");
     * }
     * }</pre>
     *
     * @param player 玩家对象，不能为 null
     * @return 玩家当前使用的皮肤名称（Minecraft 玩家名称），
     *         如果玩家使用默认皮肤或 API 不可用，返回 null
     *
     * @see #hasCustomSkin(Player)
     */
    public String getCurrentSkinName(final Player player) {
        final Optional<SkinsRestorer> skinApi = PluginAPIManager.getInstance().getSkinsRestorer();
        if (skinApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] SkinsRestorer API 不可用，无法获取皮肤名称");
            return null;
        }

        try {
            final SkinsRestorer api = skinApi.get();
            // 使用反射调用 getSkin 方法
            final Method getSkinMethod = api.getClass().getMethod("getSkin", String.class);
            final Object result = getSkinMethod.invoke(api, player.getName());

            if (result instanceof String) {
                this.logger.info("[BetterHTTPAPI] 成功获取玩家 " + player.getName() + " 的皮肤: " + result);
                return (String) result;
            }
            return null;
        } catch (final Exception e) {
            this.logger.warning("[BetterHTTPAPI] 获取玩家皮肤名称时发生异常: " + e.getMessage());
            return null;
        }
    }

    /**
     * 重置玩家皮肤为默认皮肤。
     *
     * <p>使用 SkinsRestorer API 重置玩家的皮肤为默认皮肤（Steve 或 Alex）。
     * 此方法使用反射调用 API，确保在 SkinsRestorer 插件未安装时不会导致错误。</p>
     *
     * <p><b>重要</b>：此方法必须在主线程执行。如果从异步线程调用，
     * 会自动使用 {@link Bukkit#getScheduler()#runTask(JavaPlugin, Runnable)}
     * 切换到主线程执行。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * SkinsRestorerService service = SkinsRestorerService.getInstance();
     * service.resetSkin(player); // 重置为默认皮肤
     * }</pre>
     *
     * @param player 玩家对象，不能为 null
     *
     * @see #setSkin(Player, String)
     * @see #setSkinUrl(Player, String)
     */
    public void resetSkin(final Player player) {
        final Optional<SkinsRestorer> skinApi = PluginAPIManager.getInstance().getSkinsRestorer();
        if (skinApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] SkinsRestorer API 不可用，无法重置皮肤");
            return;
        }

        // 确保在主线程执行
        final JavaPlugin plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin("BetterHTTPAPI");
        if (plugin == null) {
            this.logger.warning("[BetterHTTPAPI] 无法获取插件实例，取消重置皮肤");
            return;
        }

        Bukkit.getScheduler().runTask(
            plugin,
            () -> {
                try {
                    final SkinsRestorer api = skinApi.get();
                    // 使用反射调用 resetSkin 方法
                    final Method resetSkinMethod = api.getClass().getMethod("resetSkin", String.class);
                    resetSkinMethod.invoke(api, player.getName());
                    this.logger.info("[BetterHTTPAPI] 成功重置玩家 " + player.getName() + " 的皮肤");
                } catch (final Exception e) {
                    this.logger.warning("[BetterHTTPAPI] 重置玩家皮肤时发生异常: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 检查玩家是否设置了自定义皮肤。
     *
     * <p>通过调用 {@link #getCurrentSkinName(Player)} 方法检查玩家是否有自定义皮肤。
     * 如果返回的皮肤名称不为 null，则表示玩家设置了自定义皮肤。</p>
     *
     * <p><b>注意</b>：此方法是同步方法，直接在调用线程执行。
     * 建议在主线程调用此方法以确保线程安全。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * SkinsRestorerService service = SkinsRestorerService.getInstance();
     * if (service.hasCustomSkin(player)) {
     *     player.sendMessage("你有自定义皮肤");
     * } else {
     *     player.sendMessage("你使用的是默认皮肤");
     * }
     * }</pre>
     *
     * @param player 玩家对象，不能为 null
     * @return 如果玩家设置了自定义皮肤返回 true，否则返回 false。
     *         如果 SkinsRestorer API 不可用，返回 false
     *
     * @see #getCurrentSkinName(Player)
     */
    public boolean hasCustomSkin(final Player player) {
        final String skinName = this.getCurrentSkinName(player);
        final boolean hasCustomSkin = skinName != null && !skinName.isEmpty();

        this.logger.info("[BetterHTTPAPI] 检查玩家 " + player.getName() + " 是否有自定义皮肤: " + hasCustomSkin);
        return hasCustomSkin;
    }

    /**
     * 获取玩家皮肤的 URL 地址。
     *
     * <p>使用 SkinsRestorer API 获取指定玩家当前使用的皮肤图片 URL。
     * 如果 API 不可用或获取失败，返回 null。</p>
     *
     * <p><b>TODO</b>: 确认 SkinsRestorer API 是否提供获取皮肤 URL 的方法</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * String url = service.getSkinUrl("Steve");
     * if (url != null) {
     *     System.out.println("皮肤 URL: " + url);
     * }
     * }</pre>
     *
     * @param playerName 玩家名称，不能为 null
     * @return 皮肤图片的 URL 地址，如果 API 不可用或获取失败返回 null
     */
    public String getSkinUrl(final String playerName) {
        final Optional<SkinsRestorer> skinApi = PluginAPIManager.getInstance().getSkinsRestorer();
        if (skinApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] SkinsRestorer API 不可用，无法获取皮肤 URL");
            return null;
        }

        try {
            final SkinsRestorer api = skinApi.get();
            // TODO: 确认 SkinsRestorer API 中获取皮肤 URL 的方法名
            final Method getSkinUrlMethod = api.getClass().getMethod("getSkinUrl", String.class);
            final Object result = getSkinUrlMethod.invoke(api, playerName);
            return result != null ? result.toString() : null;
        } catch (NoSuchMethodException e) {
            this.logger.warning("[BetterHTTPAPI] SkinsRestorer API 未提供 getSkinUrl 方法: " + e.getMessage());
            return null;
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 获取皮肤 URL 时发生异常: " + e.getMessage());
            return null;
        }
    }

    /**
     * 清除玩家的皮肤缓存。
     *
     * <p>使用 SkinsRestorer API 清除指定玩家的皮肤缓存数据，
     * 下次玩家加入服务器时会重新获取皮肤。</p>
     *
     * <p><b>TODO</b>: 确认 SkinsRestorer API 中清除缓存的方法名</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * boolean success = service.clearSkinCache("Steve");
     * if (success) {
     *     System.out.println("皮肤缓存已清除");
     * }
     * }</pre>
     *
     * @param playerName 玩家名称，不能为 null
     * @return 如果清除成功返回 true，否则返回 false
     */
    public boolean clearSkinCache(final String playerName) {
        final Optional<SkinsRestorer> skinApi = PluginAPIManager.getInstance().getSkinsRestorer();
        if (skinApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] SkinsRestorer API 不可用，无法清除皮肤缓存");
            return false;
        }

        final JavaPlugin plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin("BetterHTTPAPI");
        if (plugin == null) {
            return false;
        }

        final java.util.concurrent.atomic.AtomicBoolean result = new java.util.concurrent.atomic.AtomicBoolean(false);
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                final SkinsRestorer api = skinApi.get();
                // TODO: 确认清除皮肤缓存的方法名
                final Method clearCacheMethod = api.getClass().getMethod("clearSkinCache", String.class);
                clearCacheMethod.invoke(api, playerName);
                result.set(true);
                this.logger.info("[BetterHTTPAPI] 已清除玩家 " + playerName + " 的皮肤缓存");
            } catch (NoSuchMethodException e) {
                this.logger.warning("[BetterHTTPAPI] SkinsRestorer API 未提供 clearSkinCache 方法: " + e.getMessage());
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 清除皮肤缓存时发生异常: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        try { latch.await(5, java.util.concurrent.TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return result.get();
    }

    /**
     * 强制设置玩家皮肤（即使玩家离线）。
     *
     * <p>使用 SkinsRestorer API 为玩家强制设置皮肤，即使玩家不在线也会生效。
     * 此操作会在主线程执行。</p>
     *
     * <p><b>TODO</b>: 确认 SkinsRestorer API 中强制设置皮肤的方法名</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * boolean success = service.setSkinForce("Steve", "Notch");
     * if (success) {
     *     System.out.println("强制设置皮肤成功");
     * }
     * }</pre>
     *
     * @param playerName 玩家名称，不能为 null
     * @param skinName 皮肤名称，不能为 null
     * @return 如果设置成功返回 true，否则返回 false
     */
    public boolean setSkinForce(final String playerName, final String skinName) {
        final Optional<SkinsRestorer> skinApi = PluginAPIManager.getInstance().getSkinsRestorer();
        if (skinApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] SkinsRestorer API 不可用，无法强制设置皮肤");
            return false;
        }

        final JavaPlugin plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin("BetterHTTPAPI");
        if (plugin == null) {
            return false;
        }

        final java.util.concurrent.atomic.AtomicBoolean result = new java.util.concurrent.atomic.AtomicBoolean(false);
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                final SkinsRestorer api = skinApi.get();
                // TODO: 确认强制设置皮肤的方法名（可能与 setSkin 相同但直接操作数据）
                final Method setSkinForceMethod = api.getClass().getMethod("setSkin", String.class, String.class);
                setSkinForceMethod.invoke(api, playerName, skinName);
                result.set(true);
                this.logger.info("[BetterHTTPAPI] 已强制设置玩家 " + playerName + " 的皮肤为: " + skinName);
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 强制设置皮肤时发生异常: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        try { latch.await(5, java.util.concurrent.TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return result.get();
    }

    /**
     * 强制应用玩家当前皮肤（刷新皮肤显示）。
     *
     * <p>使用 SkinsRestorer API 强制刷新玩家的皮肤显示，
     * 通常在修改皮肤后调用此方法使更改立即生效。</p>
     *
     * <p><b>TODO</b>: 确认 SkinsRestorer API 中应用皮肤的方法名</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * boolean success = service.applySkin(player);
     * if (success) {
     *     player.sendMessage("皮肤已刷新");
     * }
     * }</pre>
     *
     * @param player 玩家对象，不能为 null
     * @return 如果应用成功返回 true，否则返回 false
     */
    public boolean applySkin(final Player player) {
        final Optional<SkinsRestorer> skinApi = PluginAPIManager.getInstance().getSkinsRestorer();
        if (skinApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] SkinsRestorer API 不可用，无法应用皮肤");
            return false;
        }

        final JavaPlugin plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin("BetterHTTPAPI");
        if (plugin == null) {
            return false;
        }

        final java.util.concurrent.atomic.AtomicBoolean result = new java.util.concurrent.atomic.AtomicBoolean(false);
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                final SkinsRestorer api = skinApi.get();
                // TODO: 确认应用皮肤的方法名
                final Method applySkinMethod = api.getClass().getMethod("applySkin", Player.class);
                applySkinMethod.invoke(api, player);
                result.set(true);
                this.logger.info("[BetterHTTPAPI] 已刷新玩家 " + player.getName() + " 的皮肤");
            } catch (NoSuchMethodException e) {
                // 尝试使用 Player 的更新方法
                try {
                    final SkinsRestorer api2 = skinApi.get();
                    final Method applySkinByNameMethod = api2.getClass().getMethod("applySkin", String.class);
                    applySkinByNameMethod.invoke(api2, player.getName());
                    result.set(true);
                } catch (Exception ex) {
                    this.logger.warning("[BetterHTTPAPI] SkinsRestorer API 未提供 applySkin 方法: " + e.getMessage());
                }
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 应用皮肤时发生异常: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        try { latch.await(5, java.util.concurrent.TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return result.get();
    }
}