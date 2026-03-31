package com.astral.s1discordcmd.command;

import com.astral.s1discordcmd.data.PermissionState;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * /s1 exec <roleId> <command...> - ロール権限チェック付きでコマンドを実行する。
 * Discord BotがRCON経由でこのコマンドを叩く。
 * roleIdでユーザーのDiscordロールを指定し、許可されたコマンドのみ実行される。
 */
public final class ExecCommand {

    private ExecCommand() {}

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(literal("exec")
                .requires(source -> source.hasPermissionLevel(4))
                .then(argument("roleId", StringArgumentType.word())
                        .then(argument("command", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String roleId = StringArgumentType.getString(ctx, "roleId");
                                    String command = StringArgumentType.getString(ctx, "command");

                                    MinecraftServer server = ctx.getSource().getServer();
                                    PermissionState state = PermissionState.get(server);

                                    // 権限チェック
                                    if (!state.isAllowed(roleId, command)) {
                                        ctx.getSource().sendError(
                                                Text.literal("DENIED: Role " + roleId
                                                        + " is not permitted to execute: " + command)
                                        );
                                        return 0;
                                    }

                                    // コマンド先頭のスラッシュを除去
                                    String cleanCmd = command.startsWith("/")
                                            ? command.substring(1)
                                            : command;

                                    // サーバーコマンドとして実行
                                    int result = server.getCommandManager()
                                            .executeWithPrefix(
                                                    server.getCommandSource(),
                                                    cleanCmd
                                            );

                                    if (result > 0) {
                                        ctx.getSource().sendFeedback(
                                                Text.literal("OK: Executed '" + cleanCmd
                                                        + "' for role " + roleId),
                                                true
                                        );
                                    }

                                    return result;
                                })
                        )
                )
        );
    }
}
