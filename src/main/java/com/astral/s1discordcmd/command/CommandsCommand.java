package com.astral.s1discordcmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.server.command.ServerCommandSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.minecraft.server.command.CommandManager.literal;

/**
 * /s1 commands - サーバー上の全登録コマンドをJSON配列で返す。
 * Discord Botがこの出力をパースしてスラッシュコマンドを自動登録する想定。
 */
public final class CommandsCommand {

    private CommandsCommand() {}

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(literal("commands")
                .requires(source -> source.hasPermissionLevel(4))
                .executes(ctx -> {
                    CommandDispatcher<ServerCommandSource> dispatcher =
                            ctx.getSource().getServer().getCommandManager().getDispatcher();

                    List<String> names = new ArrayList<>();
                    for (CommandNode<ServerCommandSource> node : dispatcher.getRoot().getChildren()) {
                        names.add(node.getName());
                    }
                    Collections.sort(names);

                    // JSON配列形式で出力
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < names.size(); i++) {
                        if (i > 0) sb.append(",");
                        sb.append("\"").append(names.get(i)).append("\"");
                    }
                    sb.append("]");

                    ctx.getSource().sendFeedback(
                            net.minecraft.text.Text.literal(sb.toString()), false
                    );
                    return names.size();
                })
        );
    }
}
