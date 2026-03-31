package com.astral.s1discordcmd.discord;

import net.minecraft.server.command.CommandOutput;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * コマンド実行結果を収集するCommandOutput実装。
 * Discord側にフィードバックを返すために使用する。
 */
public class OutputCollector implements CommandOutput {

    private final List<String> messages = new ArrayList<>();

    @Override
    public void sendMessage(Text message) {
        messages.add(message.getString());
    }

    @Override
    public boolean shouldReceiveFeedback() {
        return true;
    }

    @Override
    public boolean shouldTrackOutput() {
        return true;
    }

    @Override
    public boolean shouldBroadcastConsoleToOps() {
        return false;
    }

    public String getOutput() {
        return String.join("\n", messages);
    }
}
