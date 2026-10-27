package io.github.oridiu.magicledger.upgrade;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.CraftRecipeEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.oridiu.magicledger.MagicLedgerPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The Ledger Upgrade can be bought only once: crafting it is refused when the
 * player already has the upgrade or still carries an unused one.
 */
public class UpgradeCraftingSystem extends EntityEventSystem<EntityStore, CraftRecipeEvent.Pre> {

    public static final String UPGRADE_ID = "Magic_Ledger_Upgrade";

    public UpgradeCraftingSystem() {
        super(CraftRecipeEvent.Pre.class);
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull CraftRecipeEvent.Pre event
    ) {
        MaterialQuantity output = event.getCraftedRecipe().getPrimaryOutput();
        if (output == null || !UPGRADE_ID.equals(output.getItemId())) {
            return;
        }
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        String reason = null;
        if (MagicLedgerPlugin.getInstance().getUpgrades().has(playerRef.getUuid())) {
            reason = "server.magicledger.upgrade.already";
        } else if (event.getQuantity() > 1 || carriesUpgrade(InventoryComponent.getCombined(commandBuffer, ref, InventoryComponent.EVERYTHING))) {
            reason = "server.magicledger.upgrade.onlyOne";
        }
        if (reason != null) {
            event.setCancelled(true);
            playerRef.sendMessage(Message.translation(reason).color("#ffaa55"));
        }
    }

    private static boolean carriesUpgrade(@Nonnull CombinedItemContainer inventory) {
        for (short slot = 0; slot < inventory.getCapacity(); slot++) {
            ItemStack stack = inventory.getItemStack(slot);
            if (!ItemStack.isEmpty(stack) && UPGRADE_ID.equals(stack.getItemId())) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }
}
