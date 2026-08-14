package com.example.addon;

import com.example.addon.commands.AdvertisingCommand;
import com.example.addon.commands.MuteCommand;
import com.example.addon.commands.RacismCommand;
import com.example.addon.commands.SpamCommand;
import com.example.addon.modules.*;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class SomethingRandom extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("SomethingRandom");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor Addon Template");

        // Modules
        Modules.get().add(new JoinLeaveNotifier());
        Modules.get().add(new JoinAlarm());
        Modules.get().add(new DiscordBridge());
        //Modules.get().add(new FreecamFlight());
        //Modules.get().add(new AutoQueue());

        // Commands
        Commands.add(new MuteCommand());
        Commands.add(new RacismCommand());
        Commands.add(new SpamCommand());
        Commands.add(new AdvertisingCommand());

    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.example.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("Ben0saurus", "something-random");
    }
}
