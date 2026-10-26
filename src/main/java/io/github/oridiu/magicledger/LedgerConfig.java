package io.github.oridiu.magicledger;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Settings of the mod, stored in {@code mods/oridiu_MagicStorageLedger/config.json}
 * on the server. Operators can also change them in game with
 * {@code /magicledger settings}.
 */
public class LedgerConfig {

    public static final int MAX_RADIUS = 64;

    public static final BuilderCodec<LedgerConfig> CODEC = BuilderCodec.builder(LedgerConfig.class, LedgerConfig::new)
            .append(new KeyedCodec<>("HorizontalRadius", Codec.INTEGER),
                    (config, value) -> config.horizontalRadius = value,
                    config -> config.horizontalRadius)
            .documentation("How far the Magic Ledger searches sideways, in blocks. 0 uses the range in which workbenches find chests (14 by default).")
            .add()
            .append(new KeyedCodec<>("VerticalRadius", Codec.INTEGER),
                    (config, value) -> config.verticalRadius = value,
                    config -> config.verticalRadius)
            .documentation("How far the Magic Ledger searches up and down, in blocks. 0 uses the range in which workbenches find chests (6 by default).")
            .add()
            .build();

    private int horizontalRadius;
    private int verticalRadius;

    /** In blocks; 0 means the workbench range. */
    public int getHorizontalRadius() {
        return Math.clamp(horizontalRadius, 0, MAX_RADIUS);
    }

    public void setHorizontalRadius(int horizontalRadius) {
        this.horizontalRadius = Math.clamp(horizontalRadius, 0, MAX_RADIUS);
    }

    /** In blocks; 0 means the workbench range. */
    public int getVerticalRadius() {
        return Math.clamp(verticalRadius, 0, MAX_RADIUS);
    }

    public void setVerticalRadius(int verticalRadius) {
        this.verticalRadius = Math.clamp(verticalRadius, 0, MAX_RADIUS);
    }
}
