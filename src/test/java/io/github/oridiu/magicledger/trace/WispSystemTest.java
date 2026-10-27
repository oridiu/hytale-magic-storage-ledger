package io.github.oridiu.magicledger.trace;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WispSystemTest {

    @Test
    void curlsBetweenItsEndpointsAndTapersOnArrival() {
        WispSystem.Wisp wisp = new WispSystem.Wisp(new Vector3d(0, 1, 0), new Vector3d(10, 1, 0));
        assertEquals(1.2, wisp.position().x, 0.000001);
        float departureScale = wisp.scale();

        wisp.travelled = 5.6;
        Vector3d middle = wisp.position();
        assertTrue(middle.y > 1.0);
        assertTrue(Math.abs(middle.z) > 0.01);
        assertTrue(middle.distance(new Vector3d(5.6, 1, 0)) < 0.21);
        assertTrue(wisp.scale() > departureScale);

        wisp.travelled = wisp.length;
        assertTrue(wisp.position().distance(new Vector3d(10, 1, 0)) < 0.000001);
        assertEquals(departureScale, wisp.scale(), 0.000001);
    }

    @Test
    void verticalAndCoincidentTargetsStayFinite() {
        for (Vector3d target : new Vector3d[]{new Vector3d(0, 10, 0), new Vector3d()}) {
            WispSystem.Wisp wisp = new WispSystem.Wisp(new Vector3d(), target);
            wisp.travelled = wisp.length;
            assertTrue(wisp.position().isFinite());
            assertTrue(wisp.position().distance(target) < 0.000001);
        }
    }

    @Test
    void lateWavesMustArriveAndDissolveBeforeTheChestMarkEnds() {
        WispSystem.Wisp wisp = new WispSystem.Wisp(new Vector3d(), new Vector3d(12, 0, 0));
        assertTrue(wisp.canArrive(4.0F));
        assertFalse(wisp.canArrive(3.0F));
        assertFalse(new WispSystem.Wisp(new Vector3d(), new Vector3d(1, 0, 0)).canArrive(10.0F));
    }

    @Test
    void extendedSearchRangesStillGetAFirstWisp() {
        WispSystem.Wisp wisp = new WispSystem.Wisp(new Vector3d(), new Vector3d(64, 64, 0));
        assertTrue(wisp.canArrive(WispSystem.TRACE_SECONDS));
        assertEquals(4.5, (wisp.length - wisp.travelled) / wisp.speed, 0.000001);
    }
}
