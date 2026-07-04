package top.blym.betterhttpapi.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import top.blym.betterhttpapi.PluginAPIManager;

/**
 * Residence 领地服务类。
 *
 * <p>提供与 Residence 插件集成的领地管理功能，包括领地检查、所有者查询、
 * 玩家领地列表、权限检查以及领地信息查询等功能。所有方法均使用反射调用 Residence API，
 * 确保在插件未安装时安全降级。</p>
 *
 * <p><b>重要说明</b>：所有涉及领地操作的 Bukkit API 调用均应在主线程执行。
 * 部分方法内部已包含主线程调度，但调用者仍需注意线程安全。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * ResidenceService service = ResidenceService.getInstance();
 *
 * // 检查位置是否在领地内
 * boolean inRes = service.isInResidence(location);
 *
 * // 获取领地所有者
 * String owner = service.getResidenceOwner(location);
 *
 * // 获取玩家领地列表
 * List<String> residences = service.getPlayerResidences(playerUuid);
 *
 * // 检查玩家权限
 * boolean hasPerm = service.hasPermission(player, location, "build");
 *
 * // 获取领地详细信息
 * Map<String, Object> info = service.getResidenceInfo("MyResidence");
 * }</pre>
 *
 * @author 白鹿原嚒
 * @version 1.0.0
 */
public class ResidenceService {

    /** 单例实例 */
    private static volatile ResidenceService instance;

    /** 日志记录器 */
    private final Logger logger;

    /**
     * 私有构造器（单例模式）。
     *
     * <p>初始化日志记录器。Residence API 的实际获取延迟到方法调用时。</p>
     */
    private ResidenceService() {
        this.logger = Bukkit.getLogger();
    }

    /**
     * 获取单例实例。
     *
     * <p>使用双重检查锁定确保线程安全的单例初始化。</p>
     *
     * @return ResidenceService 实例
     */
    public static ResidenceService getInstance() {
        if (instance == null) {
            synchronized (ResidenceService.class) {
                if (instance == null) {
                    instance = new ResidenceService();
                }
            }
        }
        return instance;
    }

    /**
     * 检查指定位置是否在领地内。
     *
     * <p>通过 Residence API 获取位置所在的领地，判断该位置是否属于任何领地。
     * 如果 Residence API 不可用或发生异常，返回 false。</p>
     *
     * <p><b>线程要求</b>：此方法包含 Bukkit API 调用，应在主线程执行。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * Location loc = new Location(world, 100, 64, 200);
     * boolean inResidence = service.isInResidence(loc);
     * if (inResidence) {
     *     player.sendMessage("此位置在领地内！");
     * }
     * }</pre>
     *
     * @param location 要检查的位置，不能为 null
     * @return 如果位置在领地内返回 true，否则返回 false。
     *         如果 Residence API 不可用或发生异常，返回 false
     */
    public boolean isInResidence(final Location location) {
        final Optional<Object> resApi = PluginAPIManager.getInstance().getResidenceAPI();
        if (resApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] Residence API 不可用，无法检查领地");
            return false;
        }

        try {
            final Object api = resApi.get();

            // TODO: 确认实际方法名 - 假设为 getResidenceManager()
            final Method getResidenceManagerMethod = api.getClass().getMethod("getResidenceManager");
            final Object residenceManager = getResidenceManagerMethod.invoke(api);

            // TODO: 确认实际方法名 - 假设为 getByLoc(Location)
            final Method getByLocMethod = residenceManager.getClass().getMethod("getByLoc", Location.class);
            final Object residence = getByLocMethod.invoke(residenceManager, location);

            return residence != null;
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 检查位置是否在领地内时发生异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取指定位置所在领地的所有者名称。
     *
     * <p>通过 Residence API 获取位置所在的领地，然后提取领地所有者的名称。
     * 如果 Residence API 不可用、位置不在领地内或发生异常，返回 null。</p>
     *
     * <p><b>线程要求</b>：此方法包含 Bukkit API 调用，应在主线程执行。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * Location loc = new Location(world, 100, 64, 200);
     * String owner = service.getResidenceOwner(loc);
     * if (owner != null) {
     *     player.sendMessage("此领地的所有者是: " + owner);
     * } else {
     *     player.sendMessage("此位置不在领地内");
     * }
     * }</pre>
     *
     * @param location 要检查的位置，不能为 null
     * @return 领地所有者的名称，如果位置不在领地内或发生异常，返回 null
     */
    public String getResidenceOwner(final Location location) {
        final Optional<Object> resApi = PluginAPIManager.getInstance().getResidenceAPI();
        if (resApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] Residence API 不可用，无法获取领地所有者");
            return null;
        }

        try {
            final Object api = resApi.get();

            // TODO: 确认实际方法名 - 假设为 getResidenceManager()
            final Method getResidenceManagerMethod = api.getClass().getMethod("getResidenceManager");
            final Object residenceManager = getResidenceManagerMethod.invoke(api);

            // TODO: 确认实际方法名 - 假设为 getByLoc(Location)
            final Method getByLocMethod = residenceManager.getClass().getMethod("getByLoc", Location.class);
            final Object residence = getByLocMethod.invoke(residenceManager, location);

            if (residence == null) {
                return null;
            }

            // TODO: 确认实际方法名 - 假设为 getOwner()
            final Method getOwnerMethod = residence.getClass().getMethod("getOwner");
            final Object owner = getOwnerMethod.invoke(residence);

            return owner != null ? owner.toString() : null;
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 获取领地所有者时发生异常: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取指定玩家拥有的所有领地名称列表。
     *
     * <p>通过 Residence API 获取玩家的所有领地，返回领地名称列表。
     * 如果 Residence API 不可用或发生异常，返回空列表。</p>
     *
     * <p><b>线程要求</b>：此方法包含 Bukkit API 调用，应在主线程执行。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * UUID playerUuid = player.getUniqueId();
     * List<String> residences = service.getPlayerResidences(playerUuid);
     * residences.forEach(name -> {
     *     System.out.println("领地: " + name);
     * });
     * }</pre>
     *
     * @param player 玩家的 UUID，不能为 null
     * @return 领地名称列表，如果玩家没有领地或发生异常，返回空列表
     */
    public List<String> getPlayerResidences(final UUID player) {
        final Optional<Object> resApi = PluginAPIManager.getInstance().getResidenceAPI();
        if (resApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] Residence API 不可用，无法获取玩家领地列表");
            return Collections.emptyList();
        }

        try {
            final Object api = resApi.get();

            // TODO: 确认实际方法名 - 假设为 getResidenceManager()
            final Method getResidenceManagerMethod = api.getClass().getMethod("getResidenceManager");
            final Object residenceManager = getResidenceManagerMethod.invoke(api);

            // TODO: 确认实际方法名 - 假设为 getResidencesMap() 或类似方法
            // 可能需要遍历所有领地并过滤出属于该玩家的领地
            final Method getResidencesMethod = residenceManager.getClass().getMethod("getResidences");
            final Object residences = getResidencesMethod.invoke(residenceManager);

            if (residences instanceof Map) {
                @SuppressWarnings("unchecked")
                final Map<String, ?> residenceMap = (Map<String, ?>) residences;

                return residenceMap.entrySet().stream()
                    .filter(entry -> {
                        try {
                            final Object residence = entry.getValue();
                            // TODO: 确认实际方法名 - 假设为 getOwner()
                            final Method getOwnerMethod = residence.getClass().getMethod("getOwner");
                            final Object owner = getOwnerMethod.invoke(residence);

                            if (owner != null) {
                                // 可能是 UUID 字符串或玩家名称
                                final String ownerStr = owner.toString();
                                return ownerStr.equals(player.toString()) ||
                                       ownerStr.equals(Bukkit.getOfflinePlayer(player).getName());
                            }
                            return false;
                        } catch (Exception e) {
                            this.logger.warning("[BetterHTTPAPI] 过滤玩家领地时发生异常: " + e.getMessage());
                            return false;
                        }
                    })
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            }

            return Collections.emptyList();
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 获取玩家领地列表时发生异常: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 检查玩家在指定位置是否拥有特定权限。
     *
     * <p>通过 Residence API 检查玩家在指定位置的领地权限（如 build、destroy、use 等）。
     * 如果 Residence API 不可用、位置不在领地内或发生异常，返回 false。</p>
     *
     * <p><b>线程要求</b>：此方法包含 Bukkit API 调用，应在主线程执行。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * boolean canBuild = service.hasPermission(player, location, "build");
     * if (canBuild) {
     *     player.sendMessage("你可以在此领地内建造");
     * } else {
     *     player.sendMessage("你没有权限在此领地内建造");
     * }
     * }</pre>
     *
     * @param player 玩家对象，不能为 null
     * @param location 要检查的位置，不能为 null
     * @param flag 权限标志（如 build、destroy、use、container 等），不能为 null
     * @return 如果玩家拥有指定权限返回 true，否则返回 false。
     *         如果 Residence API 不可用或发生异常，返回 false
     */
    public boolean hasPermission(final Player player, final Location location, final String flag) {
        final Optional<Object> resApi = PluginAPIManager.getInstance().getResidenceAPI();
        if (resApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] Residence API 不可用，无法检查玩家权限");
            return false;
        }

        try {
            final Object api = resApi.get();

            // TODO: 确认实际方法名 - 假设为 getResidenceManager()
            final Method getResidenceManagerMethod = api.getClass().getMethod("getResidenceManager");
            final Object residenceManager = getResidenceManagerMethod.invoke(api);

            // TODO: 确认实际方法名 - 假设为 getByLoc(Location)
            final Method getByLocMethod = residenceManager.getClass().getMethod("getByLoc", Location.class);
            final Object residence = getByLocMethod.invoke(residenceManager, location);

            if (residence == null) {
                return false;
            }

            // TODO: 确认实际方法名 - 假设为 getPermissions() 或类似方法
            final Method getPermsMethod = residence.getClass().getMethod("getPermissions");
            final Object permissions = getPermsMethod.invoke(residence);

            if (permissions == null) {
                return false;
            }

            // TODO: 确认实际方法名 - 假设为 playerHas(String playerName, String flag, boolean def)
            final Method playerHasMethod = permissions.getClass().getMethod(
                "playerHas", String.class, String.class, boolean.class
            );
            final Object result = playerHasMethod.invoke(permissions, player.getName(), flag, false);

            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 检查玩家领地权限时发生异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取指定领地的详细信息。
     *
     * <p>通过 Residence API 获取领地的详细信息，包括所有者、成员、面积、子领地数量、世界等。
     * 如果 Residence API 不可用、领地不存在或发生异常，返回空 Map。</p>
     *
     * <p><b>线程要求</b>：此方法包含 Bukkit API 调用，应在主线程执行。</p>
     *
     * <p>返回的信息包括：</p>
     * <ul>
     *   <li>name - 领地名称</li>
     *   <li>owner - 所有者名称</li>
     *   <li>world - 所在世界名称</li>
     *   <li>area - 领地面积（块数）</li>
     *   <li>members - 成员列表</li>
     *   <li>subzones - 子领地数量</li>
     *   <li>permissions - 权限设置</li>
     * </ul>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * Map<String, Object> info = service.getResidenceInfo("MyResidence");
     * if (!info.isEmpty()) {
     *     System.out.println("领地名称: " + info.get("name"));
     *     System.out.println("所有者: " + info.get("owner"));
     *     System.out.println("面积: " + info.get("area"));
     * }
     * }</pre>
     *
     * @param residenceName 领地名称，不能为 null 或空
     * @return 包含领地信息的 Map，如果领地不存在或发生异常，返回空 Map
     */
    public Map<String, Object> getResidenceInfo(final String residenceName) {
        final Optional<Object> resApi = PluginAPIManager.getInstance().getResidenceAPI();
        if (resApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] Residence API 不可用，无法获取领地信息");
            return Collections.emptyMap();
        }

        try {
            final Object api = resApi.get();

            // TODO: 确认实际方法名 - 假设为 getResidenceManager()
            final Method getResidenceManagerMethod = api.getClass().getMethod("getResidenceManager");
            final Object residenceManager = getResidenceManagerMethod.invoke(api);

            // TODO: 确认实际方法名 - 假设为 getByName(String)
            final Method getByNameMethod = residenceManager.getClass().getMethod("getByName", String.class);
            final Object residence = getByNameMethod.invoke(residenceManager, residenceName);

            if (residence == null) {
                this.logger.warning("[BetterHTTPAPI] 领地不存在: " + residenceName);
                return Collections.emptyMap();
            }

            // 构建领地信息 Map
            final Map<String, Object> info = new HashMap<>();

            // 获取领地名称
            info.put("name", residenceName);

            // 获取所有者
            final Method getOwnerMethod = residence.getClass().getMethod("getOwner");
            final Object owner = getOwnerMethod.invoke(residence);
            info.put("owner", owner != null ? owner.toString() : "Unknown");

            // 获取所在世界
            final Method getWorldMethod = residence.getClass().getMethod("getWorld");
            final Object world = getWorldMethod.invoke(residence);
            if (world != null) {
                final Method getWorldNameMethod = world.getClass().getMethod("getName");
                final String worldName = (String) getWorldNameMethod.invoke(world);
                info.put("world", worldName);
            }

            // 获取领地面积
            try {
                final Method getAreaMethod = residence.getClass().getMethod("getArea");
                final Object area = getAreaMethod.invoke(residence);
                info.put("area", area != null ? area : 0);
            } catch (NoSuchMethodException e) {
                // 如果没有 getArea 方法，尝试获取坐标范围计算面积
                this.logger.warning("[BetterHTTPAPI] 领地面积计算方法不可用: " + e.getMessage());
            }

            // 获取成员列表
            try {
                final Method getPlayersInResidenceMethod = residence.getClass().getMethod("getPlayersInResidence");
                final Object players = getPlayersInResidenceMethod.invoke(residence);
                if (players instanceof List) {
                    @SuppressWarnings("unchecked")
                    final List<?> playerList = (List<?>) players;
                    info.put("playersInResidence", playerList.size());
                }
            } catch (NoSuchMethodException e) {
                // 方法不存在，跳过
            }

            // 获取子领地数量
            try {
                final Method getSubzonesMethod = residence.getClass().getMethod("getSubzones");
                final Object subzones = getSubzonesMethod.invoke(residence);
                if (subzones instanceof Map) {
                    @SuppressWarnings("unchecked")
                    final Map<?, ?> subzoneMap = (Map<?, ?>) subzones;
                    info.put("subzones", subzoneMap.size());
                }
            } catch (NoSuchMethodException e) {
                // 方法不存在，跳过
            }

            // 获取权限设置
            try {
                final Method getPermissionsMethod = residence.getClass().getMethod("getPermissions");
                final Object permissions = getPermissionsMethod.invoke(residence);
                if (permissions != null) {
                    // 转换权限对象为字符串表示
                    info.put("permissions", permissions.toString());
                }
            } catch (NoSuchMethodException e) {
                // 方法不存在，跳过
            }

            return info;
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 获取领地信息时发生异常: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 检查 Residence API 是否可用。
     *
     * <p>用于在调用其他方法前检查 API 状态。</p>
     *
     * @return 如果 Residence API 已加载且可用返回 true，否则返回 false
     */
    public boolean isAvailable() {
        return PluginAPIManager.getInstance().getResidenceAPI().isPresent();
    }

    /**
     * 创建领地。
     *
     * <p>通过两个对角位置在 Residence 插件中创建新的领地。</p>
     *
     * <p><b>TODO</b>: 确认 Residence API 中创建领地的方法名</p>
     *
     * @param name 领地名称，不能为 null
     * @param owner 领地所有者，不能为 null
     * @param loc1 位置1（对角点），不能为 null
     * @param loc2 位置2（对角点），不能为 null
     * @return 如果创建成功返回 true，否则返回 false
     */
    public boolean createResidence(final String name, final Player owner, final Location loc1, final Location loc2) {
        final Optional<Object> resApi = PluginAPIManager.getInstance().getResidenceAPI();
        if (resApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] Residence API 不可用，无法创建领地");
            return false;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"), () -> {
            try {
                final Object api = resApi.get();
                // TODO: 确认创建领地的方法 - 假设为 getResidenceManager().addResidence(...)
                final Method getResidenceManagerMethod = api.getClass().getMethod("getResidenceManager");
                final Object residenceManager = getResidenceManagerMethod.invoke(api);

                // 尝试调用 addResidence 或 createResidence 方法
                try {
                    final Method addResidenceMethod = residenceManager.getClass().getMethod("addResidence", String.class, Player.class, Location.class, Location.class);
                    addResidenceMethod.invoke(residenceManager, name, owner, loc1, loc2);
                    result[0] = true;
                    this.logger.info("[BetterHTTPAPI] 领地创建成功: " + name);
                } catch (NoSuchMethodException e) {
                    // 尝试其他方法名
                    this.logger.warning("[BetterHTTPAPI] addResidence 方法不存在，尝试其他方法名: " + e.getMessage());
                }
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 创建领地时发生异常: " + e.getMessage());
                result[0] = false;
            } finally {
                latch.countDown();
            }
        });

        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
        return result[0];
    }

    /**
     * 删除领地。
     *
     * <p>通过领地名称在 Residence 插件中删除指定的领地。</p>
     *
     * <p><b>TODO</b>: 确认 Residence API 中删除领地的方法名</p>
     *
     * @param name 领地名称，不能为 null
     * @return 如果删除成功返回 true，否则返回 false
     */
    public boolean deleteResidence(final String name) {
        final Optional<Object> resApi = PluginAPIManager.getInstance().getResidenceAPI();
        if (resApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] Residence API 不可用，无法删除领地");
            return false;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"), () -> {
            try {
                final Object api = resApi.get();
                final Method getResidenceManagerMethod = api.getClass().getMethod("getResidenceManager");
                final Object residenceManager = getResidenceManagerMethod.invoke(api);

                // TODO: 确认删除领地的方法 - 假设为 removeResidence(String name)
                final Method removeResidenceMethod = residenceManager.getClass().getMethod("removeResidence", String.class);
                removeResidenceMethod.invoke(residenceManager, name);
                result[0] = true;
                this.logger.info("[BetterHTTPAPI] 领地删除成功: " + name);
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 删除领地时发生异常: " + e.getMessage());
                result[0] = false;
            } finally {
                latch.countDown();
            }
        });

        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
        return result[0];
    }

    /**
     * 添加领地成员。
     *
     * <p>向指定的领地中添加玩家作为成员。</p>
     *
     * <p><b>TODO</b>: 确认 Residence API 中添加成员的方法名</p>
     *
     * @param residenceName 领地名称，不能为 null
     * @param playerName 要添加的玩家名称，不能为 null
     * @return 如果添加成功返回 true，否则返回 false
     */
    public boolean addResidenceMember(final String residenceName, final String playerName) {
        final Optional<Object> resApi = PluginAPIManager.getInstance().getResidenceAPI();
        if (resApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] Residence API 不可用，无法添加领地成员");
            return false;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"), () -> {
            try {
                final Object api = resApi.get();
                final Method getResidenceManagerMethod = api.getClass().getMethod("getResidenceManager");
                final Object residenceManager = getResidenceManagerMethod.invoke(api);

                // TODO: 确认实际方法名 - 假设为 getByName(String)
                final Method getByNameMethod = residenceManager.getClass().getMethod("getByName", String.class);
                final Object residence = getByNameMethod.invoke(residenceManager, residenceName);

                if (residence == null) {
                    this.logger.warning("[BetterHTTPAPI] 领地不存在: " + residenceName);
                    result[0] = false;
                    latch.countDown();
                    return;
                }

                // TODO: 确认添加成员的方法 - 假设为 addMember(String playerName)
                try {
                    final Method addMemberMethod = residence.getClass().getMethod("addMember", String.class);
                    addMemberMethod.invoke(residence, playerName);
                    result[0] = true;
                    this.logger.info("[BetterHTTPAPI] 成员添加成功: " + playerName + " -> " + residenceName);
                } catch (NoSuchMethodException e) {
                    this.logger.warning("[BetterHTTPAPI] addMember 方法不存在: " + e.getMessage());
                }
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 添加领地成员时发生异常: " + e.getMessage());
                result[0] = false;
            } finally {
                latch.countDown();
            }
        });

        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
        return result[0];
    }

    /**
     * 移除领地成员。
     *
     * <p>从指定的领地中移除指定玩家成员。</p>
     *
     * <p><b>TODO</b>: 确认 Residence API 中移除成员的方法名</p>
     *
     * @param residenceName 领地名称，不能为 null
     * @param playerName 要移除的玩家名称，不能为 null
     * @return 如果移除成功返回 true，否则返回 false
     */
    public boolean removeResidenceMember(final String residenceName, final String playerName) {
        final Optional<Object> resApi = PluginAPIManager.getInstance().getResidenceAPI();
        if (resApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] Residence API 不可用，无法移除领地成员");
            return false;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"), () -> {
            try {
                final Object api = resApi.get();
                final Method getResidenceManagerMethod = api.getClass().getMethod("getResidenceManager");
                final Object residenceManager = getResidenceManagerMethod.invoke(api);

                // TODO: 确认实际方法名 - 假设为 getByName(String)
                final Method getByNameMethod = residenceManager.getClass().getMethod("getByName", String.class);
                final Object residence = getByNameMethod.invoke(residenceManager, residenceName);

                if (residence == null) {
                    this.logger.warning("[BetterHTTPAPI] 领地不存在: " + residenceName);
                    result[0] = false;
                    latch.countDown();
                    return;
                }

                // TODO: 确认移除成员的方法 - 假设为 removeMember(String playerName)
                try {
                    final Method removeMemberMethod = residence.getClass().getMethod("removeMember", String.class);
                    removeMemberMethod.invoke(residence, playerName);
                    result[0] = true;
                    this.logger.info("[BetterHTTPAPI] 成员移除成功: " + playerName + " -> " + residenceName);
                } catch (NoSuchMethodException e) {
                    this.logger.warning("[BetterHTTPAPI] removeMember 方法不存在: " + e.getMessage());
                }
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 移除领地成员时发生异常: " + e.getMessage());
                result[0] = false;
            } finally {
                latch.countDown();
            }
        });

        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
        return result[0];
    }

    /**
     * 设置领地标志。
     *
     * <p>设置指定领地的权限标志（如 build、destroy、use、move 等）。</p>
     *
     * <p><b>TODO</b>: 确认 Residence API 中设置领地标志的方法名</p>
     *
     * @param residenceName 领地名称，不能为 null
     * @param flag 权限标志名称，不能为 null
     * @param value 标志值
     * @return 如果设置成功返回 true，否则返回 false
     */
    public boolean setResidenceFlag(final String residenceName, final String flag, final boolean value) {
        final Optional<Object> resApi = PluginAPIManager.getInstance().getResidenceAPI();
        if (resApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] Residence API 不可用，无法设置领地标志");
            return false;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"), () -> {
            try {
                final Object api = resApi.get();
                final Method getResidenceManagerMethod = api.getClass().getMethod("getResidenceManager");
                final Object residenceManager = getResidenceManagerMethod.invoke(api);

                // TODO: 确认实际方法名 - 假设为 getByName(String)
                final Method getByNameMethod = residenceManager.getClass().getMethod("getByName", String.class);
                final Object residence = getByNameMethod.invoke(residenceManager, residenceName);

                if (residence == null) {
                    this.logger.warning("[BetterHTTPAPI] 领地不存在: " + residenceName);
                    result[0] = false;
                    latch.countDown();
                    return;
                }

                // TODO: 确认设置标志的方法 - 假设为 setFlag(String flag, boolean value)
                try {
                    final Method setFlagMethod = residence.getClass().getMethod("setFlag", String.class, boolean.class);
                    setFlagMethod.invoke(residence, flag, value);
                    result[0] = true;
                    this.logger.info("[BetterHTTPAPI] 领地标志设置成功: " + residenceName + " [" + flag + "=" + value + "]");
                } catch (NoSuchMethodException e) {
                    this.logger.warning("[BetterHTTPAPI] setFlag 方法不存在: " + e.getMessage());
                }
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 设置领地标志时发生异常: " + e.getMessage());
                result[0] = false;
            } finally {
                latch.countDown();
            }
        });

        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
        return result[0];
    }

    /**
     * 转移领地所有权。
     *
     * <p>将指定领地的所有权转移给新的玩家。</p>
     *
     * <p><b>TODO</b>: 确认 Residence API 中转移所有权的方法名</p>
     *
     * @param residenceName 领地名称，不能为 null
     * @param newOwner 新所有者的玩家名称，不能为 null
     * @return 如果转移成功返回 true，否则返回 false
     */
    public boolean transferResidence(final String residenceName, final String newOwner) {
        final Optional<Object> resApi = PluginAPIManager.getInstance().getResidenceAPI();
        if (resApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] Residence API 不可用，无法转移领地所有权");
            return false;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"), () -> {
            try {
                final Object api = resApi.get();
                final Method getResidenceManagerMethod = api.getClass().getMethod("getResidenceManager");
                final Object residenceManager = getResidenceManagerMethod.invoke(api);

                // TODO: 确认实际方法名 - 假设为 getByName(String)
                final Method getByNameMethod = residenceManager.getClass().getMethod("getByName", String.class);
                final Object residence = getByNameMethod.invoke(residenceManager, residenceName);

                if (residence == null) {
                    this.logger.warning("[BetterHTTPAPI] 领地不存在: " + residenceName);
                    result[0] = false;
                    latch.countDown();
                    return;
                }

                // TODO: 确认转移所有权的方法 - 假设为 transferOwner(String newOwner)
                try {
                    final Method transferOwnerMethod = residence.getClass().getMethod("transferOwner", String.class);
                    transferOwnerMethod.invoke(residence, newOwner);
                    result[0] = true;
                    this.logger.info("[BetterHTTPAPI] 领地所有权转移成功: " + residenceName + " -> " + newOwner);
                } catch (NoSuchMethodException e) {
                    this.logger.warning("[BetterHTTPAPI] transferOwner 方法不存在: " + e.getMessage());
                }
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 转移领地所有权时发生异常: " + e.getMessage());
                result[0] = false;
            } finally {
                latch.countDown();
            }
        });

        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
        return result[0];
    }

    /**
     * 获取领地的所有者和成员列表。
     *
     * <p>返回一个包含 owner 和 members 两个键的 Map，分别对应所有者和成员列表。</p>
     *
     * <p><b>TODO</b>: 确认 Residence API 中获取成员和所有者的方法名</p>
     *
     * @param residenceName 领地名称，不能为 null
     * @return 包含 "owner"（字符串）和 "members"（字符串列表）的 Map，
     *         如果领地不存在或发生异常，返回空 Map
     */
    public Map<String, List<String>> getResidenceMembers(final String residenceName) {
        final Optional<Object> resApi = PluginAPIManager.getInstance().getResidenceAPI();
        if (resApi.isEmpty()) {
            this.logger.warning("[BetterHTTPAPI] Residence API 不可用，无法获取领地成员");
            return Collections.emptyMap();
        }

        final CountDownLatch latch = new CountDownLatch(1);
        @SuppressWarnings("unchecked")
        final Map<String, List<String>>[] result = new Map[]{null};

        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("BetterHTTPAPI"), () -> {
            try {
                final Object api = resApi.get();
                final Method getResidenceManagerMethod = api.getClass().getMethod("getResidenceManager");
                final Object residenceManager = getResidenceManagerMethod.invoke(api);

                // TODO: 确认实际方法名 - 假设为 getByName(String)
                final Method getByNameMethod = residenceManager.getClass().getMethod("getByName", String.class);
                final Object residence = getByNameMethod.invoke(residenceManager, residenceName);

                if (residence == null) {
                    this.logger.warning("[BetterHTTPAPI] 领地不存在: " + residenceName);
                    latch.countDown();
                    return;
                }

                final Map<String, List<String>> membersMap = new HashMap<>();

                // 获取所有者
                // TODO: 确认实际方法名 - 假设为 getOwner()
                try {
                    final Method getOwnerMethod = residence.getClass().getMethod("getOwner");
                    final Object owner = getOwnerMethod.invoke(residence);
                    final List<String> ownerList = new ArrayList<>();
                    ownerList.add(owner != null ? owner.toString() : "Unknown");
                    membersMap.put("owner", ownerList);
                } catch (NoSuchMethodException e) {
                    this.logger.warning("[BetterHTTPAPI] getOwner 方法不存在: " + e.getMessage());
                    membersMap.put("owner", Collections.emptyList());
                }

                // 获取成员列表
                // TODO: 确认实际方法名 - 假设为 getMembers() 返回 Map 或 List
                try {
                    final Method getMembersMethod = residence.getClass().getMethod("getMembers");
                    final Object members = getMembersMethod.invoke(residence);
                    final List<String> memberList = new ArrayList<>();
                    if (members instanceof Map) {
                        @SuppressWarnings("unchecked")
                        final Map<?, ?> membersMapRaw = (Map<?, ?>) members;
                        for (final Object key : membersMapRaw.keySet()) {
                            memberList.add(key.toString());
                        }
                    } else if (members instanceof List) {
                        @SuppressWarnings("unchecked")
                        final List<?> membersList = (List<?>) members;
                        for (final Object member : membersList) {
                            memberList.add(member.toString());
                        }
                    }
                    membersMap.put("members", memberList);
                } catch (NoSuchMethodException e) {
                    this.logger.warning("[BetterHTTPAPI] getMembers 方法不存在: " + e.getMessage());
                    membersMap.put("members", Collections.emptyList());
                }

                result[0] = membersMap;
            } catch (Exception e) {
                this.logger.warning("[BetterHTTPAPI] 获取领地成员时发生异常: " + e.getMessage());
                result[0] = Collections.emptyMap();
            } finally {
                latch.countDown();
            }
        });

        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return Collections.emptyMap(); }
        return result[0] != null ? result[0] : Collections.emptyMap();
    }
}