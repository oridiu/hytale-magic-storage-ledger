package io.github.oridiu.magicledger.trace;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceInteraction;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.oridiu.magicledger.MagicLedgerPlugin;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Clicking an item in the ledger's window: closes the window, every nearby
 * chest that holds the item is wrapped in glittering mist in its colour for
 * a few seconds, so it stands out even among stacked chests, and wisps show
 * the way there. Only the searching player sees them. The chests are looked
 * up again on the click, so items moved meanwhile are found where they are
 * now.
 */
public class TraceChoice extends ChoiceInteraction {

    private static final String TRACE_SYSTEM = "Magic_Ledger_Mark";
    private static final float DURATION_SECONDS = 10.0F;

    private final String itemId;

    public TraceChoice(@Nonnull String itemId) {
        this.itemId = itemId;
    }

    @Override
    public void run(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            player.getPageManager().setPage(ref, store, Page.None);
        }
        LedgerPage.stopReading(ref, store);
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }
        World world = store.getExternalData().getWorld();
        List<NearbyChests.Chest> chests = NearbyChests.around(world, transform.getPosition(), MagicLedgerPlugin.getInstance().getSettings())
                .stream().filter(chest -> chest.holds(itemId)).toList();
        if (chests.isEmpty()) {
            playerRef.sendMessage(Message.translation("server.magicledger.trace.none").color("#ffaa55"));
            return;
        }
        Color color = ItemColors.of(itemId);
        for (NearbyChests.Chest chest : chests) {
            for (Vector3d cell : chest.cells()) {
                ParticleUtil.spawnParticleEffect(
                        TRACE_SYSTEM, cell.x, cell.y, cell.z, 0.0F, 0.0F, 0.0F, 1.0F, color, null, List.of(ref), store, DURATION_SECONDS);
            }
        }
        WispSystem.start(world.getName(), ref, transform.getPosition(),
                chests.stream().map(NearbyChests.Chest::centre).toList(), color);
        playerRef.sendMessage(Message.translation("server.magicledger.trace.found").param("count", chests.size()).color("#b48cff"));
    }
}
