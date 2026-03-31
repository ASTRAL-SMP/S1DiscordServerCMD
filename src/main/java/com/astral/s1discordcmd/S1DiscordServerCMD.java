package com.astral.s1discordcmd;

import com.astral.s1discordcmd.command.*;
import com.astral.s1discordcmd.config.ModConfig;
import com.astral.s1discordcmd.discord.DiscordBot;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.command.CommandManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S1DiscordServerCMD implements DedicatedServerModInitializer {

    public static final String MOD_ID = "s1discordcmd";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private final DiscordBot discordBot = new DiscordBot();

    @Override
    public void onInitializeServer() {
        LOGGER.info("S1 Discord Server CMD を初期化中...");

        // /s1 コマンド登録
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var root = CommandManager.literal("s1")
                    .requires(source -> source.hasPermissionLevel(4));

            CommandsCommand.register(root);
            PermitCommand.register(root);
            DenyCommand.register(root);
            PermissionsCommand.register(root);
            ExecCommand.register(root);
            BlacklistCommand.register(root);

            dispatcher.register(root);
            LOGGER.info("S1コマンドを登録しました");
        });

        // サーバー起動完了時にDiscord Bot起動
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ModConfig config = ModConfig.load();
            discordBot.start(config, server);
        });

        // サーバー停止時にDiscord Botシャットダウン
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            discordBot.shutdown();
        });
    }
}
