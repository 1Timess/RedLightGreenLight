package com.iTimess.redlightgreenlight;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RLGListener implements Listener {

    private final RedLightGreenLight plugin;
    private final Map<UUID, BukkitTask> pendingEliminations = new HashMap<>();

    public RLGListener(RedLightGreenLight plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        RLGGame game = plugin.getGame();

        if (!game.isRunning()) return;
        if (!game.isParticipant(player)) return;

        // --- Finish line check first ---
        if (game.isOnFinishLine(player)) {
            game.handlePlayerFinished(player);
            return; // stop further processing for this tick
        }

        // --- Red light elimination check ---
        if (!game.isInGameArea(player)) return; // ensure inside game area before elimination
        if (!game.isGreenLight() && moved(event)) {
            long graceTicks = plugin.getConfig().getLong("redlight-grace-ticks", 10L);
            long eliminationDelay = plugin.getConfig().getLong("elimination-delay-ticks", 20L);

            long redStartMs = game.getRedLightStartMs();
            if (redStartMs >= 0) {
                long nowMs = System.currentTimeMillis();
                long graceMs = graceTicks * 50L;
                if (nowMs - redStartMs < graceMs) return; // still in grace period
            }

            UUID id = player.getUniqueId();
            if (pendingEliminations.containsKey(id)) return;

            BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                pendingEliminations.remove(id);

                if (game.isParticipant(player) && !game.isGreenLight() && game.isInGameArea(player)) {
                    game.removePlayer(player);
                    player.setHealth(0.0);
                    player.sendMessage(ChatColor.RED + "You moved during RED LIGHT! Eliminated.");
                }
            }, eliminationDelay);

            pendingEliminations.put(id, task);
        }
    }

    // Helper to check if the player actually moved
    private boolean moved(PlayerMoveEvent event) {
        return !event.getFrom().getBlock().equals(event.getTo().getBlock());
    }
}
