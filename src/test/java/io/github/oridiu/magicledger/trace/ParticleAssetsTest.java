package io.github.oridiu.magicledger.trace;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.server.core.asset.type.particle.config.ParticleAnimationFrame;
import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleAssetsTest {

    @Test
    void everyParticleInheritsTheRuntimeItemColourAndFadesOut() throws Exception {
        for (String name : List.of("Mist", "Glitter", "Wisp", "Wisp_Glitter")) {
            try (InputStream stream = getClass().getResourceAsStream(
                    "/Server/Particles/MagicLedger/Spawners/Magic_Ledger_" + name + ".particlespawner")) {
                assertNotNull(stream, name);
                BsonDocument json = BsonDocument.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                BsonDocument particle = json.getDocument("Particle");
                ParticleAnimationFrame initial = frame(particle.getDocument("InitialAnimationFrame"));
                assertNull(initial.toPacket().color, name + " initial tint");
                BsonDocument animation = particle.getDocument("Animation");
                for (var frame : animation.values()) {
                    assertNull(frame(frame.asDocument()).toPacket().color, name + " animation tint");
                }
                assertEquals(0.0F, frame(animation.getDocument("0")).getOpacity(), name + " fade in");
                assertEquals(0.0F, frame(animation.getDocument("100")).getOpacity(), name + " fade out");
                BsonDocument life = json.getDocument("ParticleLifeSpan");
                assertTrue(life.getNumber("Min").doubleValue() <= life.getNumber("Max").doubleValue(), name);
                if (json.containsKey("LifeSpan")) {
                    assertTrue(json.getNumber("LifeSpan").doubleValue() + life.getNumber("Max").doubleValue() <= WispSystem.TRACE_SECONDS, name);
                }
            }
        }
    }

    private static ParticleAnimationFrame frame(BsonDocument json) {
        return ParticleAnimationFrame.CODEC.decode(json, new ExtraInfo());
    }
}
