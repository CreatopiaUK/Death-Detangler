package uk.creatopia.death_detangler.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to intercept Hardcore Revival's knockout method and use totems instead.
 * When a player would be knocked out and has a totem, the totem is consumed
 * and the player is immediately revived, skipping the knockout entirely.
 */
@Mixin(targets = "net.blay09.mods.hardcorerevival.HardcoreRevivalManager")
public abstract class HardcoreRevivalTotemMixin {

    @Inject(
        method = "knockout(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/damagesource/DamageSource;)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void deathDetangler$useTotemInstead(
            ServerPlayer player,
            DamageSource source,
            CallbackInfo ci
    ) {
        if (!hasTotem(player)) {
            return;
        }

        consumeTotem(player);

        // Wake player immediately using HR logic
        try {
            Class<?> managerClass = Class.forName("net.blay09.mods.hardcorerevival.HardcoreRevivalManager");
            java.lang.reflect.Method wakeupMethod = managerClass.getMethod("wakeup", ServerPlayer.class);
            wakeupMethod.invoke(null, player);
        } catch (Exception e) {
            // If wakeup() is not available, try reset() as fallback
            try {
                Class<?> managerClass = Class.forName("net.blay09.mods.hardcorerevival.PlayerHardcoreRevivalManager");
                java.lang.reflect.Method resetMethod = managerClass.getMethod("reset", ServerPlayer.class);
                resetMethod.invoke(null, player);
            } catch (Exception e2) {
                // Log error but don't crash
                com.mojang.logging.LogUtils.getLogger().error("Failed to wake up player after totem usage", e2);
            }
        }

        // Vanilla totem animation
        if (player.level() != null) {
            player.level().broadcastEntityEvent(player, (byte) 35);
        }

        // Prevent knockout entirely
        ci.cancel();
    }

    private static boolean hasTotem(ServerPlayer player) {
        if (player == null) return false;
        
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                return true;
            }
        }
        return false;
    }

    private static void consumeTotem(ServerPlayer player) {
        if (player == null) return;
        
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                stack.shrink(1);
                return;
            }
        }
    }
}

