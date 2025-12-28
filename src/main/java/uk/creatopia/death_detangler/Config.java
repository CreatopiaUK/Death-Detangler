package uk.creatopia.death_detangler;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = DeathDetanglerMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ENABLE_LOG_NOTIFICATIONS = BUILDER
            .comment("Enable or disable log notifications from Death Detangler.", 
                     "When enabled, the mod will log information about detected revival clones and compatibility fixes.")
            .define("enableLogNotifications", true);

    private static final ForgeConfigSpec.BooleanValue AUTO_RUN_ON_START = BUILDER
            .comment("Automatically run cleanup when the server starts.",
                     "This will scan and remove orphan clones on server startup.")
            .define("autoRunOnStart", true);

    private static final ForgeConfigSpec.BooleanValue AUTO_REMOVE = BUILDER
            .comment("If true, periodic cleanup will automatically remove clones.",
                     "If false, periodic cleanup will only scan and log.")
            .define("autoRemove", true);

    private static final ForgeConfigSpec.BooleanValue VERBOSE_LOGGING = BUILDER
            .comment("Enable verbose logging for cleanup operations.",
                     "When enabled, detailed information about each cleanup operation will be logged.")
            .define("verboseLogging", false);

    private static final ForgeConfigSpec.IntValue CLEAN_INTERVAL_TICKS = BUILDER
            .comment("Interval in ticks between periodic cleanup runs.",
                     "Default: 6000 (5 minutes at 20 TPS). Set to 0 to disable periodic cleanup.")
            .defineInRange("cleanIntervalTicks", 6000, 0, Integer.MAX_VALUE);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enableLogNotifications;
    public static boolean autoRunOnStart;
    public static boolean autoRemove;
    public static boolean verboseLogging;
    public static int cleanIntervalTicks;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        enableLogNotifications = ENABLE_LOG_NOTIFICATIONS.get();
        autoRunOnStart = AUTO_RUN_ON_START.get();
        autoRemove = AUTO_REMOVE.get();
        verboseLogging = VERBOSE_LOGGING.get();
        cleanIntervalTicks = CLEAN_INTERVAL_TICKS.get();
    }
}

