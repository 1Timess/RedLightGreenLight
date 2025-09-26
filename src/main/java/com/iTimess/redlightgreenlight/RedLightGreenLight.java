package com.iTimess.redlightgreenlight;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class RedLightGreenLight extends JavaPlugin {
    private static RedLightGreenLight instance;
    private RLGGame game; // game manager

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("Red Light Green Light enabled!");

        // Save default config if not exists
        saveDefaultConfig();

        // Initialize game manager
        game = new RLGGame(this);

        // Register command
        this.getCommand("rlg").setExecutor(new RLGCommand(this));

        // Register listener
        Bukkit.getPluginManager().registerEvents(new RLGListener(this), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("Red Light Green Light disabled!");
        if (game != null && game.isRunning()) {
            game.stopGame(); // make sure any running game shuts down cleanly
        }
    }

    public static RedLightGreenLight getInstance() {
        return instance;
    }

    public RLGGame getGame() {
        return game;
    }
}