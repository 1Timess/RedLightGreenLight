package com.iTimess.redlightgreenlight;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.configuration.MemorySection;

import java.util.*;

public class RLGGame {
    public enum GameState { WAITING, COUNTDOWN, STARTED, ENDED }

    private final RedLightGreenLight plugin;
    private final Set<Player> players = new HashSet<>();
    private final List<Player> finishers = new ArrayList<>();
    private boolean greenLight = true;
    private GameState state = GameState.WAITING;

    private BukkitTask lightTask;
    private BukkitTask countdownTask;

    private final Location startCorner1;
    private final Location startCorner2;
    private final Location gameCorner1;
    private final Location gameCorner2;
    private final Location barrierCorner1;
    private final Location barrierCorner2;
    private final Location finishCorner1;
    private final Location finishCorner2;

    private final Map<Location, Material> originalBarrierBlocks = new HashMap<>();
    private boolean barriersRemoved = false;

    private long redLightStartMs = -1L;
    private final Random random = new Random();

    public RLGGame(RedLightGreenLight plugin) {
        this.plugin = plugin;

        startCorner1 = getLocationFromConfig("start-line.corner1");
        startCorner2 = getLocationFromConfig("start-line.corner2");
        gameCorner1 = getLocationFromConfig("game-area.corner1");
        gameCorner2 = getLocationFromConfig("game-area.corner2");
        barrierCorner1 = getLocationFromConfig("barrier.corner1");
        barrierCorner2 = getLocationFromConfig("barrier.corner2");
        finishCorner1 = getLocationFromConfig("finish-line.corner1");
        finishCorner2 = getLocationFromConfig("finish-line.corner2");

        plugin.getLogger().info("CONFIG LOADED:");
        plugin.getLogger().info("Start corner1: " + startCorner1);
        plugin.getLogger().info("Start corner2: " + startCorner2);
        plugin.getLogger().info("Game corner1: " + gameCorner1);
        plugin.getLogger().info("Game corner2: " + gameCorner2);
        plugin.getLogger().info("Barrier corner1: " + barrierCorner1);
        plugin.getLogger().info("Barrier corner2: " + barrierCorner2);
        plugin.getLogger().info("Finish corner1: " + finishCorner1);
        plugin.getLogger().info("Finish corner2: " + finishCorner2);
    }

    private Location getLocationFromConfig(String path) {
        Object raw = plugin.getConfig().get(path);

        double x = 0, y = 64, z = 0;
        if (raw instanceof List<?> list) {
            x = ((Number) list.get(0)).doubleValue();
            y = ((Number) list.get(1)).doubleValue();
            z = ((Number) list.get(2)).doubleValue();
        } else if (raw instanceof MemorySection section) {
            x = section.getDouble("0");
            y = section.getDouble("1");
            z = section.getDouble("2");
        }

        return new Location(Bukkit.getWorld("rlgmap"), x, y, z);
    }

    // --- Start/Countdown/Light logic ---
    public void startGame() {
        if (state == GameState.ENDED) resetGame();

        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (lightTask != null) {
            lightTask.cancel();
            lightTask = null;
        }

        if (state != GameState.WAITING) return;

        state = GameState.COUNTDOWN;
        plugin.getLogger().info("Countdown started...");

        players.clear();
        finishers.clear();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isOnStartLine(p)) addPlayer(p);
        }

        plugin.getLogger().info("Players added to game for countdown: " + players.size());

        countdownTask = new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                if (countdown > 0) {
                    String msg = ChatColor.YELLOW + "Game starting in " + countdown + "...";
                    broadcast(msg);
                    for (Player p : players) {
                        p.sendActionBar(msg);
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
                    }
                    countdown--;
                } else {
                    if (state != GameState.COUNTDOWN) {
                        this.cancel();
                        countdownTask = null;
                        return;
                    }

                    this.cancel();
                    countdownTask = null;

                    removeBarrierBlocks();
                    beginGame();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void beginGame() {
        if (state != GameState.COUNTDOWN) return;

        state = GameState.STARTED;
        greenLight = true;
        redLightStartMs = -1L;

        broadcast(ChatColor.GREEN + "Game started! Barriers are dropping. Go!");
        updateActionBars();

        scheduleNextLight();
    }

    private void scheduleNextLight() {
        long minTicks = plugin.getConfig().getLong("light-interval-min-ticks", 60L);
        long maxTicks = plugin.getConfig().getLong("light-interval-max-ticks", 120L);
        long nextInterval = minTicks + random.nextInt((int)(maxTicks - minTicks + 1));

        lightTask = new BukkitRunnable() {
            @Override
            public void run() {
                toggleLight();
                scheduleNextLight(); // recursively schedule next light
            }
        }.runTaskLater(plugin, nextInterval);
    }

    private void toggleLight() {
        greenLight = !greenLight;
        redLightStartMs = greenLight ? -1L : System.currentTimeMillis();

        broadcast(greenLight ? ChatColor.GREEN + "GREEN LIGHT! You may move!"
                : ChatColor.RED + "RED LIGHT! Stop moving!");

        // play sound for light toggle
        Sound lightSound = greenLight ? Sound.BLOCK_NOTE_BLOCK_HARP : Sound.BLOCK_NOTE_BLOCK_BASS;
        for (Player p : players) p.playSound(p.getLocation(), lightSound, 1.0f, 1.0f);

        updateActionBars();
    }

    public void stopGame() {
        if (state != GameState.STARTED && state != GameState.COUNTDOWN) return;

        state = GameState.ENDED;

        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (lightTask != null) {
            lightTask.cancel();
            lightTask = null;
        }

        broadcast(ChatColor.RED + "Game has ended.");

        resetGame();
        plugin.getLogger().info("Game stopped and reset.");
    }

    private void resetGame() {
        restoreBarrierBlocks();

        players.clear();
        finishers.clear();

        state = GameState.WAITING;
        greenLight = true;
        redLightStartMs = -1L;
    }

    public long getRedLightStartMs() { return redLightStartMs; }
    public boolean isRunning() { return state == GameState.STARTED; }
    public boolean isGreenLight() { return greenLight; }
    public boolean isParticipant(Player player) { return players.contains(player); }
    public boolean isOnStartLine(Player player) { return isInArea(player.getLocation(), startCorner1, startCorner2); }
    public boolean isInGameArea(Player player) { return isInArea(player.getLocation(), gameCorner1, gameCorner2); }
    public boolean isOnFinishLine(Player player) { return isInArea(player.getLocation(), finishCorner1, finishCorner2); }

    private boolean isInArea(Location loc, Location c1, Location c2) {
        double x1 = Math.min(c1.getX(), c2.getX()), y1 = Math.min(c1.getY(), c2.getY()), z1 = Math.min(c1.getZ(), c2.getZ());
        double x2 = Math.max(c1.getX(), c2.getX()), y2 = Math.max(c1.getY(), c2.getY()), z2 = Math.max(c1.getZ(), c2.getZ());

        return loc.getX() >= x1 && loc.getX() <= x2
                && loc.getY() >= y1 && loc.getY() <= y2
                && loc.getZ() >= z1 && loc.getZ() <= z2;
    }

    // --- Finish-line helpers ---
    public boolean hasFinished(Player player) {
        return finishers.contains(player);
    }

    public int getFinishersCount() {
        return finishers.size();
    }

    public void handlePlayerFinished(Player player) {
        if (hasFinished(player)) return;

        // silently remove player from active participants
        players.remove(player);

        int place = addFinisher(player);

        player.sendMessage(ChatColor.GREEN + "Congrats! You finished in " + placeToString(place) + " place!");
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        broadcast(ChatColor.GOLD + player.getName() + " has finished in " + placeToString(place) + " place!");

        if (players.isEmpty()) {
            broadcast(ChatColor.AQUA + "All players have finished! Game over.");
            stopGame();
        }
    }

    public int addFinisher(Player player) {
        if (!isRunning()) return -1;
        if (finishers.contains(player)) return finishers.indexOf(player) + 1;

        finishers.add(player);
        int place = finishers.size();
        if (place <= 5) broadcast(ChatColor.GOLD + player.getName() + " has finished in " + placeToString(place) + " place!");
        return place;
    }

    private String placeToString(int place) {
        return switch (place) {
            case 1 -> "1st";
            case 2 -> "2nd";
            case 3 -> "3rd";
            case 4 -> "4th";
            case 5 -> "5th";
            default -> place + "th";
        };
    }

    public void broadcast(String message) {
        for (Player p : players) p.sendMessage(message);
    }

    public void addPlayer(Player player) {
        if (!players.contains(player)) {
            players.add(player);
            player.sendMessage(ChatColor.AQUA + "You joined the game!");
            sendActionBar(player);
        }
    }

    public void removePlayer(Player player) {
        if (players.contains(player)) {
            players.remove(player);
            player.sendMessage(ChatColor.RED + "You have been eliminated.");
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_DEATH, 1.0f, 1.0f);
        }
    }

    // --- Barrier block management ---
    private void removeBarrierBlocks() {
        if (barriersRemoved) return;

        int minX = Math.min(barrierCorner1.getBlockX(), barrierCorner2.getBlockX());
        int maxX = Math.max(barrierCorner1.getBlockX(), barrierCorner2.getBlockX());
        int minY = Math.min(barrierCorner1.getBlockY(), barrierCorner2.getBlockY());
        int maxY = Math.max(barrierCorner1.getBlockY(), barrierCorner2.getBlockY());
        int minZ = Math.min(barrierCorner1.getBlockZ(), barrierCorner2.getBlockZ());
        int maxZ = Math.max(barrierCorner1.getBlockZ(), barrierCorner2.getBlockZ());

        originalBarrierBlocks.clear();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location loc = new Location(barrierCorner1.getWorld(), x, y, z);
                    Block block = loc.getBlock();
                    originalBarrierBlocks.put(loc.clone(), block.getType());
                    block.setType(Material.AIR);
                }
            }
        }

        barriersRemoved = true;
    }

    private void restoreBarrierBlocks() {
        if (!barriersRemoved || originalBarrierBlocks.isEmpty()) return;

        for (Map.Entry<Location, Material> entry : originalBarrierBlocks.entrySet()) {
            entry.getKey().getBlock().setType(entry.getValue());
        }
        originalBarrierBlocks.clear();
        barriersRemoved = false;
    }

    // --- Action bar updates ---
    private void updateActionBars() {
        for (Player p : players) sendActionBar(p);
    }

    private void sendActionBar(Player player) {
        String text = greenLight ? ChatColor.GREEN + "LIGHT IS GREEN" : ChatColor.RED + "LIGHT IS RED";
        player.sendActionBar(text);
    }
}
