package uk.creatopia.death_detangler.report;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import uk.creatopia.death_detangler.scan.ScanResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates JSON reports of clone scan results.
 */
public class ReportGenerator {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Generates a JSON report file from a scan result.
     * 
     * @param server The Minecraft server instance
     * @param result The scan result to report
     * @return The path to the generated report file
     * @throws IOException If file writing fails
     */
    public static Path generateReport(MinecraftServer server, ScanResult result) throws IOException {
        Map<String, Object> report = new HashMap<>();
        report.put("timestamp", System.currentTimeMillis() / 1000);
        report.put("totalFound", result.getTotalFound());

        // Per-dimension counts
        Map<String, Integer> dimensions = new HashMap<>();
        for (Map.Entry<ResourceKey<Level>, Integer> entry : result.getPerDimension().entrySet()) {
            dimensions.put(entry.getKey().location().toString(), entry.getValue());
        }
        report.put("dimensions", dimensions);

        // Entity details
        report.put("entities", result.getEntities());

        // Write to file
        Path reportPath = server.getWorldPath(LevelResource.ROOT)
                .resolve("death_detangler_report.json");

        Files.writeString(reportPath, GSON.toJson(report), StandardCharsets.UTF_8);

        return reportPath;
    }
}

