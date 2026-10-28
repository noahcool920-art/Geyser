package org.geysermc.geyser.util;

import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

public final class OreSnitch {

    private static final DateTimeFormatter TS =
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final ConcurrentHashMap<String, BufferedWriter> WRITERS =
        new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> LAST_FLUSH =
        new ConcurrentHashMap<>();
    private static final long FLUSH_INTERVAL_MS = 2000L;

    private OreSnitch() { }

    public static void logOre(GeyserSession session,
                              String dimension,
                              int x, int y, int z,
                              String javaPath) {
        if (session == null || javaPath == null) return;
        if (!(javaPath.endsWith("_ore") || javaPath.equals("ancient_debris"))) return;

        String who = session.bedrockUsername();
        if (who == null || who.isBlank()) who = "unknown";

        String line = x + "," + y + "," + z + ","
                    + javaPath + "," + dimension + "\n";

        try {
            BufferedWriter w = WRITERS.computeIfAbsent(who, OreSnitch::open);
            if (w == null) return;
            synchronized (w) {
                w.write(line);
                long now = System.currentTimeMillis();
                if (now - LAST_FLUSH.getOrDefault(who, 0L) > FLUSH_INTERVAL_MS) {
                    w.flush();
                    LAST_FLUSH.put(who, now);
                }
            }
        } catch (IOException ignored) { }
    }

    private static BufferedWriter open(String who) {
        try {
            Path dir = Paths.get("orelog");
            Files.createDirectories(dir);
            Path f = dir.resolve("ores-" + who + "-"
                + LocalDateTime.now().format(TS) + ".csv");
            BufferedWriter w = new BufferedWriter(new FileWriter(f.toFile(), true));
            w.write("x,y,z,block,dim\n");
            w.flush();
            GeyserImpl.getInstance().getLogger()
                .info("[OreSnitch] logging to " + f.toAbsolutePath());
            return w;
        } catch (IOException e) {
            GeyserImpl.getInstance().getLogger()
                .error("[OreSnitch] could not open log", e);
            return null;
        }
    }
}
