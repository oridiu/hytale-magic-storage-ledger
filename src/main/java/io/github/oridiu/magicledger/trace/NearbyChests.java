package io.github.oridiu.magicledger.trace;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.gameplay.CraftingConfig;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import io.github.oridiu.magicledger.LedgerConfig;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The chests around a player, found the same way workbenches find the chests
 * they take materials from: through the server's index of item containers,
 * within a cylinder around the player.
 */
public final class NearbyChests {

    /** A chest and the centres of the blocks it fills (two for a large chest), where its glitter shows. */
    public record Chest(@Nonnull ItemContainer container, @Nonnull List<Vector3d> cells) {

        /** The middle of all its blocks: between the halves of a large chest. */
        @Nonnull
        public Vector3d centre() {
            Vector3d centre = new Vector3d();
            for (Vector3d cell : cells) {
                centre.add(cell);
            }
            return centre.div(Math.max(1, cells.size()));
        }

        public boolean holds(@Nonnull String itemId) {
            for (short slot = 0; slot < container.getCapacity(); slot++) {
                ItemStack stack = container.getItemStack(slot);
                if (!ItemStack.isEmpty(stack) && itemId.equals(stack.getItemId())) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Upper bound of blocks marked per chest, for unusually large modded containers. */
    private static final int MAX_CELLS = 8;

    private NearbyChests() {
    }

    /** All chests in range of this position, at most as many as a workbench would use. */
    @Nonnull
    public static List<Chest> around(@Nonnull World world, @Nonnull Vector3d centre, @Nonnull LedgerConfig config) {
        CraftingConfig crafting = world.getGameplayConfig().getCraftingConfig();
        int horizontal = config.getHorizontalRadius() > 0 ? config.getHorizontalRadius() : crafting.getBenchMaterialHorizontalChestSearchRadius();
        int vertical = config.getVerticalRadius() > 0 ? config.getVerticalRadius() : crafting.getBenchMaterialVerticalChestSearchRadius();
        int limit = crafting.getBenchMaterialChestLimit();

        Store<ChunkStore> store = world.getChunkStore().getStore();
        SpatialResource<Ref<ChunkStore>, ChunkStore> index = store.getResource(BlockModule.get().getItemContainerSpatialResourceType());
        List<Ref<ChunkStore>> refs = new ArrayList<>();
        index.getSpatialStructure().collectCylinder(centre, horizontal, vertical * 2.0, refs);

        List<Chest> chests = new ArrayList<>();
        for (Ref<ChunkStore> ref : refs) {
            if (chests.size() >= limit) {
                break;
            }
            if (!ref.isValid()) {
                continue;
            }
            ItemContainerBlock block = store.getComponent(ref, ItemContainerBlock.getComponentType());
            Vector3d pos = block != null && block.getItemContainer() != null ? positionOf(store, ref) : null;
            if (pos != null) {
                chests.add(new Chest(block.getItemContainer(), cellsOf(world.getChunkStore(), pos)));
            }
        }
        return chests;
    }

    /**
     * Centres of the blocks the chest at this position fills, taken from its
     * rotated hitbox, so a large chest is marked over both halves.
     */
    @Nonnull
    private static List<Vector3d> cellsOf(@Nonnull ChunkStore chunkStore, @Nonnull Vector3d pos) {
        int x = (int) pos.x;
        int y = (int) pos.y;
        int z = (int) pos.z;
        Box box = null;
        Ref<ChunkStore> sectionRef = chunkStore.getChunkSectionReferenceAtBlock(x, y, z);
        BlockSection section = sectionRef != null && sectionRef.isValid()
                ? chunkStore.getStore().getComponent(sectionRef, BlockSection.getComponentType()) : null;
        if (section != null) {
            BlockType blockType = BlockType.getAssetMap().getAsset(section.get(x, y, z));
            BlockBoundingBoxes hitbox = blockType != null ? BlockBoundingBoxes.getAssetMap().getAsset(blockType.getHitboxTypeIndex()) : null;
            if (hitbox != null) {
                box = hitbox.get(section.getRotationIndex(x, y, z)).getBoundingBox();
            }
        }
        List<Vector3d> cells = new ArrayList<>();
        List<int[]> offsets = box != null
                ? TraceRules.cells(box.min.x, box.min.y, box.min.z, box.max.x, box.max.y, box.max.z, MAX_CELLS)
                : List.<int[]>of(new int[]{0, 0, 0});
        for (int[] offset : offsets) {
            cells.add(new Vector3d(x + offset[0] + 0.5, y + offset[1] + 0.5, z + offset[2] + 0.5));
        }
        return cells;
    }

    /** World position of a block entity, as the server's own container index computes it. */
    @Nullable
    private static Vector3d positionOf(@Nonnull Store<ChunkStore> store, @Nonnull Ref<ChunkStore> blockRef) {
        BlockModule.BlockStateInfo info = store.getComponent(blockRef, BlockModule.BlockStateInfo.getComponentType());
        if (info == null || !info.getSectionRef().isValid()) {
            return null;
        }
        ChunkSection section = store.getComponent(info.getSectionRef(), ChunkSection.getComponentType());
        if (section == null) {
            return null;
        }
        return new Vector3d(
                section.getX() << 5 | ChunkUtil.xFromIndex(info.getIndex()),
                section.getY() << 5 | ChunkUtil.yFromIndex(info.getIndex()),
                section.getZ() << 5 | ChunkUtil.zFromIndex(info.getIndex()));
    }
}
