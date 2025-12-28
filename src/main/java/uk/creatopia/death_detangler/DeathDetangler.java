package uk.creatopia.death_detangler;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Death Detangler - Fixes compatibility issues between Hardcore Revival and other death-related mods.
 *
 * High level:
 *  - Detect "revival clone" entities from Hardcore Revival.
 *  - Mark them so other mods can know they're not real deaths.
 *  - Block death handling on clones so corpse/grave mods don't trigger.
 *  - Track clones per player and clean them up on respawn/logout/server stop.
 */
@Mod.EventBusSubscriber(modid = "death_detangler")
public class DeathDetangler {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_NAME = "Death Detangler";

    // Helper method to check if logging is enabled
    private static boolean shouldLog() {
        return Config.enableLogNotifications;
    }

    // Persistent data keys
    private static final String KEY_CLONE_MARK = "DeathDetanglerClone";
    private static final String KEY_SPAWN_TIME = "DeathDetanglerSpawnTime";
    private static final String KEY_OWNER = "DeathDetanglerOwner";
    private static final String KEY_IN_REVIVAL = "DeathDetanglerInRevival";
    private static final String KEY_IS_REVIVAL_CLONE = "IsRevivalClone";

    // Track active revival clones by player UUID
    private static final Map<UUID, Entity> activeClones = new ConcurrentHashMap<>();

    // Track players currently in a revival state
    private static final Set<UUID> playersInRevival = ConcurrentHashMap.newKeySet();

    // ---------------------------------------------------------------------
    // 1) Detect Hardcore Revival clones when they join the world
    // ---------------------------------------------------------------------
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;

        Entity entity = event.getEntity();

        try {
            if (isHardcoreRevivalClone(entity)) {
                if (shouldLog()) {
                    LOGGER.info("[{}] Detected Hardcore Revival clone: {} at {}",
                            MOD_NAME, entity.getUUID(), entity.position());
                }

                CompoundTag data = entity.getPersistentData();
                data.putBoolean(KEY_CLONE_MARK, true);
                data.putLong(KEY_SPAWN_TIME, event.getLevel().getGameTime());
                data.putBoolean(KEY_IS_REVIVAL_CLONE, true);

                UUID ownerUUID = getCloneOwnerUUID(entity);
                if (ownerUUID != null) {
                    data.putUUID(KEY_OWNER, ownerUUID);
                    activeClones.put(ownerUUID, entity);
                    playersInRevival.add(ownerUUID);

                    if (shouldLog()) {
                        LOGGER.info("[{}] Registered clone for player UUID: {}", MOD_NAME, ownerUUID);
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("[{}] Exception while checking entity for HC Revival clone", MOD_NAME, ex);
        }
    }

    // ---------------------------------------------------------------------
    // 2) Block death events for marked clones (prevents Corpse/gravestones)
    //    Note: Totem handling is now done via Mixin (TotemBeforeKnockoutMixin)
    // ---------------------------------------------------------------------
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        Entity entity = event.getEntity();

        try {
            CompoundTag data = entity.getPersistentData();
            if (data.getBoolean(KEY_CLONE_MARK)
                    || data.getBoolean(KEY_IS_REVIVAL_CLONE)
                    || entity.getTags().contains("revival_in_progress")) {

                if (shouldLog()) {
                    LOGGER.debug("[{}] Blocking death event for HC Revival clone: {}", MOD_NAME, entity.getUUID());
                }
                event.setCanceled(true);
                return;
            }

            // If it's the real player and they're flagged "in revival", we let Hardcore Revival handle it.
            if (entity instanceof Player player && playersInRevival.contains(player.getUUID())) {
                if (shouldLog()) {
                    LOGGER.debug("[{}] Player {} in revival state; letting Hardcore Revival handle death.",
                            MOD_NAME, player.getUUID());
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("[{}] Exception in onLivingDeath", MOD_NAME, ex);
        }
    }

    // ---------------------------------------------------------------------
    // 3) Mark clones on capability attach (so Curios & co. can detect them)
    // ---------------------------------------------------------------------
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();

        try {
            CompoundTag data = entity.getPersistentData();
            if (data.getBoolean(KEY_CLONE_MARK) || data.getBoolean(KEY_IS_REVIVAL_CLONE)) {
                if (shouldLog()) {
                    LOGGER.debug("[{}] Marking entity {} as revival clone for capabilities", MOD_NAME, entity.getUUID());
                }
                data.putBoolean(KEY_IS_REVIVAL_CLONE, true);

                // Optional: add a tiny marker capability if other mods are updated to read it.
                // event.addCapability(
                //     new ResourceLocation("death_detangler", "revival_marker"),
                //     new DummyCapabilityProvider()
                // );
            }
        } catch (Exception ex) {
            LOGGER.warn("[{}] Exception in onAttachCapabilities", MOD_NAME, ex);
        }
    }

    // ---------------------------------------------------------------------
    // 4) Clean up when the player is cloned (respawn/rewrap)
    // ---------------------------------------------------------------------
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player newPlayer = event.getEntity();
        UUID playerUUID = newPlayer.getUUID();

        if (shouldLog()) {
            LOGGER.info("[{}] Player clone event - UUID: {}, wasDeath: {}",
                    MOD_NAME, playerUUID, event.isWasDeath());
        }

        try {
            Entity clone = activeClones.remove(playerUUID);
            if (clone != null && !clone.isRemoved()) {
                if (shouldLog()) {
                    LOGGER.info("[{}] Discarding HC Revival clone for player: {}", MOD_NAME, playerUUID);
                }
                clone.discard();
            }
        } catch (Exception ex) {
            LOGGER.warn("[{}] Exception removing clone on player clone event", MOD_NAME, ex);
        } finally {
            playersInRevival.remove(playerUUID);
            newPlayer.getPersistentData().remove(KEY_IN_REVIVAL);
            newPlayer.getPersistentData().remove(KEY_IS_REVIVAL_CLONE);
        }
    }

    // ---------------------------------------------------------------------
    // 5) Clean up if player logs out mid-revival
    // ---------------------------------------------------------------------
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerUUID = event.getEntity().getUUID();
        try {
            Entity clone = activeClones.remove(playerUUID);
            if (clone != null && !clone.isRemoved()) {
                if (shouldLog()) {
                    LOGGER.info("[{}] Discarding HC Revival clone for player {} on logout", MOD_NAME, playerUUID);
                }
                clone.discard();
            }
        } catch (Exception ex) {
            LOGGER.warn("[{}] Exception removing clone on logout", MOD_NAME, ex);
        } finally {
            playersInRevival.remove(playerUUID);
        }
    }

    // ---------------------------------------------------------------------
    // 6) Periodic cleanup (every ~5 seconds)
    // ---------------------------------------------------------------------
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        long tick = server.getTickCount();
        if (tick % 100L != 0L) return; // every 100 ticks ~5s

        Iterator<Map.Entry<UUID, Entity>> it = activeClones.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Entity> entry = it.next();
            UUID playerUUID = entry.getKey();
            Entity clone = entry.getValue();

            try {
                if (clone == null || clone.isRemoved()) {
                    it.remove();
                    continue;
                }

                ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
                boolean stillInRevival = player != null
                        && player.getPersistentData().getBoolean(KEY_IN_REVIVAL);

                if (player == null || !stillInRevival) {
                    if (shouldLog()) {
                        LOGGER.info("[{}] Discarding orphaned HC Revival clone for player: {}", MOD_NAME, playerUUID);
                    }
                    clone.discard();
                    it.remove();
                    playersInRevival.remove(playerUUID);
                }
            } catch (Exception ex) {
                LOGGER.warn("[{}] Exception during periodic clone cleanup for {}", MOD_NAME, playerUUID, ex);
                it.remove();
                playersInRevival.remove(playerUUID);
            }
        }
    }

    // ---------------------------------------------------------------------
    // 7) Server stopping = wipe tracking maps
    // ---------------------------------------------------------------------
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        if (shouldLog()) {
            LOGGER.info("[{}] Server stopping, clearing clone tracking maps.", MOD_NAME);
        }
        activeClones.clear();
        playersInRevival.clear();
    }

    // ---------------------------------------------------------------------
    // Helper: decide whether an entity is a Hardcore Revival "downed" clone
    // ---------------------------------------------------------------------
    private static boolean isHardcoreRevivalClone(Entity entity) {
        if (entity == null) return false;

        try {
            CompoundTag data = entity.getPersistentData();
            if (data != null) {
                if (data.contains("HardcoreRevival")
                        || data.contains("is_downed")
                        || data.contains("isRevivalClone")
                        || data.contains("downed")
                        || data.contains("revive_clone")) {
                    return true;
                }
            }

            ResourceLocation entityTypeKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (entityTypeKey != null) {
                String reg = entityTypeKey.toString().toLowerCase();
                if (reg.contains("reviv") || reg.contains("hardcorerevival")) {
                    return true;
                }
            }

            if (entity instanceof Player player) {
                if (player.getPersistentData().contains(KEY_OWNER)
                        || player.getPersistentData().getBoolean(KEY_IS_REVIVAL_CLONE)) {
                    return true;
                }
            }
        } catch (Throwable t) {
            if (shouldLog()) {
                LOGGER.debug("[{}] Error while checking isHardcoreRevivalClone", MOD_NAME, t);
            }
        }

        return false;
    }

    // ---------------------------------------------------------------------
    // Helper: get the real player UUID that this clone belongs to
    // ---------------------------------------------------------------------
    private static UUID getCloneOwnerUUID(Entity entity) {
        if (entity == null) return null;

        try {
            CompoundTag data = entity.getPersistentData();
            if (data.hasUUID("OwnerUUID")) return data.getUUID("OwnerUUID");
            if (data.hasUUID("PlayerUUID")) return data.getUUID("PlayerUUID");
            if (data.hasUUID(KEY_OWNER)) return data.getUUID(KEY_OWNER);

            if (entity instanceof Player p) {
                return p.getUUID();
            }
        } catch (Throwable t) {
            if (shouldLog()) {
                LOGGER.debug("[{}] Error extracting clone owner UUID", MOD_NAME, t);
            }
        }

        return null;
    }

    // ---------------------------------------------------------------------
    // Helper: check if a player is currently knocked out by Hardcore Revival
    // ---------------------------------------------------------------------
    private static boolean isPlayerKnockedOut(ServerPlayer player) {
        if (player == null) return false;

        try {
            // Check persistent data for Hardcore Revival knockout markers
            CompoundTag data = player.getPersistentData();
            if (data.contains("HardcoreRevival")
                    || data.contains("is_downed")
                    || data.contains("isKnockedOut")
                    || data.contains("knocked_out")
                    || data.getBoolean("HardcoreRevivalKnockedOut")) {
                return true;
            }

            // Check if player has the "knocked_out" tag
            if (player.getTags().contains("knocked_out") 
                    || player.getTags().contains("hardcorerevival_knocked_out")) {
                return true;
            }

            // Check if player is in revival state (tracked by us)
            if (playersInRevival.contains(player.getUUID())) {
                return true;
            }
        } catch (Throwable t) {
            if (shouldLog()) {
                LOGGER.debug("[{}] Error checking if player is knocked out", MOD_NAME, t);
            }
        }

        return false;
    }

    // Optional marker capability provider (not strictly needed yet)
    private static class DummyCapabilityProvider implements ICapabilityProvider {
        @Override
        public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
                net.minecraftforge.common.capabilities.Capability<T> cap,
                net.minecraft.core.Direction side
        ) {
            return net.minecraftforge.common.util.LazyOptional.empty();
        }
    }
}

