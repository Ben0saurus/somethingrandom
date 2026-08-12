package com.example.addon.modules;

import com.example.addon.SomethingRandom;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JoinLeaveNotifier extends Module {

    private final Map<UUID, String> playerCache = new HashMap<>();

    public JoinLeaveNotifier() {
        super(SomethingRandom.CATEGORY, "join-leave-notifier", "Notifies when players join or leave the server.");
    }

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


        if (event.packet instanceof ClientboundPlayerInfoUpdatePacket packet) {
            for (ClientboundPlayerInfoUpdatePacket.Entry entry : packet.newEntries()) {

                if (packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) {
                    UUID id = entry.profileId();
                    String name = entry.profile().name();

                    if (name != null && !playerCache.containsKey(id)) {
                        playerCache.put(id, name);


                        if (!id.equals(mc.player.getUUID())) {
                            ChatUtils.info("§a[+] §f" + name + " joined the game.");
                        }
                    }
                }
            }
        }


        if (event.packet instanceof ClientboundPlayerInfoRemovePacket packet) {
            for (UUID id : packet.profileIds()) {
                if (playerCache.containsKey(id)) {
                    String name = playerCache.get(id);
                    playerCache.remove(id);


                    if (!id.equals(mc.player.getUUID())) {
                        ChatUtils.info("§c[-] §f" + name + " left the game.");
                    }
                }
            }
        }
    }
}
