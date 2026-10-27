package io.github.oridiu.magicledger;

import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import io.github.oridiu.magicledger.command.LedgerCommand;
import io.github.oridiu.magicledger.trace.OpenLedgerInteraction;
import io.github.oridiu.magicledger.trace.WispSystem;
import io.github.oridiu.magicledger.upgrade.LedgerUpgrades;
import io.github.oridiu.magicledger.upgrade.UpgradeCraftingSystem;
import io.github.oridiu.magicledger.upgrade.UpgradeLedgerInteraction;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Entry point of the mod: the Magic Ledger lists what the chests nearby hold
 * and marks the chests that hold the item you pick.
 */
public class MagicLedgerPlugin extends JavaPlugin {

    private static MagicLedgerPlugin instance;

    private final Config<LedgerConfig> config = withConfig(LedgerConfig.CODEC);
    private LedgerUpgrades upgrades;

    public MagicLedgerPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static MagicLedgerPlugin getInstance() {
        return instance;
    }

    public LedgerConfig getSettings() {
        return config.get();
    }

    /** Players who used the Ledger Upgrade. */
    public LedgerUpgrades getUpgrades() {
        return upgrades;
    }

    /** Changes settings in game: applies them right away and writes config.json. */
    public synchronized void updateSettings(@Nonnull Consumer<LedgerConfig> change) {
        change.accept(getSettings());
        config.save();
    }

    @Override
    protected void setup() {
        getCodecRegistry(Interaction.CODEC).register(OpenLedgerInteraction.TYPE, OpenLedgerInteraction.class, OpenLedgerInteraction.CODEC);
        getCodecRegistry(Interaction.CODEC).register(UpgradeLedgerInteraction.TYPE, UpgradeLedgerInteraction.class, UpgradeLedgerInteraction.CODEC);
        getEntityStoreRegistry().registerSystem(new WispSystem());
        getEntityStoreRegistry().registerSystem(new UpgradeCraftingSystem());
        getCommandRegistry().registerCommand(new LedgerCommand());
    }

    @Override
    protected void start() {
        // Write the defaults once so server admins can find and edit the file.
        if (!Files.exists(getDataDirectory().resolve("config.json"))) {
            config.save();
        }
        upgrades = new LedgerUpgrades(getDataDirectory().resolve("upgrades.txt"));
        try {
            upgrades.load();
        } catch (IOException | IllegalArgumentException e) {
            // Left unloaded: nobody gets new upgrades until the file is fixed, so it is never overwritten.
            getLogger().at(Level.SEVERE).withCause(e).log("[MagicLedger] could not read upgrades.txt");
        }
        getLogger().at(Level.INFO).log("[MagicLedger] started");
    }
}
