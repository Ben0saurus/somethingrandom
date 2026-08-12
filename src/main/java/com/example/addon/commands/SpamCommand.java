package com.example.addon.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;

public class SpamCommand extends Command {
    public SpamCommand() {
        super("spam", "Mutes a player for 12 hours for spamming.");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.then(argument("player", StringArgumentType.word())
            .suggests((context, suggestionsBuilder) ->
                SharedSuggestionProvider.suggest(context.getSource().getOnlinePlayerNames(), suggestionsBuilder)
            )
            .executes(context -> {
                String targetName = StringArgumentType.getString(context, "player");
                ChatUtils.sendPlayerMsg("/mute " + targetName + " 12h Spamming");
                return SINGLE_SUCCESS;
            })
        );
    }
}
