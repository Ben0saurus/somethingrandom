package com.example.addon.modules;

import com.example.addon.SomethingRandom;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.sounds.SoundEvents;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JoinAlarm extends Module {

    private final Map<UUID, String> playerCache = new HashMap<>();

    public JoinAlarm() {
        super(SomethingRandom.CATEGORY, "join-alarm", "Plays a sound when certain players join the server.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> targetPlayers = sgGeneral.add(new StringListSetting.Builder()
        .name("target-players")
        .description("Target Players")
        .defaultValue(List.of("ItsDumzy", "FractureZen", "charq", "Plutoren", "Bramsy"))
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

        if (event.packet instanceof ClientboundPlayerInfoUpdatePacket packet) {
            if (packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) {
                for (ClientboundPlayerInfoUpdatePacket.Entry entry : packet.newEntries()) {
                    UUID id = entry.profileId();

                    String name = entry.profile() != null ? entry.profile().name() : null;
                    if (name == null && entry.profile() != null) name = entry.profile().name();

                    if (name == null || name.isEmpty()) continue;

                    if (!playerCache.containsKey(id)) {
                        playerCache.put(id, name);

                        String finalName = name;
                        boolean isTarget = targetPlayers.get().stream()
                            .anyMatch(target -> target.equalsIgnoreCase(finalName));

                        if (isTarget) {

                            mc.getSoundManager().play(
                                SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f, 2.0f)
                            );

                        }
                    }
                }
            }
        }

        // 2. Handle Player Leaves (clears cache so re-joins trigger the sound again)
        if (event.packet instanceof ClientboundPlayerInfoRemovePacket packet) {
            for (UUID id : packet.profileIds()) {
                playerCache.remove(id);
            }
        }
    }
}
