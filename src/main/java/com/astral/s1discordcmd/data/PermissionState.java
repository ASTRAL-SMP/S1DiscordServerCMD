package com.astral.s1discordcmd.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.*;

/**
 * ロールごとの許可コマンドとブラックリストを永続化する。
 * デフォルト拒否方式: 明示的にpermitされたコマンドのみ実行可能。
 * ブラックリストはpermitより優先される。
 */
public class PermissionState extends PersistentState {

    private static final String KEY = "s1discordcmd_permissions";

    // デフォルトで永久ブロックするコマンド
    private static final Set<String> DEFAULT_BLACKLIST = Set.of(
            "stop", "op", "deop", "ban-ip", "pardon-ip",
            "save-all", "save-off", "save-on", "debug"
    );

    // roleId -> 許可コマンドセット
    private final Map<String, Set<String>> rolePermissions = new HashMap<>();
    // ブラックリスト
    private final Set<String> blacklist = new HashSet<>();

    public PermissionState() {
        blacklist.addAll(DEFAULT_BLACKLIST);
    }

    public static PermissionState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate(PermissionState::fromNbt, PermissionState::new, KEY);
    }

    // --- ロール権限 ---

    public boolean permit(String roleId, String command) {
        rolePermissions.computeIfAbsent(roleId, k -> new HashSet<>()).add(command.toLowerCase());
        markDirty();
        return true;
    }

    public boolean deny(String roleId, String command) {
        Set<String> perms = rolePermissions.get(roleId);
        if (perms == null) return false;
        boolean removed = perms.remove(command.toLowerCase());
        if (perms.isEmpty()) {
            rolePermissions.remove(roleId);
        }
        if (removed) markDirty();
        return removed;
    }

    public Set<String> getPermissions(String roleId) {
        return Collections.unmodifiableSet(
                rolePermissions.getOrDefault(roleId, Collections.emptySet())
        );
    }

    public Map<String, Set<String>> getAllPermissions() {
        Map<String, Set<String>> result = new HashMap<>();
        rolePermissions.forEach((k, v) -> result.put(k, Collections.unmodifiableSet(v)));
        return Collections.unmodifiableMap(result);
    }

    /**
     * 指定roleIdが指定コマンドを実行可能か判定する。
     * コマンド名の先頭スラッシュは除去して比較する。
     */
    public boolean isAllowed(String roleId, String command) {
        String cmd = command.toLowerCase().startsWith("/")
                ? command.substring(1).toLowerCase()
                : command.toLowerCase();
        String rootCmd = cmd.split(" ")[0];

        // ブラックリストチェック（最優先）
        if (blacklist.contains(rootCmd)) return false;

        // 許可チェック
        Set<String> perms = rolePermissions.get(roleId);
        if (perms == null) return false;

        // 完全一致 or ワイルドカード "*"
        return perms.contains(rootCmd) || perms.contains("*");
    }

    // --- ブラックリスト ---

    public boolean addToBlacklist(String command) {
        boolean added = blacklist.add(command.toLowerCase());
        if (added) markDirty();
        return added;
    }

    public boolean removeFromBlacklist(String command) {
        boolean removed = blacklist.remove(command.toLowerCase());
        if (removed) markDirty();
        return removed;
    }

    public Set<String> getBlacklist() {
        return Collections.unmodifiableSet(blacklist);
    }

    // --- 永続化 ---

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        // ロール権限
        NbtCompound rolesNbt = new NbtCompound();
        rolePermissions.forEach((roleId, perms) -> {
            NbtList list = new NbtList();
            perms.forEach(cmd -> list.add(NbtString.of(cmd)));
            rolesNbt.put(roleId, list);
        });
        nbt.put("roles", rolesNbt);

        // ブラックリスト
        NbtList blacklistNbt = new NbtList();
        blacklist.forEach(cmd -> blacklistNbt.add(NbtString.of(cmd)));
        nbt.put("blacklist", blacklistNbt);

        return nbt;
    }

    public static PermissionState fromNbt(NbtCompound nbt) {
        PermissionState state = new PermissionState();
        // デフォルトブラックリストをクリアして保存済みデータで復元
        state.blacklist.clear();

        // ロール権限復元
        NbtCompound rolesNbt = nbt.getCompound("roles");
        for (String roleId : rolesNbt.getKeys()) {
            NbtList list = rolesNbt.getList(roleId, 8); // 8 = NbtString
            Set<String> perms = new HashSet<>();
            for (int i = 0; i < list.size(); i++) {
                perms.add(list.getString(i));
            }
            state.rolePermissions.put(roleId, perms);
        }

        // ブラックリスト復元
        NbtList blacklistNbt = nbt.getList("blacklist", 8);
        for (int i = 0; i < blacklistNbt.size(); i++) {
            state.blacklist.add(blacklistNbt.getString(i));
        }

        return state;
    }
}
