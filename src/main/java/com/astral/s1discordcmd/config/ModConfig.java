package com.astral.s1discordcmd.config;

import com.astral.s1discordcmd.S1DiscordServerCMD;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "s1discordcmd.json";

    private String botToken = "";
    private String guildId = "";

    public String getBotToken() { return botToken; }
    public String getGuildId() { return guildId; }

    public static ModConfig load() {
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);

        if (!Files.exists(configFile)) {
            ModConfig defaults = new ModConfig();
            defaults.save();
            S1DiscordServerCMD.LOGGER.info(
                    "config/{} を生成しました。botToken と guildId を設定してサーバーを再起動してください。",
                    FILE_NAME
            );
            return defaults;
        }

        try {
            String json = Files.readString(configFile);
            ModConfig config = GSON.fromJson(json, ModConfig.class);
            if (config == null) return new ModConfig();
            return config;
        } catch (IOException e) {
            S1DiscordServerCMD.LOGGER.error("Failed to load config", e);
            return new ModConfig();
        }
    }

    private void save() {
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try {
            Files.writeString(configFile, GSON.toJson(this));
        } catch (IOException e) {
            S1DiscordServerCMD.LOGGER.error("Failed to save config", e);
        }
    }
}
