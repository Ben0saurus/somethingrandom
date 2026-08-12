package com.example.addon.modules;

import com.example.addon.SomethingRandom;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;

public class AutoQueue extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> targetIp = sgGeneral.add(new StringSetting.Builder()
        .name("target-ip")
        .description("The server IP to match.")
        .defaultValue("simpcraft.com")
        .build()
    );

    private final Setting<String> command = sgGeneral.add(new StringSetting.Builder()
        .name("command")
        .description("The command to run upon joining.")
        .defaultValue("/queue simpcraft")
        .build()
    );

    private final Setting<Integer> delayTicks = sgGeneral.add(new IntSetting.Builder()
        .name("delay-ticks")
        .description("Ticks to wait after joining before sending the command.")
        .defaultValue(40)
        .min(1)
        .sliderMax(100)
        .build()
    );

    private boolean shouldSend = false;
    private int timer = 0;

    public AutoQueue() {
        super(SomethingRandom.CATEGORY, "auto-queue", "Automatically sends a command when connecting to a specific IP.");
    }

    @Override
    public void onDeactivate() {
        shouldSend = false;
        timer = 0;
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {

        if (mc.getCurrentServer() == null) {
            ChatUtils.info("Joined Singleplayer world (skipping AutoQueue).");
            return;
        }

        String rawServerIp = mc.getCurrentServer().ip.toLowerCase();
        String target = targetIp.get().toLowerCase().trim();

        ChatUtils.info("[AutoQueue Debug] Detected Server IP: §e" + rawServerIp);

        String cleanedIp = rawServerIp.split(":")[0];

        if (cleanedIp.contains(target) || rawServerIp.contains(target)) {
            shouldSend = true;
            timer = delayTicks.get();
            ChatUtils.info("§a[AutoQueue] Target matched! Executing command in " + timer + " ticks...");
        } else {
            ChatUtils.info("§c[AutoQueue] IP did not match target: " + target);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!shouldSend || mc.player == null) return;

        if (timer > 0) {
            timer--;
        } else {
            ChatUtils.sendPlayerMsg(command.get());
            ChatUtils.info("§a[AutoQueue] Sent command: §f" + command.get());
            shouldSend = false;
        }
    }
}
