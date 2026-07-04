package top.blym.betterhttpapi;

import java.util.Optional;

import fr.xephi.authme.api.v3.AuthMeApi;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 插件 API 管理器。
 * <p>
 * 提供统一的接口来获取其他插件（如 LuckPerms、Multiverse-Core、PlayerTitle、
 * SkinsRestorer、Residence）的 API 实例。所有 API 获取均为可选，如果插件未安装
 * 或获取失败则返回 {@link Optional#empty()}。
 * </p>
 *
 * <p>该类使用单例模式，通过 {@link #getInstance()} 获取实例，并通过 {@link #init(JavaPlugin)}
 * 进行初始化。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * PluginAPIManager apiManager = plugin.getPluginAPIManager();
 * Optional<LuckPerms> luckPerms = apiManager.getLuckPerms();
 * luckPerms.ifPresent(api -> {
 *     // 使用 LuckPerms API
 * });
 * }</pre>
 *
 * @author 白鹿原嚒
 * @version 1.0.0
 */
public final class PluginAPIManager {

    /** 单例实例 */
    private static PluginAPIManager instance;

    /** 插件实例引用 */
    private JavaPlugin plugin;

    /** LuckPerms API 缓存 */
    private Optional<LuckPerms> luckPermsCache;

    /** Multiverse-Core API 缓存 */
    private Optional<Object> multiverseCoreCache;

    /** PlayerTitle API 缓存 */
    private Optional<Object> playerTitleCache;

    /** SkinsRestorer API 缓存 */
    private Optional<SkinsRestorer> skinsRestorerCache;

    /** Residence API 缓存 */
    private Optional<Object> residenceCache;

    /** AuthMe API 缓存 */
    private Optional<AuthMeApi> authMeApiCache;

    /**
     * 私有构造器，防止外部实例化。
     */
    private PluginAPIManager() {
        this.luckPermsCache = null;
        this.multiverseCoreCache = null;
        this.playerTitleCache = null;
        this.skinsRestorerCache = null;
        this.residenceCache = null;
        this.authMeApiCache = null;
    }

    /**
     * 获取 PluginAPIManager 单例实例。
     * <p>
     * 在调用此方法之前，必须先调用 {@link #init(JavaPlugin)} 进行初始化。
     * </p>
     *
     * @return PluginAPIManager 实例
     * @throws IllegalStateException 如果未初始化
     */
    public static PluginAPIManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PluginAPIManager 未初始化，请先调用 init() 方法");
        }
        return instance;
    }

    /**
     * 初始化 PluginAPIManager。
     * <p>
     * 此方法应在插件启动时调用（通常在 {@code onEnable()} 中）。
     * 只能初始化一次，重复调用将抛出异常。
     * </p>
     *
     * @param plugin JavaPlugin 实例
     * @throws IllegalStateException 如果已经初始化
     * @throws IllegalArgumentException 如果 plugin 为 null
     */
    public static void init(final JavaPlugin plugin) {
        if (instance != null) {
            throw new IllegalStateException("PluginAPIManager 已经初始化");
        }
        if (plugin == null) {
            throw new IllegalArgumentException("plugin 不能为 null");
        }
        instance = new PluginAPIManager();
        instance.plugin = plugin;
    }

    /**
     * 获取 LuckPerms API 实例。
     * <p>
     * 如果服务器未安装 LuckPerms 插件或获取失败，返回 {@link Optional#empty()}。
     * 结果会被缓存，后续调用直接返回缓存值。
     * </p>
     *
     * @return 包含 LuckPerms API 的 Optional，或空 Optional
     * 
     * <h4>使用示例：</h4>
     * <pre>{@code
     * Optional<LuckPerms> api = manager.getLuckPerms();
     * if (api.isPresent()) {
     *     LuckPerms luckPerms = api.get();
     *     // 使用 API...
     * }
     * }</pre>
     */
    public Optional<LuckPerms> getLuckPerms() {
        if (this.luckPermsCache != null) {
            return this.luckPermsCache;
        }

        try {
            final LuckPerms api = LuckPermsProvider.get();
            this.luckPermsCache = Optional.ofNullable(api);
            this.plugin.getLogger().info("成功加载 LuckPerms API");
        } catch (final IllegalStateException | NoClassDefFoundError e) {
            this.plugin.getLogger().warning("无法加载 LuckPerms API: " + e.getMessage());
            this.luckPermsCache = Optional.empty();
        }

        return this.luckPermsCache;
    }

    /**
     * 获取 Multiverse-Core API 实例。
     * <p>
     * 通过 Bukkit 服务管理器获取 Multiverse-Core API。如果服务器未安装
     * Multiverse-Core 插件或获取失败，返回 {@link Optional#empty()}。
     * 结果会被缓存，后续调用直接返回缓存值。
     * </p>
     *
     * <p>注意：此方法必须在主线程调用，因为它使用了 Bukkit 的服务管理器。</p>
     *
     * <p>返回类型为 Object，调用者需要自行转换为 Multiverse-Core API 类型。</p>
     *
     * @return 包含 Multiverse-Core API 的 Optional，或空 Optional
     * 
     * <h4>使用示例：</h4>
     * <pre>{@code
     * Optional<Object> api = manager.getMultiverseCore();
     * if (api.isPresent()) {
     *     // 转换为具体类型使用（需要导入 Multiverse-Core API）
     *     // MultiverseCoreApi mvCore = (MultiverseCoreApi) api.get();
     * }
     * }</pre>
     */
    public Optional<Object> getMultiverseCore() {
        if (this.multiverseCoreCache != null) {
            return this.multiverseCoreCache;
        }

        try {
            // 使用反射获取 Multiverse-Core API 类
            final Class<?> apiClass = Class.forName("com.onarandombox.MultiverseCore.api.MultiverseCoreApi");
            final var registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration != null) {
                final Object api = registration.getProvider();
                this.multiverseCoreCache = Optional.ofNullable(api);
                this.plugin.getLogger().info("成功加载 Multiverse-Core API");
            } else {
                this.multiverseCoreCache = Optional.empty();
            }
        } catch (final ClassNotFoundException | NoClassDefFoundError e) {
            this.plugin.getLogger().warning("无法加载 Multiverse-Core API: " + e.getMessage());
            this.multiverseCoreCache = Optional.empty();
        }

        return this.multiverseCoreCache;
    }

    /**
     * 获取 PlayerTitle API 实例。
     * <p>
     * 通过插件管理器获取 PlayerTitle 插件实例并提取其 API。
     * 如果服务器未安装 PlayerTitle 插件或获取失败，返回 {@link Optional#empty()}。
     * 结果会被缓存，后续调用直接返回缓存值。
     * </p>
     *
     * <p>返回类型为 Object，调用者需要自行转换为 PlayerTitle 的 API 类型。</p>
     *
     * @return 包含 PlayerTitle API 的 Optional，或空 Optional
     * 
     * <h4>使用示例：</h4>
     * <pre>{@code
     * Optional<Object> api = manager.getPlayerTitleAPI();
     * if (api.isPresent()) {
     *     // 转换为具体类型使用
     *     // PlayerTitleAPI titleApi = (PlayerTitleAPI) api.get();
     * }
     * }</pre>
     */
    public Optional<Object> getPlayerTitleAPI() {
        if (this.playerTitleCache != null) {
            return this.playerTitleCache;
        }

        try {
            final var plugin = Bukkit.getPluginManager().getPlugin("PlayerTitle");
            if (plugin != null) {
                // 使用反射调用 getAPI() 方法
                final java.lang.reflect.Method getApiMethod = plugin.getClass().getMethod("getAPI");
                final Object api = getApiMethod.invoke(plugin);
                this.playerTitleCache = Optional.ofNullable(api);
                this.plugin.getLogger().info("成功加载 PlayerTitle API");
            } else {
                this.playerTitleCache = Optional.empty();
            }
        } catch (final Exception e) {
            this.plugin.getLogger().warning("无法加载 PlayerTitle API: " + e.getMessage());
            this.playerTitleCache = Optional.empty();
        }

        return this.playerTitleCache;
    }

    /**
     * 获取 SkinsRestorer API 实例。
     * <p>
     * 如果服务器未安装 SkinsRestorer 插件或获取失败，返回 {@link Optional#empty()}。
     * 结果会被缓存，后续调用直接返回缓存值。
     * </p>
     *
     * @return 包含 SkinsRestorer 的 Optional，或空 Optional
     * 
     * <h4>使用示例：</h4>
     * <pre>{@code
     * Optional<SkinsRestorer> api = manager.getSkinsRestorer();
     * if (api.isPresent()) {
     *     SkinsRestorer skinsApi = api.get();
     *     // 使用 API...
     * }
     * }</pre>
     */
    public Optional<SkinsRestorer> getSkinsRestorer() {
        if (this.skinsRestorerCache != null) {
            return this.skinsRestorerCache;
        }

        try {
            final SkinsRestorer api = SkinsRestorerProvider.get();
            this.skinsRestorerCache = Optional.ofNullable(api);
            this.plugin.getLogger().info("成功加载 SkinsRestorer API");
        } catch (final IllegalStateException | NoClassDefFoundError e) {
            this.plugin.getLogger().warning("无法加载 SkinsRestorer API: " + e.getMessage());
            this.skinsRestorerCache = Optional.empty();
        }

        return this.skinsRestorerCache;
    }

    /**
     * 获取 Residence API 实例。
     * <p>
     * 如果服务器未安装 Residence 插件或获取失败，返回 {@link Optional#empty()}。
     * 结果会被缓存，后续调用直接返回缓存值。
     * </p>
     *
     * <p>返回类型为 Object，调用者需要自行转换为 Residence 的 API 类型。</p>
     *
     * @return 包含 Residence API 的 Optional，或空 Optional
     * 
     * <h4>使用示例：</h4>
     * <pre>{@code
     * Optional<Object> api = manager.getResidenceAPI();
     * if (api.isPresent()) {
     *     // 转换为具体类型使用
     *     // ResidenceApi resApi = (ResidenceApi) api.get();
     * }
     * }</pre>
     */
    public Optional<Object> getResidenceAPI() {
        if (this.residenceCache != null) {
            return this.residenceCache;
        }

        try {
            // 使用反射获取 Residence 插件实例和 API
            final Class<?> residenceClass = Class.forName("com.bekvon.bukkit.residence.Residence");
            final java.lang.reflect.Method getInstanceMethod = residenceClass.getMethod("getInstance");
            final Object residenceInstance = getInstanceMethod.invoke(null);
            
            if (residenceInstance != null) {
                final java.lang.reflect.Method getApiMethod = residenceClass.getMethod("getAPI");
                final Object api = getApiMethod.invoke(residenceInstance);
                this.residenceCache = Optional.ofNullable(api);
                this.plugin.getLogger().info("成功加载 Residence API");
            } else {
                this.residenceCache = Optional.empty();
            }
        } catch (final Exception e) {
            this.plugin.getLogger().warning("无法加载 Residence API: " + e.getMessage());
            this.residenceCache = Optional.empty();
        }

        return this.residenceCache;
    }

    /**
     * 获取 AuthMe API 实例。
     * <p>
     * 使用 AuthMeApi.getInstance() 获取 API。如果 AuthMe 插件未安装或 API 不可用，
     * 返回 {@link Optional#empty()}。结果会被缓存，后续调用直接返回缓存值。
     * </p>
     *
     * @return 包含 AuthMeApi 的 Optional，或空 Optional
     */
    public Optional<AuthMeApi> getAuthMeApi() {
        if (this.authMeApiCache != null) {
            return this.authMeApiCache;
        }

        try {
            final AuthMeApi api = AuthMeApi.getInstance();
            this.authMeApiCache = Optional.ofNullable(api);
            this.plugin.getLogger().info("成功加载 AuthMe API");
        } catch (final IllegalStateException | NoClassDefFoundError e) {
            this.plugin.getLogger().warning("无法加载 AuthMe API: " + e.getMessage());
            this.authMeApiCache = Optional.empty();
        }

        return this.authMeApiCache;
    }

    /**
     * 检查指定插件是否已加载。
     * <p>
     * 使用 Bukkit 插件管理器检查插件是否存在且已启用。
     * </p>
     *
     * @param pluginName 插件名称（区分大小写）
     * @return 如果插件已加载返回 true，否则返回 false
     * 
     * <h4>使用示例：</h4>
     * <pre>{@code
     * if (manager.isPluginLoaded("LuckPerms")) {
     *     Optional<LuckPerms> api = manager.getLuckPerms();
     *     // ...
     * }
     * }</pre>
     */
    public boolean isPluginLoaded(final String pluginName) {
        return Bukkit.getPluginManager().getPlugin(pluginName) != null;
    }
}