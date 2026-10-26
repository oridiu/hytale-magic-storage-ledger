package io.github.oridiu.magicledger.trace;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.asset.common.CommonAsset;
import com.hypixel.hytale.server.core.asset.common.CommonAssetRegistry;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * The main colour of an item, for the sparks that mark the chests holding
 * it. Blocks carry a precomputed texture colour; for everything else the
 * item's icon is read once in the background, and until then the sparks
 * are the ledger's violet.
 */
public final class ItemColors {

    /** The ledger's own colour (void crystal violet). */
    public static final int DEFAULT_RGB = 0xA070FF;

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Map<String, Color> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> LOADING = new ConcurrentHashMap<>();

    private ItemColors() {
    }

    @Nonnull
    public static Color of(@Nonnull String itemId) {
        Color cached = CACHE.get(itemId);
        if (cached != null) {
            return cached;
        }
        Item item = Item.getAssetMap().getAsset(itemId);
        if (item == null) {
            return toColor(DEFAULT_RGB);
        }
        Color blockColor = blockColor(item);
        if (blockColor != null) {
            Color color = toColor(TraceRules.brighten(rgbOf(blockColor)));
            remember(itemId, color, "block");
            return color;
        }
        loadIconColor(itemId, item);
        return toColor(DEFAULT_RGB);
    }

    @Nullable
    private static Color blockColor(@Nonnull Item item) {
        if (!item.hasBlockType()) {
            return null;
        }
        BlockType blockType = BlockType.getAssetMap().getAsset(item.getBlockId());
        if (blockType == null) {
            return null;
        }
        return blockType.getTextureComputedColor() != null ? blockType.getTextureComputedColor() : blockType.getParticleColor();
    }

    private static void loadIconColor(@Nonnull String itemId, @Nonnull Item item) {
        String icon = item.getIcon();
        CommonAsset asset = icon != null ? CommonAssetRegistry.getByName(icon) : null;
        if (asset == null) {
            remember(itemId, toColor(DEFAULT_RGB), "no icon");
            return;
        }
        if (LOADING.putIfAbsent(itemId, Boolean.TRUE) != null) {
            return;
        }
        asset.getBlob().whenComplete((bytes, error) -> {
            int rgb = error == null && bytes != null ? iconColor(bytes) : -1;
            remember(itemId, toColor(rgb >= 0 ? TraceRules.brighten(rgb) : DEFAULT_RGB), rgb >= 0 ? "icon" : "unreadable icon");
            LOADING.remove(itemId);
        });
    }

    private static void remember(@Nonnull String itemId, @Nonnull Color color, @Nonnull String source) {
        CACHE.put(itemId, color);
        LOGGER.at(Level.FINE).log("[MagicLedger] colour of %s: #%06X (%s)", itemId, rgbOf(color), source);
    }

    private static int iconColor(@Nonnull byte[] png) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
            if (image == null) {
                return -1;
            }
            int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
            return TraceRules.mainColor(pixels);
        } catch (Exception e) {
            return -1;
        }
    }

    private static int rgbOf(@Nonnull Color color) {
        return (color.red & 0xFF) << 16 | (color.green & 0xFF) << 8 | (color.blue & 0xFF);
    }

    @Nonnull
    private static Color toColor(int rgb) {
        return new Color((byte) (rgb >> 16), (byte) (rgb >> 8), (byte) rgb);
    }
}
