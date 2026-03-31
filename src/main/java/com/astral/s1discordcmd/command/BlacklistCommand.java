package com.astral.s1discordcmd.command;

import com.astral.s1discordcmd.data.PermissionState;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * /s1 blacklist add|remove|list - 危険コマンドのブラックリスト管理。
 * ブラックリストされたコマンドはpermitされていても実行不可。
 */
public final class BlacklistCommand {

    private BlacklistCommand() {}

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(literal("blacklist")
                .requires(source -> source.hasPermissionLevel(4))
                // /s1 blacklist list
                .then(literal("list")
                        .executes(ctx -> {
                            PermissionState state = PermissionState.get(ctx.getSource().getServer());
                            Set<String> blacklist = state.getBlacklist();

                            if (blacklist.isEmpty()) {
                                ctx.getSource().sendFeedback(
                                        Text.literal("Blacklist is empty"), false
                                );
                                return 0;
                            }

                            List<String> sorted = new ArrayList<>(blacklist);
                            Collections.sort(sorted);

                            ctx.getSource().sendFeedback(
                                    Text.literal("Blacklisted commands: " + String.join(", ", sorted)),
                                    false
                            );
                            return blacklist.size();
                        })
                )
                // /s1 blacklist add <command>
                .then(literal("add")
                        .then(argument("command", StringArgumentType.word())
                                .executes(ctx -> {
                                    String command = StringArgumentType.getString(ctx, "command");
                                    PermissionState state = PermissionState.get(ctx.getSource().getServer());

                                    if (state.addToBlacklist(command)) {
                                        ctx.getSource().sendFeedback(
                                                Text.literal("Added '" + command + "' to blacklist"),
                                                true
                                        );
                                        return 1;
                                    } else {
                                        ctx.getSource().sendError(
                                                Text.literal("'" + command + "' is already blacklisted")
                                        );
                                        return 0;
                                    }
                                })
                        )
                )
                // /s1 blacklist remove <command>
                .then(literal("remove")
                        .then(argument("command", StringArgumentType.word())
                                .executes(ctx -> {
                                    String command = StringArgumentType.getString(ctx, "command");
                                    PermissionState state = PermissionState.get(ctx.getSource().getServer());

                                    if (state.removeFromBlacklist(command)) {
                                        ctx.getSource().sendFeedback(
                                                Text.literal("Removed '" + command + "' from blacklist"),
                                                true
                                        );
                                        return 1;
                                    } else {
                                        ctx.getSource().sendError(
                                                Text.literal("'" + command + "' was not in the blacklist")
                                        );
                                        return 0;
                                    }
                                })
                        )
                )
        );
    }
}
