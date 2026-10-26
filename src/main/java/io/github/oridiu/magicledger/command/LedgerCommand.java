package io.github.oridiu.magicledger.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.oridiu.magicledger.MagicLedgerPlugin;
import io.github.oridiu.magicledger.LedgerConfig;

import javax.annotation.Nonnull;

/**
 * {@code /magicledger} (or {@code /ml}): shows and changes the search range in
 * game. Like every plugin command it needs the plugin's command permission,
 * which operators have.
 */
public class LedgerCommand extends AbstractCommandCollection {

    public LedgerCommand() {
        super("magicledger", "server.magicledger.commands.desc");
        addAliases("ml");
        addSubCommand(new SettingsCommand());
        addSubCommand(new ShowCommand());
    }

    /** Opens the settings window. */
    static class SettingsCommand extends AbstractPlayerCommand {

        SettingsCommand() {
            super("settings", "server.magicledger.commands.settings.desc");
            addAliases("ui");
        }

        @Override
        protected void execute(
                @Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef, @Nonnull World world
        ) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                player.getPageManager().openCustomPage(ref, store, new SettingsPage(playerRef));
            }
        }
    }

    static class ShowCommand extends CommandBase {

        ShowCommand() {
            super("show", "server.magicledger.commands.show.desc");
            addAliases("list");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            LedgerConfig config = MagicLedgerPlugin.getInstance().getSettings();
            context.sendMessage(Message.translation("server.magicledger.settings.show")
                    .param("horizontal", config.getHorizontalRadius())
                    .param("vertical", config.getVerticalRadius())
                    .color("#b48cff"));
        }
    }
}
