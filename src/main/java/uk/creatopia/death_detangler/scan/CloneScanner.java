package uk.creatopia.death_detangler.scan;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import uk.creatopia.death_detangler.Config;
import uk.creatopia.death_detangler.util.CloneHeuristics;

import java.util.*;

/**
 * Scans all dimensions for orphan player clones and optionally removes them.
 */
public class CloneScanner {

    /**
     * Scans all server levels for orphan player clones.
     * 
     * @param server The Minecraft server instance
     * @param remove If true, removes detected clones. If false, only counts them.
     * @return The number of clones found (and removed if remove=true)
     */
    public static int scanAndFix(MinecraftServer server, boolean remove) {
        ScanResult result = scanWithDetails(server, remove);
        return result.getTotalFound();
    }

    /**
     * Scans all server levels for orphan player clones with detailed reporting.
     * 
     * @param server The Minecraft server instance
     * @param remove If true, removes detected clones. If false, only counts them.
     * @return Detailed scan result with per-dimension stats and entity info
     */
    public static ScanResult scanWithDetails(MinecraftServer server, boolean remove) {
        int totalCount = 0;
        Map<ResourceKey<Level>, Integer> perDimension = new HashMap<>();
        List<ScanResult.EntityInfo> entities = new ArrayList<>();

        for (ServerLevel level : server.getAllLevels()) {
            if (Config.verboseLogging) {
                com.mojang.logging.LogUtils.getLogger().info("Scanning dimension: {}", level.dimension().location());
            }

            int dimensionCount = 0;

            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof ServerPlayer player)) continue;

                if (CloneHeuristics.isOrphanClone(server, player)) {
                    totalCount++;
                    dimensionCount++;

                    // Collect entity details
                    List<String> flags = CloneHeuristics.getCloneFlags(server, player);
                    entities.add(new ScanResult.EntityInfo(
                        player.getUUID().toString(),
                        level.dimension().location().toString(),
                        player.tickCount,
                        flags
                    ));

                    if (remove) {
                        player.discard();
                        player.remove(Entity.RemovalReason.DISCARDED);
                    }
                }
            }

            if (dimensionCount > 0) {
                perDimension.put(level.dimension(), dimensionCount);
            }
        }

        return new ScanResult(totalCount, perDimension, entities);
    }
}

