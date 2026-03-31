package com.astral.s1discordcmd;

import com.astral.s1discordcmd.command.*;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * S1 Discord Server CMD - メインエントリポイント。
 * Discord BotからMCRCON経由でサーバーコマンドを安全に実行するためのゲートウェイmod。
 */
public class S1DiscordServerCMD implements DedicatedServerModInitializer {

    public static final String MOD_ID = "s1discordcmd";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeServer() {
        LOGGER.info("S1 Discord Server CMD を初期化中...");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var root = CommandManager.literal("s1")
                    .requires(source -> source.hasPermissionLevel(4));

            // 各サブコマンドを登録
            CommandsCommand.register(root);
            PermitCommand.register(root);
            DenyCommand.register(root);
            PermissionsCommand.register(root);
            ExecCommand.register(root);
            BlacklistCommand.register(root);

            dispatcher.register(root);
            LOGGER.info("S1コマンドを登録しました");
        });
    }
}
