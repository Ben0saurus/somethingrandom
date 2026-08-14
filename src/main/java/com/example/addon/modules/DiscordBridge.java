package com.example.addon.modules;

import com.example.addon.SomethingRandom;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DiscordBridge extends Module {

    private final Map<UUID, String> playerCache = new HashMap<>();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public DiscordBridge() {
        super(SomethingRandom.CATEGORY, "discord-bridge", "Sends all chat messages through a discord webhook");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> webhook = sgGeneral.add(new StringSetting.Builder()
        .name("webhook")
        .description("The discord Webhook link")
        .defaultValue("")
        .build()
    );

    @Override
    public void onActivate() {
        playerCache.clear();

        if (mc.getConnection() != null) {
            mc.getConnection().getOnlinePlayers().forEach(entry -> {
                String name = entry.getProfile().name();
                if (name != null) playerCache.put(entry.getProfile().id(), name);
            });
        }
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (mc.player == null || mc.level == null) return;


        if (event.packet instanceof ClientboundPlayerChatPacket packet) {
            String message = packet.unsignedContent() != null
                ? packet.unsignedContent().getString()
                : packet.body().content();

            String sender = packet.sender().toString();

            String senderName = playerCache.getOrDefault(packet.sender(), "Player");

            sendToWebhook("[" + senderName + "] " + message);
        }

        if (event.packet instanceof ClientboundSystemChatPacket packet) {
            String message = packet.content().getString();
            sendToWebhook(message);
        }
    }


    private void sendToWebhook(String content) {
        String url = webhook.get();
        if (url == null || url.trim().isEmpty()) return;

        String cleanContent = content.replaceAll("§[0-9a-fk-or]", "");

        String jsonPayload = "{\"content\": \"" + sanitizeJson(cleanContent) + "\"}";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
        }
    }

    private String sanitizeJson(String input) {
        return input.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "");
    }
}
