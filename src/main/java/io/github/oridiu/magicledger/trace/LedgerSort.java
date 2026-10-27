package io.github.oridiu.magicledger.trace;

import javax.annotation.Nonnull;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * How the ledger's rows are ordered: by one column, ascending or descending.
 * Rows that tie keep alphabetical order by name, so the list stays stable.
 */
public final class LedgerSort {

    public enum Column {
        NAME, COUNT, CARRIED;

        /** Numbers are most useful highest first; names from A. */
        public boolean ascendingFirst() {
            return this == NAME;
        }
    }

    /** One row of the ledger, reduced to what sorting needs. */
    public record Row(@Nonnull String itemId, @Nonnull String name, int count, int carried) {
    }

    private LedgerSort() {
    }

    /** The column and direction after clicking a heading: the same heading flips the direction, another one starts with its natural direction. */
    public static boolean ascendingAfterClick(@Nonnull Column current, boolean ascending, @Nonnull Column clicked) {
        return clicked == current ? !ascending : clicked.ascendingFirst();
    }

    @Nonnull
    public static List<Row> sorted(@Nonnull List<Row> rows, @Nonnull Column column, boolean ascending) {
        Collator collator = Collator.getInstance(Locale.ENGLISH);
        collator.setStrength(Collator.SECONDARY);
        Comparator<Row> byName = Comparator.comparing(Row::name, collator).thenComparing(Row::itemId);
        Comparator<Row> order = switch (column) {
            case NAME -> byName;
            case COUNT -> Comparator.comparingInt(Row::count);
            case CARRIED -> Comparator.comparingInt(Row::carried);
        };
        if (!ascending) {
            order = order.reversed();
        }
        List<Row> sorted = new ArrayList<>(rows);
        sorted.sort(column == Column.NAME ? order : order.thenComparing(byName));
        return sorted;
    }
}
