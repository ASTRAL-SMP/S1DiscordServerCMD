package com.astral.s1discordcmd.command;

import com.astral.s1discordcmd.data.PermissionState;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * /s1 permit <roleId> <command> - Discord roleにコマンド実行を許可する。
 */
public final class PermitCommand {

    private PermitCommand() {}

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(literal("permit")
                .requires(source -> source.hasPermissionLevel(4))
                .then(argument("roleId", StringArgumentType.word())
                        .then(argument("command", StringArgumentType.word())
                                .executes(ctx -> {
                                    String roleId = StringArgumentType.getString(ctx, "roleId");
                                    String command = StringArgumentType.getString(ctx, "command");

                                    PermissionState state = PermissionState.get(ctx.getSource().getServer());

                                    // ブラックリストチェック
                                    if (state.getBlacklist().contains(command.toLowerCase())) {
                                        ctx.getSource().sendError(
                                                Text.literal("Command '" + command + "' is blacklisted and cannot be permitted")
                                        );
                                        return 0;
                                    }

                                    state.permit(roleId, command);
                                    ctx.getSource().sendFeedback(
                                            Text.literal("Permitted role " + roleId + " to use command: " + command),
                                            true
                                    );
                                    return 1;
                                })
                        )
                )
        );
    }
}
