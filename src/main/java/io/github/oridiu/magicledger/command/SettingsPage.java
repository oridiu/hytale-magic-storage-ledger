package io.github.oridiu.magicledger.command;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.oridiu.magicledger.MagicLedgerPlugin;
import io.github.oridiu.magicledger.LedgerConfig;

import javax.annotation.Nonnull;

/** Settings window opened by {@code /magicledger settings}. */
public class SettingsPage extends InteractiveCustomUIPage<SettingsPage.Data> {

    public SettingsPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, Data.CODEC);
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store
    ) {
        LedgerConfig config = MagicLedgerPlugin.getInstance().getSettings();
        commandBuilder.append("Pages/MagicLedgerSettings.ui");
        commandBuilder.set("#HorizontalRadius.Value", config.getHorizontalRadius());
        commandBuilder.set("#VerticalRadius.Value", config.getVerticalRadius());
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SaveButton",
                new EventData()
                        .append(Data.HORIZONTAL_RADIUS, "#HorizontalRadius.Value")
                        .append(Data.VERTICAL_RADIUS, "#VerticalRadius.Value"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull Data data) {
        MagicLedgerPlugin.getInstance().updateSettings(config -> {
            config.setHorizontalRadius((int) Math.round(data.horizontalRadius));
            config.setVerticalRadius((int) Math.round(data.verticalRadius));
        });
        playerRef.sendMessage(Message.translation("server.magicledger.settings.saved").color("#b48cff"));
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            player.getPageManager().setPage(ref, store, Page.None);
        }
    }

    public static class Data {
        static final String HORIZONTAL_RADIUS = "@HorizontalRadius";
        static final String VERTICAL_RADIUS = "@VerticalRadius";

        public static final BuilderCodec<Data> CODEC = BuilderCodec.builder(Data.class, Data::new)
                .addField(new KeyedCodec<>(HORIZONTAL_RADIUS, Codec.DOUBLE), (data, value) -> data.horizontalRadius = value, data -> data.horizontalRadius)
                .addField(new KeyedCodec<>(VERTICAL_RADIUS, Codec.DOUBLE), (data, value) -> data.verticalRadius = value, data -> data.verticalRadius)
                .build();

        private double horizontalRadius;
        private double verticalRadius;
    }
}
