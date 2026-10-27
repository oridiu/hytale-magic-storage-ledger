package io.github.oridiu.magicledger.upgrade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LedgerUpgradesTest {

    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000000a");

    @Test
    void parseSkipsBlanksAndComments() {
        assertEquals(Set.of(ALICE), LedgerUpgrades.parse(List.of("# players", "", "  " + ALICE + "  ")));
    }

    @Test
    void grantWorksOnceAndSurvivesARestart(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("upgrades.txt");
        LedgerUpgrades upgrades = new LedgerUpgrades(file);
        upgrades.load();
        assertFalse(upgrades.has(ALICE));
        assertTrue(upgrades.grant(ALICE));
        assertFalse(upgrades.grant(ALICE));

        LedgerUpgrades reloaded = new LedgerUpgrades(file);
        reloaded.load();
        assertTrue(reloaded.has(ALICE));
    }

    @Test
    void neverOverwritesAFileItCouldNotRead(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("upgrades.txt");
        Files.writeString(file, "not a uuid\n");
        LedgerUpgrades upgrades = new LedgerUpgrades(file);
        assertThrows(IllegalArgumentException.class, upgrades::load);
        assertThrows(IOException.class, () -> upgrades.grant(ALICE));
        assertEquals("not a uuid\n", Files.readString(file));
    }
}
