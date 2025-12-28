package uk.creatopia.death_detangler.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import uk.creatopia.death_detangler.scan.CloneScanner;
import uk.creatopia.death_detangler.scan.ScanResult;
import uk.creatopia.death_detangler.report.ReportGenerator;

import java.io.IOException;
import java.util.Map;

/**
 * Command registration for Death Detangler.
 * Provides /death_detangler scan, run, dryrun, report, and dump commands.
 */
public class DeathDetanglerCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("death_detangler")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("scan")
                    .executes(ctx -> scan(ctx, false)))
                .then(Commands.literal("dryrun")
                    .executes(ctx -> scan(ctx, false)))
                .then(Commands.literal("run")
                    .executes(ctx -> scan(ctx, true)))
                .then(Commands.literal("report")
                    .executes(DeathDetanglerCommand::report))
                .then(Commands.literal("dump")
                    .executes(DeathDetanglerCommand::dump))
        );
    }

    private static int scan(CommandContext<CommandSourceStack> ctx, boolean applyFix) {
        ScanResult result = CloneScanner.scanWithDetails(ctx.getSource().getServer(), applyFix);
        int found = result.getTotalFound();

        if (applyFix) {
            ctx.getSource().sendSuccess(
                () -> Component.literal("Death Detangler removed " + found + " orphan clone(s)."),
                true
            );
        } else {
            ctx.getSource().sendSuccess(
                () -> Component.literal("Death Detangler detected " + found + " potential clone(s)."),
                false
            );
        }

        return found;
    }

    private static int report(CommandContext<CommandSourceStack> ctx) {
        ScanResult result = CloneScanner.scanWithDetails(ctx.getSource().getServer(), false);

        if (result.getTotalFound() == 0) {
            ctx.getSource().sendSuccess(
                () -> Component.literal("No orphan clones detected."),
                false
            );
            return 0;
        }

        // Build per-dimension report
        StringBuilder report = new StringBuilder("Death Detangler Report:\n");
        for (Map.Entry<ResourceKey<Level>, Integer> entry : result.getPerDimension().entrySet()) {
            report.append("  ").append(entry.getKey().location()).append(": ").append(entry.getValue()).append("\n");
        }
        report.append("Total: ").append(result.getTotalFound());

        ctx.getSource().sendSuccess(
            () -> Component.literal(report.toString()),
            false
        );

        return result.getTotalFound();
    }

    private static int dump(CommandContext<CommandSourceStack> ctx) {
        ScanResult result = CloneScanner.scanWithDetails(ctx.getSource().getServer(), false);

        try {
            java.nio.file.Path reportPath = ReportGenerator.generateReport(
                ctx.getSource().getServer(),
                result
            );

            ctx.getSource().sendSuccess(
                () -> Component.literal("Report saved to: " + reportPath.toString()),
                true
            );

            return result.getTotalFound();
        } catch (IOException e) {
            ctx.getSource().sendFailure(
                Component.literal("Failed to generate report: " + e.getMessage())
            );
            return 0;
        }
    }
}

