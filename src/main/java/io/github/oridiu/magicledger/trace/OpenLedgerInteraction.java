package io.github.oridiu.magicledger.trace;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.oridiu.magicledger.MagicLedgerPlugin;

import javax.annotation.Nonnull;
import java.util.List;

/** Right click with the Magic Ledger: opens the window listing what the chests nearby hold, or closes it again. */
public class OpenLedgerInteraction extends SimpleInstantInteraction {

    public static final String TYPE = "MagicLedgerOpen";

    public static final BuilderCodec<OpenLedgerInteraction> CODEC = BuilderCodec.builder(
                    OpenLedgerInteraction.class, OpenLedgerInteraction::new, SimpleInstantInteraction.CODEC)
            .documentation("Opens the Magic Ledger's list of the items in the chests nearby.")
            .build();

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) {
            return;
        }
        Ref<EntityStore> ref = context.getEntity();
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        TransformComponent transform = commandBuffer.getComponent(ref, TransformComponent.getComponentType());
        if (player == null || playerRef == null || transform == null) {
            return;
        }
        PageManager pageManager = player.getPageManager();
        if (pageManager.getCustomPage() instanceof LedgerPage) {
            // Using the book again closes it, also when the window is already gone on the client.
            pageManager.closePage(ref, ref.getStore());
            LedgerPage.stopReading(ref, commandBuffer);
            return;
        }
        if (pageManager.getCustomPage() != null) {
            return;
        }
        World world = commandBuffer.getExternalData().getWorld();
        List<NearbyChests.Chest> chests = NearbyChests.around(world, transform.getPosition(), MagicLedgerPlugin.getInstance().getSettings());
        pageManager.openCustomPage(ref, ref.getStore(), new LedgerPage(playerRef, chests));
        LedgerPage.startReading(ref, commandBuffer);
    }
}
