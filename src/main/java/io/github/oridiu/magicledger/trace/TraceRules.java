package io.github.oridiu.magicledger.trace;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The pure rules of the Magic Ledger: which items the nearby chests hold, and in which colour to mark them. */
public final class TraceRules {

    /** A stack in a chest, reduced to what the rules need. */
    public record Stack(@Nonnull String itemId, int quantity) {
    }

    /** Colours darker than this (brightest channel) are lifted so the sparks stay visible. */
    static final int MIN_BRIGHTNESS = 200;

    private TraceRules() {
    }

    /**
     * How many of each item the stacks hold together, most first. Equal counts
     * keep the order in which the items were first found, so the list stays
     * stable between searches.
     */
    @Nonnull
    public static List<Map.Entry<String, Integer>> totals(@Nonnull List<Stack> stacks) {
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts(stacks).entrySet());
        sorted.sort(Comparator.comparing(Map.Entry<String, Integer>::getValue).reversed());
        return sorted;
    }

    /** How many of each item the stacks hold together, in the order the items were first found. */
    @Nonnull
    public static Map<String, Integer> counts(@Nonnull List<Stack> stacks) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Stack stack : stacks) {
            if (stack.quantity() > 0) {
                counts.merge(stack.itemId(), stack.quantity(), Integer::sum);
            }
        }
        return counts;
    }

    /**
     * The block cells a hitbox (relative to its main block) covers, as offsets
     * from that block; a large chest covers two. At least the main block, at
     * most {@code limit} cells.
     */
    @Nonnull
    public static List<int[]> cells(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int limit) {
        List<int[]> cells = new ArrayList<>();
        for (int x = (int) Math.floor(minX); x < Math.max(Math.ceil(maxX), Math.floor(minX) + 1); x++) {
            for (int y = (int) Math.floor(minY); y < Math.max(Math.ceil(maxY), Math.floor(minY) + 1); y++) {
                for (int z = (int) Math.floor(minZ); z < Math.max(Math.ceil(maxZ), Math.floor(minZ) + 1); z++) {
                    if (cells.size() < limit) {
                        cells.add(new int[]{x, y, z});
                    }
                }
            }
        }
        return cells;
    }

    /**
     * The main colour of an icon (ARGB pixels) as 0xRRGGBB, or -1 if it has no
     * visible pixels. Colours are grouped coarsely; a group counts more the
     * more saturated it is, so a red apple wins over its brown stem and over
     * grey outlines.
     */
    public static int mainColor(@Nonnull int[] argbPixels) {
        Map<Integer, long[]> groups = new LinkedHashMap<>();
        for (int argb : argbPixels) {
            if ((argb >>> 24) < 128) {
                continue;
            }
            int r = argb >> 16 & 0xFF;
            int g = argb >> 8 & 0xFF;
            int b = argb & 0xFF;
            int key = (r >> 5) << 6 | (g >> 5) << 3 | (b >> 5);
            long[] sum = groups.computeIfAbsent(key, k -> new long[5]);
            int max = Math.max(r, Math.max(g, b));
            int min = Math.min(r, Math.min(g, b));
            // Weight 1 for grey, up to 5 for fully saturated.
            int weight = 1 + (max == 0 ? 0 : 4 * (max - min) / max);
            sum[0] += (long) r * weight;
            sum[1] += (long) g * weight;
            sum[2] += (long) b * weight;
            sum[3] += weight;
            sum[4]++;
        }
        long[] best = null;
        for (long[] sum : groups.values()) {
            if (best == null || sum[3] > best[3]) {
                best = sum;
            }
        }
        if (best == null) {
            return -1;
        }
        int r = (int) (best[0] / best[3]);
        int g = (int) (best[1] / best[3]);
        int b = (int) (best[2] / best[3]);
        return r << 16 | g << 8 | b;
    }

    /** Scales a 0xRRGGBB colour up so its brightest channel reaches {@link #MIN_BRIGHTNESS}; hue stays. */
    public static int brighten(int rgb) {
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        if (max >= MIN_BRIGHTNESS) {
            return rgb;
        }
        if (max == 0) {
            return MIN_BRIGHTNESS << 16 | MIN_BRIGHTNESS << 8 | MIN_BRIGHTNESS;
        }
        r = r * MIN_BRIGHTNESS / max;
        g = g * MIN_BRIGHTNESS / max;
        b = b * MIN_BRIGHTNESS / max;
        return r << 16 | g << 8 | b;
    }
}
