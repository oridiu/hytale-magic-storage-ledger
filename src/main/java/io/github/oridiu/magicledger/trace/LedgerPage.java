package io.github.oridiu.magicledger.trace;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceBasePage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceElement;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceInteraction;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Magic Ledger's window: every item in the chests nearby with its total
 * count, most first, laid out like the repair kit's window. Clicking an
 * item marks the chests that hold it. With the Ledger Upgrade a further
 * column shows how many of each item the player carries.
 */
public class LedgerPage extends ChoiceBasePage {

    private static final String LAYOUT = "Pages/MagicLedgerPage.ui";
    private static final String ELEMENT = "Pages/MagicLedgerElement.ui";
    /** The book's animation set (Server/Item/Animations/MagicLedger.json) and its reading pose. */
    private static final String ANIMATIONS = "MagicLedger";
    private static final String READ_ANIMATION = "Read";
    private static final String CLOSE_ANIMATION = "Close";

    /** Whether the carried column is shown: only for players with the Ledger Upgrade. */
    private final boolean showCarried;

    /**
     * @param carried how many of each item the player carries, or null
     *                without the Ledger Upgrade
     */
    public LedgerPage(@Nonnull PlayerRef playerRef, @Nonnull List<NearbyChests.Chest> chests, @Nullable Map<String, Integer> carried) {
        super(playerRef, elementsFor(chests, carried), LAYOUT);
        this.showCarried = carried != null;
    }

    /** The player holds the open book as if reading while the window is up; everyone nearby sees it. */
    public static void startReading(@Nonnull Ref<EntityStore> ref, @Nonnull ComponentAccessor<EntityStore> accessor) {
        AnimationUtils.playAnimation(ref, AnimationSlot.Action, ANIMATIONS, READ_ANIMATION, true, accessor);
    }

    /**
     * Replaces the looping reading pose with a short closing one. Only stopping
     * the slot left the book open on the client until the next swing.
     */
    public static void stopReading(@Nonnull Ref<EntityStore> ref, @Nonnull ComponentAccessor<EntityStore> accessor) {
        if (ref.isValid()) {
            AnimationUtils.playAnimation(ref, AnimationSlot.Action, ANIMATIONS, CLOSE_ANIMATION, true, accessor);
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        super.onDismiss(ref, store);
        stopReading(ref, store);
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store
    ) {
        if (getElements() != null && getElements().length > 0) {
            super.build(ref, commandBuilder, eventBuilder, store);
            commandBuilder.set("#CarriedHeading.Visible", showCarried);
            return;
        }
        // Nothing to list: no column headings, just a short note in the middle.
        commandBuilder.append(LAYOUT);
        commandBuilder.set("#Header.Visible", false);
        commandBuilder.set("#ElementList.Visible", false);
        commandBuilder.set("#Empty.Visible", true);
    }

    @Nonnull
    private static ChoiceElement[] elementsFor(@Nonnull List<NearbyChests.Chest> chests, @Nullable Map<String, Integer> carried) {
        List<TraceRules.Stack> stacks = new ArrayList<>();
        for (NearbyChests.Chest chest : chests) {
            for (short slot = 0; slot < chest.container().getCapacity(); slot++) {
                ItemStack stack = chest.container().getItemStack(slot);
                if (!ItemStack.isEmpty(stack)) {
                    stacks.add(new TraceRules.Stack(stack.getItemId(), stack.getQuantity()));
                }
            }
        }
        List<ChoiceElement> elements = new ArrayList<>();
        for (Map.Entry<String, Integer> total : TraceRules.totals(stacks)) {
            // Starts reading the item's icon now, so its colour is ready by the click.
            ItemColors.of(total.getKey());
            Integer inHand = carried != null ? carried.getOrDefault(total.getKey(), 0) : null;
            elements.add(new Element(total.getKey(), total.getValue(), inHand));
        }
        return elements.toArray(ChoiceElement[]::new);
    }

    /**
     * One row: the item's icon, name, how many of it the chests hold together
     * and, with the upgrade, how many the player carries (blank for none).
     */
    private static final class Element extends ChoiceElement {

        private final String itemId;
        private final int count;
        @Nullable
        private final Integer carried;

        Element(@Nonnull String itemId, int count, @Nullable Integer carried) {
            this.itemId = itemId;
            this.count = count;
            this.carried = carried;
            this.interactions = new ChoiceInteraction[]{new TraceChoice(itemId)};
        }

        @Override
        public void addButton(@Nonnull UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, String selector, PlayerRef playerRef) {
            commandBuilder.append("#ElementList", ELEMENT);
            commandBuilder.set(selector + " #Icon.ItemId", itemId);
            commandBuilder.set(selector + " #Name.TextSpans", new ItemStack(itemId).getDisplayName());
            commandBuilder.set(selector + " #Count.Text", Integer.toString(count));
            if (carried != null) {
                commandBuilder.set(selector + " #Carried.Visible", true);
                commandBuilder.set(selector + " #Carried.Text", carried > 0 ? Integer.toString(carried) : "");
            }
        }
    }
}
