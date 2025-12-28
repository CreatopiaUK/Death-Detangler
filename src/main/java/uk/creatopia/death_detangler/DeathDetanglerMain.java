package uk.creatopia.death_detangler;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import uk.creatopia.death_detangler.command.DeathDetanglerCommand;
import uk.creatopia.death_detangler.scan.CloneScanner;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(DeathDetanglerMain.MODID)
public class DeathDetanglerMain {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "death_detangler";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    // Periodic cleanup counter
    private static int tickCounter = 0;

    public DeathDetanglerMain() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Death Detangler mod loaded");
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Death Detangler server initialized");
    }

    /**
     * Auto-run cleanup on server start if configured.
     */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        if (!Config.autoRunOnStart) {
            return;
        }

        LOGGER.info("[DeathDetangler] cleanup_start");
        int cleaned = CloneScanner.scanAndFix(event.getServer(), true);
        LOGGER.info("[DeathDetangler] removed={}", cleaned);
        LOGGER.info("[DeathDetangler] cleanup_end");

        if (cleaned > 0) {
            LOGGER.info("Auto-clean complete: removed {} orphan clone(s).", cleaned);
        }
    }

    /**
     * Periodic cleanup timer.
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Config.cleanIntervalTicks <= 0) return;

        tickCounter++;

        if (tickCounter >= Config.cleanIntervalTicks) {
            tickCounter = 0;

            LOGGER.info("[DeathDetangler] cleanup_start");
            int removed = CloneScanner.scanAndFix(
                event.getServer(),
                Config.autoRemove
            );
            LOGGER.info("[DeathDetangler] removed={}", removed);
            LOGGER.info("[DeathDetangler] cleanup_end");

            if (Config.verboseLogging && removed > 0) {
                LOGGER.info("Periodic cleanup removed {} orphan clones.", removed);
            }
        }
    }

    /**
     * Registers the /death_detangler command.
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        DeathDetanglerCommand.register(event.getDispatcher());
    }
}

