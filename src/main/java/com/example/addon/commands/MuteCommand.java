package com.example.addon.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;

public class MuteCommand extends Command {
    public MuteCommand() {
        super("mute", "Mutes a player with full tab auto-completion.");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.then(argument("player", StringArgumentType.word())
            .suggests((context, suggestionsBuilder) ->
                SharedSuggestionProvider.suggest(context.getSource().getOnlinePlayerNames(), suggestionsBuilder)
            )
            .then(argument("length", StringArgumentType.word())
                .then(argument("reason", StringArgumentType.greedyString()).executes(context -> {
                    String targetName = StringArgumentType.getString(context, "player");
                    String length = StringArgumentType.getString(context, "length");
                    String reason = StringArgumentType.getString(context, "reason");

                    ChatUtils.sendPlayerMsg("/mute " + targetName + " " + length + " " + reason);
                    return SINGLE_SUCCESS;
                }))
            )
        );
    }
}
