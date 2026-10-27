package io.github.oridiu.magicledger.upgrade;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which players have used the Ledger Upgrade. Kept per player, not per book,
 * so a new ledger keeps the upgrade. Stored as one UUID per line in
 * {@code mods/oridiu_MagicStorageLedger/upgrades.txt}.
 */
public final class LedgerUpgrades {

    private final Path file;
    private final Set<UUID> players = ConcurrentHashMap.newKeySet();
    private volatile boolean loaded;

    public LedgerUpgrades(@Nonnull Path file) {
        this.file = file;
    }

    /** Reads the file. A missing file means nobody has the upgrade yet; an unreadable one is an error, so it is never overwritten. */
    public void load() throws IOException {
        if (Files.exists(file)) {
            players.addAll(parse(Files.readAllLines(file, StandardCharsets.UTF_8)));
        }
        loaded = true;
    }

    public boolean has(@Nonnull UUID player) {
        return players.contains(player);
    }

    /** Grants the upgrade and writes the file right away. Returns false if the player already had it. */
    public synchronized boolean grant(@Nonnull UUID player) throws IOException {
        if (!loaded) {
            throw new IOException(file + " could not be read; not overwriting it");
        }
        if (!players.add(player)) {
            return false;
        }
        try {
            save();
        } catch (IOException e) {
            players.remove(player);
            throw e;
        }
        return true;
    }

    private void save() throws IOException {
        List<String> lines = new ArrayList<>();
        for (UUID player : players) {
            lines.add(player.toString());
        }
        lines.sort(null);
        Files.createDirectories(file.getParent());
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.write(temp, lines, StandardCharsets.UTF_8);
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /** The UUIDs in these lines; blank lines and {@code #} comments are skipped. */
    @Nonnull
    static Set<UUID> parse(@Nonnull List<String> lines) {
        Set<UUID> parsed = new HashSet<>();
        for (String line : lines) {
            String trimmed = line.strip();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                parsed.add(UUID.fromString(trimmed));
            }
        }
        return parsed;
    }
}
