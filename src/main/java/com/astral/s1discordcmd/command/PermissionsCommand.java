package com.astral.s1discordcmd.command;

import com.astral.s1discordcmd.data.PermissionState;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.Set;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * /s1 permissions [roleId] - ロールの権限一覧を表示する。
 * roleId省略で全ロールの権限を表示。
 */
public final class PermissionsCommand {

    private PermissionsCommand() {}

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(literal("permissions")
                .requires(source -> source.hasPermissionLevel(4))
                // 引数なし: 全ロール表示
                .executes(ctx -> {
                    PermissionState state = PermissionState.get(ctx.getSource().getServer());
                    Map<String, Set<String>> all = state.getAllPermissions();

                    if (all.isEmpty()) {
                        ctx.getSource().sendFeedback(
                                Text.literal("No permissions configured"), false
                        );
                        return 0;
                    }

                    StringBuilder sb = new StringBuilder();
                    all.forEach((roleId, perms) -> {
                        sb.append("Role ").append(roleId).append(": ");
                        sb.append(String.join(", ", perms));
                        sb.append("\n");
                    });

                    ctx.getSource().sendFeedback(
                            Text.literal(sb.toString().trim()), false
                    );
                    return all.size();
                })
                // 引数あり: 特定ロール表示
                .then(argument("roleId", StringArgumentType.word())
                        .executes(ctx -> {
                            String roleId = StringArgumentType.getString(ctx, "roleId");
                            PermissionState state = PermissionState.get(ctx.getSource().getServer());
                            Set<String> perms = state.getPermissions(roleId);

                            if (perms.isEmpty()) {
                                ctx.getSource().sendFeedback(
                                        Text.literal("Role " + roleId + " has no permissions"), false
                                );
                                return 0;
                            }

                            ctx.getSource().sendFeedback(
                                    Text.literal("Role " + roleId + ": " + String.join(", ", perms)),
                                    false
                            );
                            return perms.size();
                        })
                )
        );
    }
}
