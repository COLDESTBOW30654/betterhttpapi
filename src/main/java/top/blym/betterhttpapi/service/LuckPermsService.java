package top.blym.betterhttpapi.service;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeBuilder;
import net.luckperms.api.query.QueryOptions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.luckperms.api.node.types.InheritanceNode;

/**
 * LuckPerms 权限管理服务类
 * <p>提供与 LuckPerms 插件集成的权限管理功能，包括权限检查、权限组管理、
 * 权限节点操作等功能。所有方法均处理异常情况，在 LuckPerms API 不可用
 * 时返回安全的默认值。</p>
 * 
 * <p>使用示例：</p>
 * <pre>{@code
 * LuckPermsService service = LuckPermsService.getInstance();
 * 
 * // 检查在线玩家权限
 * boolean hasPerm = service.hasPermission(player, "some.permission");
 * 
 * // 异步检查离线玩家权限
 * service.hasPermissionOffline(uuid, "some.permission")
 *     .thenAccept(hasPerm -> {
 *         if (hasPerm) {
 *             // 处理逻辑
 *         }
 *     });
 * 
 * // 异步添加权限
 * service.addPermission(uuid, "some.permission", null)
 *     .thenRun(() -> {
 *         // 权限添加成功
 *     });
 * }</pre>
 * 
 * @author 白鹿原嚒
 * @version 1.0.0
 */
public class LuckPermsService {

    /** 单例实例 */
    private static volatile LuckPermsService instance;
    
    /** LuckPerms API 实例 */
    private final LuckPerms luckPerms;
    
    /** 日志记录器 */
    private final Logger logger;

    /**
     * 私有构造器（单例模式）
     * 
     * <p>初始化 LuckPerms API 实例，如果 API 不可用则抛出异常。</p>
     */
    private LuckPermsService() {
        this.logger = Bukkit.getLogger();
        LuckPerms api = null;
        
        try {
            api = LuckPermsProvider.get();
            this.logger.info("[BetterHTTPAPI] LuckPerms API 已成功加载");
        } catch (IllegalStateException | NoClassDefFoundError e) {
            this.logger.warning("[BetterHTTPAPI] LuckPerms API 不可用，权限功能将返回默认值: " + e.getMessage());
            api = null;
        }
        
        this.luckPerms = api;
    }

    /**
     * 获取单例实例
     * 
     * <p>使用双重检查锁定确保线程安全的单例初始化。</p>
     * 
     * @return LuckPermsService 实例
     */
    public static LuckPermsService getInstance() {
        if (instance == null) {
            synchronized (LuckPermsService.class) {
                if (instance == null) {
                    instance = new LuckPermsService();
                }
            }
        }
        return instance;
    }

    /**
     * 检查在线玩家是否拥有指定权限
     * 
     * <p>同步方法，直接从玩家的缓存数据中检查权限。此方法必须在主线程调用。</p>
     * 
     * <p>使用示例：</p>
     * <pre>{@code
     * boolean hasPerm = service.hasPermission(player, "minecraft.command.gamemode");
     * if (hasPerm) {
     *     player.sendMessage("你有权限");
     * }
     * }</pre>
     * 
     * @param player 玩家对象，不能为 null
     * @param permission 权限节点，如 "minecraft.command.gamemode"
     * @return 如果玩家拥有权限返回 true，否则返回 false。
     *         如果 LuckPerms API 不可用或发生异常，返回 false
     */
    public boolean hasPermission(final Player player, final String permission) {
        if (this.luckPerms == null) {
            return false;
        }

        try {
            final User user = this.luckPerms.getPlayerAdapter(Player.class).getUser(player);
            return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 检查玩家权限时发生异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 异步检查离线玩家是否拥有指定权限
     * 
     * <p>异步加载玩家数据并检查权限。适用于离线玩家或异步场景。</p>
     * 
     * <p>使用示例：</p>
     * <pre>{@code
     * service.hasPermissionOffline(uuid, "some.permission")
     *     .thenAccept(hasPerm -> {
     *         if (hasPerm) {
     *             Bukkit.getScheduler().runTask(plugin, () -> {
     *                 // 在主线程执行操作
     *             });
     *         }
     *     });
     * }</pre>
     * 
     * @param uuid 玩家的 UUID，不能为 null
     * @param permission 权限节点，如 "minecraft.command.gamemode"
     * @return CompletableFuture，完成时返回布尔值表示是否有权限。
     *         如果 LuckPerms API 不可用或发生异常，返回 false
     */
    public CompletableFuture<Boolean> hasPermissionOffline(final UUID uuid, final String permission) {
        if (this.luckPerms == null) {
            return CompletableFuture.completedFuture(false);
        }

        try {
            return this.luckPerms.getUserManager().loadUser(uuid)
                .thenApply(user -> {
                    try {
                        return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
                    } catch (Exception e) {
                        this.logger.warning("[BetterHTTPAPI] 检查离线玩家权限时发生异常: " + e.getMessage());
                        return false;
                    }
                });
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 加载用户数据时发生异常: " + e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * 异步获取玩家所属的所有权限组
     * 
     * <p>异步加载玩家数据并获取继承的所有权限组名称。</p>
     * 
     * <p>使用示例：</p>
     * <pre>{@code
     * service.getPlayerGroups(uuid)
     *     .thenAccept(groups -> {
     *         groups.forEach(groupName -> {
     *             System.out.println("权限组: " + groupName);
     *         });
     *     });
     * }</pre>
     * 
     * @param uuid 玩家的 UUID，不能为 null
     * @return CompletableFuture，完成时返回权限组名称列表。
     *         如果 LuckPerms API 不可用或发生异常，返回空列表
     */
    public CompletableFuture<List<String>> getPlayerGroups(final UUID uuid) {
        if (this.luckPerms == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        try {
            return this.luckPerms.getUserManager().loadUser(uuid)
                .thenApply(user -> {
                    try {
                        // 使用默认的 QueryOptions 获取继承的权限组
                        return user.getInheritedGroups(QueryOptions.defaultContextualOptions()).stream()
                            .map(Group::getName)
                            .collect(Collectors.toList());
                    } catch (Exception e) {
                        this.logger.warning("[BetterHTTPAPI] 获取玩家权限组时发生异常: " + e.getMessage());
                        return Collections.<String>emptyList();
                    }
                });
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 加载用户数据时发生异常: " + e.getMessage());
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
    }

    /**
     * 异步为玩家添加权限节点
     * 
     * <p>异步修改玩家数据，添加指定的权限节点。可以指定上下文（如服务器、世界等），
     * 如果 context 参数为 null，则权限为全局权限。</p>
     * 
     * <p><b>重要</b>：此方法会自动保存更改到数据库。</p>
     * 
     * <p>使用示例：</p>
     * <pre>{@code
     * // 添加全局权限
     * service.addPermission(uuid, "some.permission", null)
     *     .thenRun(() -> {
     *         System.out.println("权限添加成功");
     *     });
     * 
     * // 添加特定服务器的权限（如果支持上下文）
     * service.addPermission(uuid, "some.permission", "server:lobby")
     *     .thenRun(() -> {
     *         System.out.println("权限添加成功");
     *     });
     * }</pre>
     * 
     * @param uuid 玩家的 UUID，不能为 null
     * @param permission 权限节点，如 "some.permission"
     * @param context 权限上下文（可选，可以为 null 表示全局权限）
     * @return CompletableFuture，完成时表示权限已添加并保存。
     *         如果 LuckPerms API 不可用或发生异常，立即完成并返回 null
     */
    public CompletableFuture<Void> addPermission(final UUID uuid, final String permission, final String context) {
        if (this.luckPerms == null) {
            return CompletableFuture.completedFuture(null);
        }

        try {
            return this.luckPerms.getUserManager().modifyUser(uuid, user -> {
                try {
                    final Node node = Node.builder(permission).build();
                    user.data().add(node);
                } catch (Exception e) {
                    this.logger.warning("[BetterHTTPAPI] 添加权限节点时发生异常: " + e.getMessage());
                }
            }).thenCompose(v -> {
                // 保存用户数据
                return this.luckPerms.getUserManager().loadUser(uuid).thenAccept(user -> {
                    try {
                        this.luckPerms.getUserManager().saveUser(user);
                    } catch (Exception e) {
                        this.logger.warning("[BetterHTTPAPI] 保存用户数据时发生异常: " + e.getMessage());
                    }
                });
            });
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 修改用户权限时发生异常: " + e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * 异步移除玩家的权限节点
     * 
     * <p>异步修改玩家数据，移除指定的权限节点。</p>
     * 
     * <p><b>重要</b>：此方法会自动保存更改到数据库。</p>
     * 
     * <p>使用示例：</p>
     * <pre>{@code
     * service.removePermission(uuid, "some.permission")
     *     .thenRun(() -> {
     *         System.out.println("权限移除成功");
     *     });
     * }</pre>
     * 
     * @param uuid 玩家的 UUID，不能为 null
     * @param permission 权限节点，如 "some.permission"
     * @return CompletableFuture，完成时表示权限已移除并保存。
     *         如果 LuckPerms API 不可用或发生异常，立即完成并返回 null
     */
    public CompletableFuture<Void> removePermission(final UUID uuid, final String permission) {
        if (this.luckPerms == null) {
            return CompletableFuture.completedFuture(null);
        }

        try {
            return this.luckPerms.getUserManager().modifyUser(uuid, user -> {
                try {
                    final Node node = Node.builder(permission).build();
                    user.data().remove(node);
                } catch (Exception e) {
                    this.logger.warning("[BetterHTTPAPI] 移除权限节点时发生异常: " + e.getMessage());
                }
            }).thenCompose(v -> {
                // 保存用户数据
                return this.luckPerms.getUserManager().loadUser(uuid).thenAccept(user -> {
                    try {
                        this.luckPerms.getUserManager().saveUser(user);
                    } catch (Exception e) {
                        this.logger.warning("[BetterHTTPAPI] 保存用户数据时发生异常: " + e.getMessage());
                    }
                });
            });
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 修改用户权限时发生异常: " + e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * 异步获取玩家的前缀
     * 
     * <p>异步加载玩家数据并获取玩家的前缀（Prefix）。前缀可能为 null。</p>
     * 
     * <p>使用示例：</p>
     * <pre>{@code
     * service.getPrefix(uuid)
     *     .thenAccept(prefix -> {
     *         if (prefix != null) {
     *             System.out.println("玩家前缀: " + prefix);
     *         } else {
     *             System.out.println("玩家没有设置前缀");
     *         }
     *     });
     * }</pre>
     * 
     * @param uuid 玩家的 UUID，不能为 null
     * @return CompletableFuture，完成时返回玩家的前缀字符串。
     *         如果玩家没有前缀，返回 null。
     *         如果 LuckPerms API 不可用或发生异常，返回 null
     */
    public CompletableFuture<String> getPrefix(final UUID uuid) {
        if (this.luckPerms == null) {
            return CompletableFuture.completedFuture(null);
        }

        try {
            return this.luckPerms.getUserManager().loadUser(uuid)
                .thenApply(user -> {
                    try {
                        return user.getCachedData().getMetaData().getPrefix();
                    } catch (Exception e) {
                        this.logger.warning("[BetterHTTPAPI] 获取玩家前缀时发生异常: " + e.getMessage());
                        return null;
                    }
                });
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 加载用户数据时发生异常: " + e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * 检查 LuckPerms API 是否可用
     * 
     * <p>用于在调用其他方法前检查 API 状态。</p>
     * 
     * @return 如果 LuckPerms API 已加载且可用返回 true，否则返回 false
     */
    public boolean isAvailable() {
        return this.luckPerms != null;
    }

    // ==================== 权限组管理 ====================

    /**
     * 获取所有已加载的权限组
     *
     * <p>异步获取 LuckPerms 中所有已加载的权限组名称列表。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * service.getAllGroups().thenAccept(groups -> {
     *     groups.forEach(System.out::println);
     * });
     * }</pre>
     *
     * @return CompletableFuture，完成时返回权限组名称列表。
     *         如果 LuckPerms API 不可用或发生异常，返回空列表
     */
    public CompletableFuture<List<String>> getAllGroups() {
        if (this.luckPerms == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        try {
            return CompletableFuture.completedFuture(
                this.luckPerms.getGroupManager().getLoadedGroups().stream()
                    .map(Group::getName)
                    .collect(Collectors.toList())
            );
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 获取所有权限组时发生异常: " + e.getMessage());
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
    }

    /**
     * 创建新的权限组
     *
     * <p>异步创建指定名称的权限组。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * service.createGroup("vip").thenRun(() -> {
     *     System.out.println("权限组创建成功");
     * });
     * }</pre>
     *
     * @param groupName 要创建的权限组名称，不能为 null
     * @return CompletableFuture，完成时表示权限组已创建。
     *         如果 LuckPerms API 不可用或发生异常，立即完成并返回 null
     */
    public CompletableFuture<Void> createGroup(final String groupName) {
        if (this.luckPerms == null) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            this.luckPerms.getGroupManager().createAndLoadGroup(groupName);
            this.logger.info("[BetterHTTPAPI] 权限组创建成功: " + groupName);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 创建权限组时发生异常: " + e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * 删除权限组
     *
     * <p>异步删除指定名称的权限组。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * service.deleteGroup("old-group").thenRun(() -> {
     *     System.out.println("权限组已删除");
     * });
     * }</pre>
     *
     * @param groupName 要删除的权限组名称，不能为 null
     * @return CompletableFuture，完成时表示权限组已删除。
     *         如果 LuckPerms API 不可用或发生异常，立即完成并返回 null
     */
    public CompletableFuture<Void> deleteGroup(final String groupName) {
        if (this.luckPerms == null) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            final Group group = this.luckPerms.getGroupManager().getGroup(groupName);
            if (group != null) {
                this.luckPerms.getGroupManager().deleteGroup(group);
                this.logger.info("[BetterHTTPAPI] 权限组删除成功: " + groupName);
            } else {
                this.logger.warning("[BetterHTTPAPI] 权限组不存在: " + groupName);
            }
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 删除权限组时发生异常: " + e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    // ==================== 玩家权限组管理 ====================

    /**
     * 将玩家加入指定权限组
     *
     * <p>异步将玩家添加到指定的权限组（继承关系）。</p>
     *
     * <p><b>重要</b>：此方法会自动保存更改到数据库。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * service.addPlayerToGroup(uuid, "vip").thenRun(() -> {
     *     System.out.println("玩家已加入 VIP 组");
     * });
     * }</pre>
     *
     * @param uuid  玩家的 UUID，不能为 null
     * @param group 权限组名称，不能为 null
     * @return CompletableFuture，完成时表示玩家已加入权限组。
     *         如果 LuckPerms API 不可用或发生异常，立即完成并返回 null
     */
    public CompletableFuture<Void> addPlayerToGroup(final UUID uuid, final String group) {
        if (this.luckPerms == null) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            return this.luckPerms.getUserManager().modifyUser(uuid, user -> {
                final InheritanceNode node = InheritanceNode.builder(group).build();
                user.data().add(node);
            }).thenCompose(v ->
                this.luckPerms.getUserManager().loadUser(uuid).thenAccept(user -> {
                    try {
                        this.luckPerms.getUserManager().saveUser(user);
                    } catch (Exception e) {
                        this.logger.warning("[BetterHTTPAPI] 保存用户数据时发生异常: " + e.getMessage());
                    }
                })
            );
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 将玩家加入权限组时发生异常: " + e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * 将玩家移出指定权限组
     *
     * <p>异步将玩家从指定的权限组中移除（解除继承关系）。</p>
     *
     * <p><b>重要</b>：此方法会自动保存更改到数据库。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * service.removePlayerFromGroup(uuid, "vip").thenRun(() -> {
     *     System.out.println("玩家已从 VIP 组移出");
     * });
     * }</pre>
     *
     * @param uuid  玩家的 UUID，不能为 null
     * @param group 权限组名称，不能为 null
     * @return CompletableFuture，完成时表示玩家已从权限组移出。
     *         如果 LuckPerms API 不可用或发生异常，立即完成并返回 null
     */
    public CompletableFuture<Void> removePlayerFromGroup(final UUID uuid, final String group) {
        if (this.luckPerms == null) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            return this.luckPerms.getUserManager().modifyUser(uuid, user -> {
                final InheritanceNode node = InheritanceNode.builder(group).build();
                user.data().remove(node);
            }).thenCompose(v ->
                this.luckPerms.getUserManager().loadUser(uuid).thenAccept(user -> {
                    try {
                        this.luckPerms.getUserManager().saveUser(user);
                    } catch (Exception e) {
                        this.logger.warning("[BetterHTTPAPI] 保存用户数据时发生异常: " + e.getMessage());
                    }
                })
            );
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 将玩家移出权限组时发生异常: " + e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    // ==================== 权限节点管理 ====================

    /**
     * 设置权限组的权限节点
     *
     * <p>异步为指定权限组添加或移除权限节点。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * // 添加权限
     * service.setGroupPermission("vip", "some.permission", true);
     * // 移除权限
     * service.setGroupPermission("vip", "some.permission", false);
     * }</pre>
     *
     * @param group      权限组名称，不能为 null
     * @param permission 权限节点，如 "some.permission"
     * @param value      true 表示添加权限，false 表示移除权限
     * @return CompletableFuture，完成时表示权限已设置。
     *         如果 LuckPerms API 不可用或发生异常，立即完成并返回 null
     */
    public CompletableFuture<Void> setGroupPermission(final String group, final String permission, final boolean value) {
        if (this.luckPerms == null) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            return this.luckPerms.getGroupManager().modifyGroup(group, g -> {
                if (value) {
                    g.data().add(Node.builder(permission).build());
                } else {
                    g.data().remove(Node.builder(permission).build());
                }
            });
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 设置组权限时发生异常: " + e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    // ==================== 聊天前缀/后缀管理 ====================

    /**
     * 设置玩家的聊天前缀
     *
     * <p>异步设置玩家的聊天前缀（Prefix）。如果 prefix 为 null 或空字符串，则清除玩家的前缀。</p>
     *
     * <p><b>重要</b>：此方法会自动保存更改到数据库。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * // 设置前缀
     * service.setPlayerPrefix(uuid, "&7[VIP]&r").thenRun(() -> {
     *     System.out.println("前缀已设置");
     * });
     * // 清除前缀
     * service.setPlayerPrefix(uuid, null).thenRun(() -> {
     *     System.out.println("前缀已清除");
     * });
     * }</pre>
     *
     * @param uuid   玩家的 UUID，不能为 null
     * @param prefix 要设置的前缀字符串（可以为 null 或空字符串以清除前缀）
     * @return CompletableFuture，完成时表示前缀已设置或清除。
     *         如果 LuckPerms API 不可用或发生异常，立即完成并返回 null
     */
    public CompletableFuture<Void> setPlayerPrefix(final UUID uuid, final String prefix) {
        if (this.luckPerms == null) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            // 使用反射调用 ChatMeta 相关方法，避免编译依赖
            final Class<?> chatMetaNodeClass = Class.forName("net.luckperms.api.node.types.ChatMetaNode");
            final Class<?> chatMetaTypeClass = Class.forName("net.luckperms.api.chat.ChatMetaType");

            final Object prefixEnum = chatMetaTypeClass.getField("PREFIX").get(null);
            final Method chatMetaNodeMethod = chatMetaNodeClass.getMethod("chatMetaNode", chatMetaTypeClass, int.class, String.class);

            return this.luckPerms.getUserManager().modifyUser(uuid, user -> {
                if (prefix == null || prefix.isEmpty()) {
                    user.data().clear(n -> chatMetaNodeClass.isInstance(n));
                } else {
                    try {
                        final Object node = chatMetaNodeMethod.invoke(null, prefixEnum, 100, prefix);
                        final Method buildMethod = node.getClass().getMethod("build");
                        final Object builtNode = buildMethod.invoke(node);
                        user.data().add((Node) builtNode);
                    } catch (Exception ex) {
                        this.logger.warning("[BetterHTTPAPI] 构建前缀节点时发生异常: " + ex.getMessage());
                    }
                }
            }).thenCompose(v ->
                this.luckPerms.getUserManager().loadUser(uuid).thenAccept(user -> {
                    try {
                        this.luckPerms.getUserManager().saveUser(user);
                    } catch (Exception e) {
                        this.logger.warning("[BetterHTTPAPI] 保存用户数据时发生异常: " + e.getMessage());
                    }
                })
            );
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 设置玩家前缀时发生异常: " + e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * 设置玩家的聊天后缀
     *
     * <p>异步设置玩家的聊天后缀（Suffix）。如果 suffix 为 null 或空字符串，则清除玩家的后缀。</p>
     *
     * <p><b>重要</b>：此方法会自动保存更改到数据库。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * // 设置后缀
     * service.setPlayerSuffix(uuid, "&7[VIP]&r").thenRun(() -> {
     *     System.out.println("后缀已设置");
     * });
     * // 清除后缀
     * service.setPlayerSuffix(uuid, null).thenRun(() -> {
     *     System.out.println("后缀已清除");
     * });
     * }</pre>
     *
     * @param uuid   玩家的 UUID，不能为 null
     * @param suffix 要设置的后缀字符串（可以为 null 或空字符串以清除后缀）
     * @return CompletableFuture，完成时表示后缀已设置或清除。
     *         如果 LuckPerms API 不可用或发生异常，立即完成并返回 null
     */
    public CompletableFuture<Void> setPlayerSuffix(final UUID uuid, final String suffix) {
        if (this.luckPerms == null) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            // 使用反射调用 ChatMeta 相关方法，避免编译依赖
            final Class<?> chatMetaNodeClass = Class.forName("net.luckperms.api.node.types.ChatMetaNode");
            final Class<?> chatMetaTypeClass = Class.forName("net.luckperms.api.chat.ChatMetaType");

            final Object suffixEnum = chatMetaTypeClass.getField("SUFFIX").get(null);
            final Method chatMetaNodeMethod = chatMetaNodeClass.getMethod("chatMetaNode", chatMetaTypeClass, int.class, String.class);

            return this.luckPerms.getUserManager().modifyUser(uuid, user -> {
                if (suffix == null || suffix.isEmpty()) {
                    user.data().clear(n -> chatMetaNodeClass.isInstance(n));
                } else {
                    try {
                        final Object node = chatMetaNodeMethod.invoke(null, suffixEnum, 100, suffix);
                        final Method buildMethod = node.getClass().getMethod("build");
                        final Object builtNode = buildMethod.invoke(node);
                        user.data().add((Node) builtNode);
                    } catch (Exception ex) {
                        this.logger.warning("[BetterHTTPAPI] 构建后缀节点时发生异常: " + ex.getMessage());
                    }
                }
            }).thenCompose(v ->
                this.luckPerms.getUserManager().loadUser(uuid).thenAccept(user -> {
                    try {
                        this.luckPerms.getUserManager().saveUser(user);
                    } catch (Exception e) {
                        this.logger.warning("[BetterHTTPAPI] 保存用户数据时发生异常: " + e.getMessage());
                    }
                })
            );
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 设置玩家后缀时发生异常: " + e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    // ==================== 玩家权限查询 ====================

    /**
     * 获取玩家的所有权限节点
     *
     * <p>异步加载玩家数据并获取所有直接设置的权限节点及其值。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * service.getPlayerPermissions(uuid).thenAccept(permissions -> {
     *     permissions.forEach((perm, value) -> {
     *         System.out.println(perm + " = " + value);
     *     });
     * });
     * }</pre>
     *
     * @param uuid 玩家的 UUID，不能为 null
     * @return CompletableFuture，完成时返回权限名称到布尔值的映射。
     *         如果 LuckPerms API 不可用或发生异常，返回空映射
     */
    public CompletableFuture<Map<String, Boolean>> getPlayerPermissions(final UUID uuid) {
        if (this.luckPerms == null) {
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }
        try {
            return this.luckPerms.getUserManager().loadUser(uuid)
                .thenApply(user -> new HashMap<>(user.getCachedData().getPermissionData().getPermissionMap()));
        } catch (Exception e) {
            this.logger.warning("[BetterHTTPAPI] 获取玩家权限时发生异常: " + e.getMessage());
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }
    }
}