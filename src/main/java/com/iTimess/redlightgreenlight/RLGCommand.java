package com.iTimess.redlightgreenlight;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RLGCommand implements CommandExecutor {
    private final RedLightGreenLight plugin;

    public RLGCommand(RedLightGreenLight plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§eUsage: /rlg <start|stop|join|leave>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                plugin.getGame().startGame();
                sender.sendMessage("§aRed Light Green Light started!");
            }
            case "stop" -> {
                plugin.getGame().stopGame();
                sender.sendMessage("§cGame stopped.");
            }
            case "join" -> {
                if (sender instanceof Player player) {
                    plugin.getGame().addPlayer(player);
                }
            }
            case "leave" -> {
                if (sender instanceof Player player) {
                    plugin.getGame().removePlayer(player);
                }
            }
            default -> sender.sendMessage("§cUnknown subcommand!");
        }

        return true;
    }
}