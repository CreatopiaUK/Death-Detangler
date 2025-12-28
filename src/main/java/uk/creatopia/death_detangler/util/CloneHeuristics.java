package uk.creatopia.death_detangler.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Heuristic detection logic for identifying orphan player clones.
 * Uses multiple signals to avoid false positives.
 */
public class CloneHeuristics {

    /**
     * Determines if a player entity is likely an orphan clone that should be removed.
     * Uses multiple heuristics to avoid false positives.
     * 
     * @param server The Minecraft server instance
     * @param player The player entity to check
     * @return true if the player is likely an orphan clone
     */
    public static boolean isOrphanClone(MinecraftServer server, ServerPlayer player) {
        return getCloneFlags(server, player).size() >= 2;
    }

    /**
     * Gets the list of flags that indicate this player is a clone.
     * 
     * @param server The Minecraft server instance
     * @param player The player entity to check
     * @return List of flag strings indicating clone status
     */
    public static List<String> getCloneFlags(MinecraftServer server, ServerPlayer player) {
        List<String> flags = new ArrayList<>();

        // 1. Not actually connected
        if (player.connection == null) {
            flags.add("no_connection");
        }

        // 2. Not in player list
        if (server.getPlayerList().getPlayer(player.getUUID()) == null) {
            flags.add("not_in_player_list");
        }

        // 3. Has revival / corpse related tags
        CompoundTag tag = player.getPersistentData();
        boolean hasRevivalTag = false;
        if (tag.contains("HardcoreRevival")) {
            flags.add("hardcore_revival");
            hasRevivalTag = true;
        }
        if (tag.contains("revival")) {
            flags.add("revival");
            hasRevivalTag = true;
        }
        if (tag.contains("corpse")) {
            flags.add("corpse");
            hasRevivalTag = true;
        }
        if (tag.contains("grave")) {
            flags.add("grave");
            hasRevivalTag = true;
        }
        if (tag.contains("IsRevivalClone")) {
            flags.add("is_revival_clone");
            hasRevivalTag = true;
        }
        if (tag.contains("HCRevivalFixClone")) {
            flags.add("hc_revival_fix_clone");
            hasRevivalTag = true;
        }
        if (hasRevivalTag) {
            flags.add("revival_tag");
        }

        // 4. Exists too long without a connection
        if (player.tickCount > 200) {
            flags.add("high_tick_count");
        }

        return flags;
    }
}

