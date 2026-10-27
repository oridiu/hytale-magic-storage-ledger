package io.github.oridiu.magicledger.trace;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceBasePage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceBasePage.ChoicePageEventData;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.ui.builder.EventData;
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
 * count, laid out like the repair kit's window, sorted by name from A. Clicking
 * a column heading sorts by that column, clicking it again flips the order.
 * Clicking an item marks the chests that hold it. With the Ledger Upgrade a
 * further column shows how many of each item the player carries.
 */
public class LedgerPage extends ChoiceBasePage {

    private static final String LAYOUT = "Pages/MagicLedgerPage.ui";
    private static final String ELEMENT = "Pages/MagicLedgerElement.ui";
    /** The book's animation set (Server/Item/Animations/MagicLedger.json) and its reading pose. */
    private static final String ANIMATIONS = "MagicLedger";
    private static final String READ_ANIMATION = "Read";
    private static final String CLOSE_ANIMATION = "Close";

    /** Headings send negative indices, rows their position from 0. */
    private static final String[] HEADINGS = {"#SortName", "#SortCount", "#SortCarried"};
    private static final String[] HEADING_KEYS = {"server.magicledger.ui.item", "server.magicledger.ui.count", "server.magicledger.ui.carried"};

    /** Whether the carried column is shown: only for players with the Ledger Upgrade. */
    private final boolean showCarried;
    private final List<LedgerSort.Row> rows;
    private List<LedgerSort.Row> shown;
    private LedgerSort.Column column = LedgerSort.Column.NAME;
    private boolean ascending = true;

    /**
     * @param carried how many of each item the player carries, or null
     *                without the Ledger Upgrade
     */
    public LedgerPage(@Nonnull PlayerRef playerRef, @Nonnull List<NearbyChests.Chest> chests, @Nullable Map<String, Integer> carried) {
        super(playerRef, null, LAYOUT);
        this.showCarried = carried != null;
        this.rows = rowsFor(chests, carried, playerRef.getLanguage());
        this.shown = LedgerSort.sorted(rows, column, ascending);
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
        commandBuilder.append(LAYOUT);
        if (shown.isEmpty()) {
            // Nothing to list: no column headings, just a short note in the middle.
            commandBuilder.set("#Header.Visible", false);
            commandBuilder.set("#ElementList.Visible", false);
            commandBuilder.set("#Empty.Visible", true);
            return;
        }
        commandBuilder.set("#SortCarried.Visible", showCarried);
        LedgerSort.Column[] columns = LedgerSort.Column.values();
        for (int i = 0; i < columns.length; i++) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, HEADINGS[i], EventData.of("Index", Integer.toString(-1 - i)), false);
        }
        commandBuilder.set(HEADINGS[column.ordinal()] + " #Label.TextSpans", headingText(column, ascending));
        commandBuilder.clear("#ElementList");
        for (int i = 0; i < shown.size(); i++) {
            String selector = "#ElementList[" + i + "]";
            addRow(commandBuilder, selector, shown.get(i));
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of("Index", Integer.toString(i)), false);
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull ChoicePageEventData data) {
        int index = data.getIndex();
        LedgerSort.Column[] columns = LedgerSort.Column.values();
        if (index < 0 && -1 - index < columns.length) {
            LedgerSort.Column clicked = columns[-1 - index];
            ascending = LedgerSort.ascendingAfterClick(column, ascending, clicked);
            column = clicked;
            shown = LedgerSort.sorted(rows, column, ascending);
            rebuild();
        } else if (index >= 0 && index < shown.size()) {
            new TraceChoice(shown.get(index).itemId()).run(store, ref, playerRef);
        }
    }

    /** The heading of the sorted column, e.g. "Item (A-Z)" or "In chests (9-1)". */
    @Nonnull
    private static Message headingText(@Nonnull LedgerSort.Column column, boolean ascending) {
        String key = column == LedgerSort.Column.NAME
                ? (ascending ? "server.magicledger.ui.sortAscending" : "server.magicledger.ui.sortDescending")
                : (ascending ? "server.magicledger.ui.sortLowFirst" : "server.magicledger.ui.sortHighFirst");
        return Message.translation(key).param("column", Message.translation(HEADING_KEYS[column.ordinal()]));
    }

    @Nonnull
    private static List<LedgerSort.Row> rowsFor(@Nonnull List<NearbyChests.Chest> chests, @Nullable Map<String, Integer> carried, @Nullable String language) {
        List<TraceRules.Stack> stacks = new ArrayList<>();
        for (NearbyChests.Chest chest : chests) {
            for (short slot = 0; slot < chest.container().getCapacity(); slot++) {
                ItemStack stack = chest.container().getItemStack(slot);
                if (!ItemStack.isEmpty(stack)) {
                    stacks.add(new TraceRules.Stack(stack.getItemId(), stack.getQuantity()));
                }
            }
        }
        List<LedgerSort.Row> rows = new ArrayList<>();
        for (Map.Entry<String, Integer> total : TraceRules.counts(stacks).entrySet()) {
            // Starts reading the item's icon now, so its colour is ready by the click.
            ItemColors.of(total.getKey());
            int inHand = carried != null ? carried.getOrDefault(total.getKey(), 0) : 0;
            rows.add(new LedgerSort.Row(total.getKey(), nameOf(total.getKey(), language), total.getValue(), inHand));
        }
        return rows;
    }

    /** The item's name in the player's language, for sorting; the item id if it has none. */
    @Nonnull
    private static String nameOf(@Nonnull String itemId, @Nullable String language) {
        Item item = new ItemStack(itemId).getItem();
        String key = item != null ? item.getTranslationKey() : null;
        I18nModule i18n = I18nModule.get();
        String name = key != null && i18n != null ? i18n.getMessage(i18n.resolveLanguage(language), key) : null;
        return name != null ? name : itemId;
    }

    /**
     * One row: the item's icon, name, how many of it the chests hold together
     * and, with the upgrade, how many the player carries (blank for none).
     */
    private void addRow(@Nonnull UICommandBuilder commandBuilder, @Nonnull String selector, @Nonnull LedgerSort.Row row) {
        commandBuilder.append("#ElementList", ELEMENT);
        commandBuilder.set(selector + " #Icon.ItemId", row.itemId());
        commandBuilder.set(selector + " #Name.TextSpans", new ItemStack(row.itemId()).getDisplayName());
        commandBuilder.set(selector + " #Count.Text", Integer.toString(row.count()));
        if (showCarried) {
            commandBuilder.set(selector + " #Carried.Visible", true);
            commandBuilder.set(selector + " #Carried.Text", row.carried() > 0 ? Integer.toString(row.carried()) : "");
        }
    }
}
