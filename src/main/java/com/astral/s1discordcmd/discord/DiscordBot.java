package com.astral.s1discordcmd.discord;

import com.astral.s1discordcmd.S1DiscordServerCMD;
import com.astral.s1discordcmd.config.ModConfig;
import com.astral.s1discordcmd.data.PermissionState;
import com.mojang.brigadier.tree.CommandNode;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DiscordBot extends ListenerAdapter {

    private JDA jda;
    private MinecraftServer server;
    private String guildId;

    public void start(ModConfig config, MinecraftServer server) {
        this.server = server;
        this.guildId = config.getGuildId();

        if (config.getBotToken().isEmpty()) {
            S1DiscordServerCMD.LOGGER.warn("botToken が未設定のため Discord Bot を起動しません。");
            return;
        }
        if (guildId.isEmpty()) {
            S1DiscordServerCMD.LOGGER.warn("guildId が未設定のため Discord Bot を起動しません。");
            return;
        }

        try {
            jda = JDABuilder.createDefault(config.getBotToken())
                    .addEventListeners(this)
                    .build();
        } catch (Exception e) {
            S1DiscordServerCMD.LOGGER.error("Discord Bot の起動に失敗しました", e);
        }
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
            try {
                if (!jda.awaitShutdown(Duration.ofSeconds(10))) {
                    jda.shutdownNow();
                }
            } catch (InterruptedException e) {
                jda.shutdownNow();
                Thread.currentThread().interrupt();
            }
            S1DiscordServerCMD.LOGGER.info("Discord Bot を停止しました");
        }
    }

    // --- JDA Events ---

    @Override
    public void onReady(ReadyEvent event) {
        S1DiscordServerCMD.LOGGER.info("Discord Bot 接続完了: {}",
                event.getJDA().getSelfUser().getName());

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            S1DiscordServerCMD.LOGGER.error("Guild が見つかりません: {}", guildId);
            return;
        }

        // /mc スラッシュコマンドを登録
        guild.updateCommands()
                .addCommands(
                        Commands.slash("mc", "Minecraftサーバーコマンドを実行")
                                .addOption(OptionType.STRING, "command", "実行するコマンド", true, true)
                                .addOption(OptionType.STRING, "args", "コマンド引数", false)
                )
                .queue(
                        success -> S1DiscordServerCMD.LOGGER.info("スラッシュコマンドを登録しました"),
                        failure -> S1DiscordServerCMD.LOGGER.error("スラッシュコマンド登録に失敗", failure)
                );
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("mc")) return;

        Member member = event.getMember();
        if (member == null) return;

        String command = event.getOption("command", OptionMapping::getAsString);
        if (command == null || command.isEmpty()) return;

        OptionMapping argsMapping = event.getOption("args");
        String args = argsMapping != null ? argsMapping.getAsString() : "";
        String fullCommand = args.isEmpty() ? command : command + " " + args;

        // 権限チェック: いずれかのロールが許可されていればOK
        PermissionState state = PermissionState.get(server);
        boolean allowed = false;
        for (Role role : member.getRoles()) {
            if (state.isAllowed(role.getId(), fullCommand)) {
                allowed = true;
                break;
            }
        }

        if (!allowed) {
            event.reply("Permission denied: `" + command + "`")
                    .setEphemeral(true).queue();
            return;
        }

        // 応答を遅延（コマンド実行に時間がかかる可能性）
        event.deferReply().queue();

        // サーバースレッドでコマンド実行
        server.execute(() -> {
            OutputCollector collector = new OutputCollector();
            ServerCommandSource source = new ServerCommandSource(
                    collector,
                    Vec3d.ZERO,
                    Vec2f.ZERO,
                    server.getOverworld(),
                    4,
                    member.getEffectiveName(),
                    Text.literal(member.getEffectiveName()),
                    server,
                    null
            );

            try {
                server.getCommandManager().getDispatcher().execute(fullCommand, source);
            } catch (Exception e) {
                collector.sendMessage(Text.literal("Error: " + e.getMessage()));
            }

            String output = collector.getOutput();
            String response = output.isEmpty()
                    ? "`" + fullCommand + "` を実行しました。"
                    : "```\n" + output + "\n```";

            // Discordメッセージ上限(2000文字)に収める
            if (response.length() > 2000) {
                response = response.substring(0, 1994) + "\n```";
            }

            event.getHook().sendMessage(response).queue();
        });
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (!event.getName().equals("mc")) return;
        if (!event.getFocusedOption().getName().equals("command")) return;

        String input = event.getFocusedOption().getValue().toLowerCase();
        Member member = event.getMember();
        if (member == null) return;

        PermissionState state = PermissionState.get(server);
        Set<String> permitted = new HashSet<>();

        for (Role role : member.getRoles()) {
            Set<String> rolePerms = state.getPermissions(role.getId());
            if (rolePerms.contains("*")) {
                // ワイルドカード: ブラックリスト以外の全コマンドを返す
                for (CommandNode<ServerCommandSource> node :
                        server.getCommandManager().getDispatcher().getRoot().getChildren()) {
                    String name = node.getName();
                    if (!state.getBlacklist().contains(name)) {
                        permitted.add(name);
                    }
                }
                break;
            }
            for (String perm : rolePerms) {
                if (!state.getBlacklist().contains(perm)) {
                    permitted.add(perm);
                }
            }
        }

        List<Command.Choice> choices = permitted.stream()
                .filter(cmd -> cmd.startsWith(input))
                .sorted()
                .limit(25)
                .map(cmd -> new Command.Choice(cmd, cmd))
                .collect(Collectors.toList());

        event.replyChoices(choices).queue();
    }
}
