# TastyFish Mod — Minecraft 26.1.2

Fabric 26.1.2 companion mod for the Tasty Fish farming leaderboard.

This repository is the 26.1.2-compatible copy of the TastyFish Mod. The 26.2 repository remains separate.

## What it does

The mod reads **SkySoft's live FARMING session tracker only**. It does not read `profitTracker.totals` or historical SkySoft data.

Every 30 seconds it sends the current session snapshot to the Tasty Fish backend:

- Minecraft username and UUID
- SkyBlock profile
- farming profit
- active farming time
- actions
- item counts
- pest kills
- a unique session ID

The server is responsible for accumulating the leaderboard. If Minecraft is restarted, the new session gets a new ID and the player's existing leaderboard total is preserved.

## Minecraft 26.1.2

- Minecraft: `26.1.2`
- Fabric Loader: `0.19.3`
- Fabric API: `0.155.2+26.1.2`
- Fabric Loom: `1.16.1`
- Java: `25`

Minecraft 26.1+ uses the unobfuscated Minecraft/Fabric toolchain, so this project uses the `net.fabricmc.fabric-loom` plugin and does not use Yarn mappings.
