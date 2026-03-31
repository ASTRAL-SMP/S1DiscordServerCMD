package com.astral.s1discordcmd.command;

import com.astral.s1discordcmd.data.PermissionState;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * /s1 deny <roleId> <command> - Discord roleからコマンド実行権限を削除する。
 */
public final class DenyCommand {

    private DenyCommand() {}

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(literal("deny")
                .requires(source -> source.hasPermissionLevel(4))
                .then(argument("roleId", StringArgumentType.word())
                        .then(argument("command", StringArgumentType.word())
                                .executes(ctx -> {
                                    String roleId = StringArgumentType.getString(ctx, "roleId");
                                    String command = StringArgumentType.getString(ctx, "command");

                                    PermissionState state = PermissionState.get(ctx.getSource().getServer());
                                    boolean removed = state.deny(roleId, command);

                                    if (removed) {
                                        ctx.getSource().sendFeedback(
                                                Text.literal("Denied role " + roleId + " from command: " + command),
                                                true
                                        );
                                        return 1;
                                    } else {
                                        ctx.getSource().sendError(
                                                Text.literal("Role " + roleId + " did not have permission for: " + command)
                                        );
                                        return 0;
                                    }
                                })
                        )
                )
        );
    }
}
