package io.github.oridiu.magicledger.trace;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shows the way to traced chests: a wisp of mist leaves the player and drifts
 * to each chest, again every few seconds from wherever the player stands
 * then. The server moves it by spawning a short puff a little further along
 * every tenth of a second; only the searching player sees it.
 */
public class WispSystem extends TickingSystem<EntityStore> {

    private static final String WISP_SYSTEM = "Magic_Ledger_Wisp";
    private static final float STEP_SECONDS = 0.1F;
    private static final double SPEED = 3.5;
    private static final float LAUNCH_EVERY_SECONDS = 3.0F;
    private static final float TRACE_SECONDS = 12.0F;
    /** Wisps start a bit away from the player, so the first puffs do not cover the view. */
    private static final double START_DISTANCE = 1.2;
    /** Puffs float at about chest height of the player. */
    private static final double EYE_OFFSET = 1.0;
    private static final int MAX_CHESTS = 5;

    private static final Map<String, List<Trace>> TRACES = new ConcurrentHashMap<>();

    private static final class Trace {
        final Ref<EntityStore> player;
        final List<Vector3d> targets;
        final Color color;
        float age;
        float sinceStep;
        float sinceLaunch = LAUNCH_EVERY_SECONDS;
        final List<Wisp> wisps = new ArrayList<>();

        Trace(Ref<EntityStore> player, List<Vector3d> targets, Color color) {
            this.player = player;
            this.targets = targets;
            this.color = color;
        }
    }

    private static final class Wisp {
        final Vector3d from;
        final Vector3d direction;
        final double length;
        double travelled = START_DISTANCE;

        Wisp(Vector3d from, Vector3d to) {
            this.from = from;
            Vector3d delta = new Vector3d(to).sub(from);
            this.length = delta.length();
            this.direction = length > 0 ? delta.div(length) : delta;
        }
    }

    /**
     * Starts wisps from this player to the nearest few of these chests,
     * replacing the player's previous trace.
     */
    public static void start(@Nonnull String world, @Nonnull Ref<EntityStore> player, @Nonnull Vector3d playerPos,
                             @Nonnull List<Vector3d> chests, @Nonnull Color color) {
        List<Vector3d> nearest = new ArrayList<>(chests);
        nearest.sort((a, b) -> Double.compare(a.distanceSquared(playerPos), b.distanceSquared(playerPos)));
        List<Trace> traces = TRACES.computeIfAbsent(world, w -> new ArrayList<>());
        traces.removeIf(trace -> trace.player.equals(player));
        traces.add(new Trace(player, List.copyOf(nearest.subList(0, Math.min(MAX_CHESTS, nearest.size()))), color));
    }

    @Override
    public void tick(float dt, int systemIndex, @Nonnull Store<EntityStore> store) {
        List<Trace> traces = TRACES.get(store.getExternalData().getWorld().getName());
        if (traces == null || traces.isEmpty()) {
            return;
        }
        for (Iterator<Trace> it = traces.iterator(); it.hasNext(); ) {
            Trace trace = it.next();
            if (!trace.player.isValid() || (trace.age >= TRACE_SECONDS && trace.wisps.isEmpty())) {
                it.remove();
                continue;
            }
            trace.age += dt;
            trace.sinceLaunch += dt;
            trace.sinceStep += dt;
            if (trace.sinceLaunch >= LAUNCH_EVERY_SECONDS && trace.age < TRACE_SECONDS) {
                trace.sinceLaunch = 0.0F;
                launch(store, trace);
            }
            if (trace.sinceStep >= STEP_SECONDS) {
                step(store, trace, trace.sinceStep);
                trace.sinceStep = 0.0F;
            }
        }
    }

    private static void launch(@Nonnull Store<EntityStore> store, @Nonnull Trace trace) {
        TransformComponent transform = store.getComponent(trace.player, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }
        Vector3d from = new Vector3d(transform.getPosition()).add(0.0, EYE_OFFSET, 0.0);
        for (Vector3d target : trace.targets) {
            Wisp wisp = new Wisp(from, target);
            if (wisp.length > START_DISTANCE) {
                trace.wisps.add(wisp);
            }
        }
    }

    private static void step(@Nonnull Store<EntityStore> store, @Nonnull Trace trace, float seconds) {
        List<Ref<EntityStore>> viewer = List.of(trace.player);
        for (Iterator<Wisp> it = trace.wisps.iterator(); it.hasNext(); ) {
            Wisp wisp = it.next();
            wisp.travelled += SPEED * seconds;
            if (wisp.travelled >= wisp.length) {
                it.remove();
                continue;
            }
            Vector3d at = new Vector3d(wisp.direction).mul(wisp.travelled).add(wisp.from);
            ParticleUtil.spawnParticleEffect(
                    WISP_SYSTEM, at.x, at.y, at.z, 0.0F, 0.0F, 0.0F, 1.0F, trace.color, null, viewer, store, 1.0F);
        }
    }
}
