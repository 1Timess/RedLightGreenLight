Red Light Green Light Minecraft Plugin

A fun minigame plugin that brings the classic “Red Light, Green Light” game to your server. Players race from the start line to the finish line while obeying the light signals, with eliminations for moving during a red light.

-Features

Configurable game area, start and finish lines

Adjustable light intervals for more unpredictable gameplay

Eliminations with enderman death sound

Countdown sounds and action bar updates

Silent removal of players when they cross the finish line

Supports multiple players (By default can handle up to ~50, adjust as needed)

-Requirements

Minecraft: 1.20.1–1.21.8

Server Software: Paper, Spigot, or Purpur

Plugin API: Paper API 1.20.1+

No external dependencies required

-Installation

Download the .jar file.

Place it into your server's plugins/ folder.

Start or restart the server.

Configure the plugin via plugins/RedLightGreenLight/config.yml.

Configuration (config.yml)

Below is a sample configuration. Make sure the coordinates match the world your game will run in. In RLGGame.java, the plugin currently looks for a world named rlgmap. You can change this if needed.

game-area:
  corner1: [189, 28, -23]
  corner2: [131, 32, 35]

start-line:
  corner1: [189, 26, -22]
  corner2: [131, 30, -30]

finish-line:
  corner1: [189, 0, 37]
  corner2: [131, 320, 36]

barrier:
  corner1: [189, 64, -23]
  corner2: [131, 28, -23]

# Light interval in ticks (20 ticks ≈ 1 second)
light-interval-min-ticks: 60    # ~3 seconds
light-interval-max-ticks: 120   # ~6 seconds

elimination-delay-ticks: 20      # ~1 second reaction time
redlight-grace-ticks: 10        # delay after red light before movement counts


-Notes:

game-area: Defines the boundaries for red light detection and eliminations.

start-line & finish-line: Players spawn at the start line; crossing the finish line completes the game.

barrier: Blocks that drop at game start to release players.

light-interval-min-ticks and max-ticks: Randomizes red/green light durations for a more dynamic game.

Commands

/rlg start – Starts the game

/rlg stop – Ends the game

/rlg join – Join the game (if needed)

-Notes & Tips

The world in which the game runs must match the world name in RLGGame.java (rlgmap by default).

Make sure the coordinates in config.yml correspond to the same world.

Player elimination sound is an enderman death sound. Green/red lights have distinct sounds for easy recognition.

The plugin currently supports multiple players, but performance may vary depending on server specs and player count.

-Future Improvements

Doll animation at the finish line to indicate light changes visually
