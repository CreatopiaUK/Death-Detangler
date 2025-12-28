package uk.creatopia.death_detangler.scan;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * Result of a clone scan operation, containing per-dimension statistics
 * and detailed entity information.
 */
public class ScanResult {
    private final int totalFound;
    private final Map<ResourceKey<Level>, Integer> perDimension;
    private final List<EntityInfo> entities;

    public ScanResult(int totalFound, Map<ResourceKey<Level>, Integer> perDimension, List<EntityInfo> entities) {
        this.totalFound = totalFound;
        this.perDimension = perDimension;
        this.entities = entities;
    }

    public int getTotalFound() {
        return totalFound;
    }

    public Map<ResourceKey<Level>, Integer> getPerDimension() {
        return perDimension;
    }

    public List<EntityInfo> getEntities() {
        return entities;
    }

    /**
     * Information about a detected clone entity.
     */
    public static class EntityInfo {
        private final String uuid;
        private final String dimension;
        private final int tickCount;
        private final List<String> flags;

        public EntityInfo(String uuid, String dimension, int tickCount, List<String> flags) {
            this.uuid = uuid;
            this.dimension = dimension;
            this.tickCount = tickCount;
            this.flags = flags;
        }

        public String getUuid() {
            return uuid;
        }

        public String getDimension() {
            return dimension;
        }

        public int getTickCount() {
            return tickCount;
        }

        public List<String> getFlags() {
            return flags;
        }
    }
}

