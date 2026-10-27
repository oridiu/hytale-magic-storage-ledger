package io.github.oridiu.magicledger.upgrade;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.oridiu.magicledger.MagicLedgerPlugin;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Using the Ledger Upgrade, like a backpack upgrade: the item is consumed and
 * from then on the player's ledger also shows how many of each item they carry.
 * Works once per player; a second upgrade is refused and kept.
 */
public class UpgradeLedgerInteraction extends SimpleInstantInteraction {

    public static final String TYPE = "MagicLedgerUpgrade";

    public static final BuilderCodec<UpgradeLedgerInteraction> CODEC = BuilderCodec.builder(
                    UpgradeLedgerInteraction.class, UpgradeLedgerInteraction::new, SimpleInstantInteraction.CODEC)
            .documentation("Unlocks the Magic Ledger's carried-items column for the player and consumes the item.")
            .build();

    @Nonnull
    @Override
    public WaitForDataFrom getWaitForDataFrom() {
        return WaitForDataFrom.Server;
    }

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) {
            return;
        }
        Ref<EntityStore> ref = context.getEntity();
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        MagicLedgerPlugin plugin = MagicLedgerPlugin.getInstance();
        boolean granted;
        try {
            granted = plugin.getUpgrades().grant(playerRef.getUuid());
        } catch (IOException e) {
            plugin.getLogger().at(Level.SEVERE).withCause(e).log("[MagicLedger] could not save the upgrade of %s", playerRef.getUuid());
            context.getState().state = InteractionState.Failed;
            playerRef.sendMessage(Message.translation("server.magicledger.upgrade.failed").color("#ff5555"));
            return;
        }
        if (!granted) {
            context.getState().state = InteractionState.Failed;
            playerRef.sendMessage(Message.translation("server.magicledger.upgrade.already").color("#ffaa55"));
            return;
        }
        context.getHeldItemContainer().removeItemStackFromSlot(context.getHeldItemSlot(), context.getHeldItem(), 1);
        playerRef.sendMessage(Message.translation("server.magicledger.upgrade.applied").color("#b48cff"));
    }
}
