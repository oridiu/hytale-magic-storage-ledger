package io.github.oridiu.magicledger.trace;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.oridiu.magicledger.MagicLedgerPlugin;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Right click with the Magic Ledger: opens the window listing what the chests
 * nearby hold, or closes it again. With the Ledger Upgrade the window also
 * shows what the player carries.
 */
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
        Map<String, Integer> carried = MagicLedgerPlugin.getInstance().getUpgrades().has(playerRef.getUuid())
                ? carried(InventoryComponent.getCombined(commandBuffer, ref, InventoryComponent.HOTBAR_STORAGE_BACKPACK))
                : null;
        pageManager.openCustomPage(ref, ref.getStore(), new LedgerPage(playerRef, chests, carried));
        LedgerPage.startReading(ref, commandBuffer);
    }

    /** How many of each item the player carries in hotbar, inventory and backpack. */
    @Nonnull
    private static Map<String, Integer> carried(@Nonnull ItemContainer inventory) {
        List<TraceRules.Stack> stacks = new ArrayList<>();
        for (short slot = 0; slot < inventory.getCapacity(); slot++) {
            ItemStack stack = inventory.getItemStack(slot);
            if (!ItemStack.isEmpty(stack)) {
                stacks.add(new TraceRules.Stack(stack.getItemId(), stack.getQuantity()));
            }
        }
        return TraceRules.counts(stacks);
    }
}
