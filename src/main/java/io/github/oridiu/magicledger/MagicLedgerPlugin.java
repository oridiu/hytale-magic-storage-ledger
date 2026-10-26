package io.github.oridiu.magicledger;

import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import io.github.oridiu.magicledger.command.LedgerCommand;
import io.github.oridiu.magicledger.trace.OpenLedgerInteraction;
import io.github.oridiu.magicledger.trace.WispSystem;

import javax.annotation.Nonnull;
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

    /** Changes settings in game: applies them right away and writes config.json. */
    public synchronized void updateSettings(@Nonnull Consumer<LedgerConfig> change) {
        change.accept(getSettings());
        config.save();
    }

    @Override
    protected void setup() {
        getCodecRegistry(Interaction.CODEC).register(OpenLedgerInteraction.TYPE, OpenLedgerInteraction.class, OpenLedgerInteraction.CODEC);
        getEntityStoreRegistry().registerSystem(new WispSystem());
        getCommandRegistry().registerCommand(new LedgerCommand());
    }

    @Override
    protected void start() {
        // Write the defaults once so server admins can find and edit the file.
        if (!Files.exists(getDataDirectory().resolve("config.json"))) {
            config.save();
        }
        getLogger().at(Level.INFO).log("[MagicLedger] started");
    }
}
