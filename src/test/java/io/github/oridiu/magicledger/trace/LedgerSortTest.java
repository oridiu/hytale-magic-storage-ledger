package io.github.oridiu.magicledger.trace;

import io.github.oridiu.magicledger.trace.LedgerSort.Column;
import io.github.oridiu.magicledger.trace.LedgerSort.Row;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LedgerSortTest {

    private static final Row COPPER = new Row("Ore_Copper", "Copper Ore", 40, 0);
    private static final Row APPLE = new Row("Plant_Fruit_Apple", "apple", 5, 3);
    private static final Row OAK = new Row("Wood_Oak", "Oak Log", 40, 12);
    private static final List<Row> ROWS = List.of(COPPER, APPLE, OAK);

    @Test
    void namesSortAlphabeticallyIgnoringCase() {
        assertEquals(List.of(APPLE, COPPER, OAK), LedgerSort.sorted(ROWS, Column.NAME, true));
        assertEquals(List.of(OAK, COPPER, APPLE), LedgerSort.sorted(ROWS, Column.NAME, false));
    }

    @Test
    void equalCountsStayAlphabetical() {
        assertEquals(List.of(COPPER, OAK, APPLE), LedgerSort.sorted(ROWS, Column.COUNT, false));
        assertEquals(List.of(APPLE, COPPER, OAK), LedgerSort.sorted(ROWS, Column.COUNT, true));
    }

    @Test
    void carriedSortsByCarriedCount() {
        assertEquals(List.of(OAK, APPLE, COPPER), LedgerSort.sorted(ROWS, Column.CARRIED, false));
    }

    @Test
    void clickingFlipsTheSameColumnAndStartsOthersNaturally() {
        assertFalse(LedgerSort.ascendingAfterClick(Column.NAME, true, Column.NAME));
        assertTrue(LedgerSort.ascendingAfterClick(Column.NAME, false, Column.NAME));
        assertFalse(LedgerSort.ascendingAfterClick(Column.NAME, true, Column.COUNT));
        assertTrue(LedgerSort.ascendingAfterClick(Column.COUNT, false, Column.NAME));
    }
}
