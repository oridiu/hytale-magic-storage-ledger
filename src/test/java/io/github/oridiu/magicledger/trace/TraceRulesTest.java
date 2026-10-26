package io.github.oridiu.magicledger.trace;

import io.github.oridiu.magicledger.trace.TraceRules.Stack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceRulesTest {

    @Test
    void noStacksNoItems() {
        assertTrue(TraceRules.totals(List.of()).isEmpty());
    }

    @Test
    void sumsAllStacksOfAnItemMostFirst() {
        List<Map.Entry<String, Integer>> totals = TraceRules.totals(List.of(
                new Stack("Ore_Copper", 50),
                new Stack("Wood_Oak", 80),
                new Stack("Ore_Copper", 50)));
        assertEquals(List.of(Map.entry("Ore_Copper", 100), Map.entry("Wood_Oak", 80)), totals);
    }

    @Test
    void equalCountsKeepTheOrderFound() {
        List<Map.Entry<String, Integer>> totals = TraceRules.totals(List.of(new Stack("Wood_Oak", 10), new Stack("Ore_Copper", 10)));
        assertEquals("Wood_Oak", totals.getFirst().getKey());
    }

    @Test
    void smallChestCoversItsOwnBlock() {
        List<int[]> cells = TraceRules.cells(0.06, 0.0, 0.06, 0.94, 0.9, 0.94, 8);
        assertEquals(1, cells.size());
        assertArrayEquals(new int[]{0, 0, 0}, cells.getFirst());
    }

    @Test
    void largeChestCoversBothHalves() {
        List<int[]> cells = TraceRules.cells(-0.94, 0.0, 0.06, 0.94, 0.9, 0.94, 8);
        assertEquals(2, cells.size());
        assertArrayEquals(new int[]{-1, 0, 0}, cells.get(0));
        assertArrayEquals(new int[]{0, 0, 0}, cells.get(1));
    }

    @Test
    void saturatedColourBeatsMoreGreyPixels() {
        int red = 0xFFD02020;
        int grey = 0xFF606060;
        int[] icon = {red, red, red, grey, grey, grey, grey, grey, 0x00000000};
        assertEquals(0xD02020, TraceRules.mainColor(icon));
    }

    @Test
    void transparentIconHasNoColour() {
        assertEquals(-1, TraceRules.mainColor(new int[]{0x00FFFFFF, 0x10FF0000}));
    }

    @Test
    void darkColoursAreLiftedKeepingTheirHue() {
        assertEquals(0xC86400, TraceRules.brighten(0x643200));
        assertEquals(0xFFA04A, TraceRules.brighten(0xFFA04A));
    }
}
