package com.atmossway.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FastGustFilterTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void smoothsVectorComponentsAndBlendsHalfTheChange() {
        FastGustFilter filter = new FastGustFilter();
        filter.initialize(WindForceMath.WindForce.NONE);

        filter.update(new WindForceMath.WindForce(1.0F, 0.0F, 1.0F), true);
        FastGustFilter.Snapshot snapshot = filter.snapshot(WindForceMath.WindForce.NONE, 1.5F);

        assertEquals(0.10F, snapshot.smoothed().forceX(), EPSILON);
        assertEquals(0.05F, snapshot.adjustment().forceX(), EPSILON);
        assertEquals(0.05F, snapshot.nearby().forceX(), EPSILON);
    }

    @Test
    void reachesAboutEightyPercentAfterFifteenTicks() {
        FastGustFilter filter = new FastGustFilter();
        filter.initialize(WindForceMath.WindForce.NONE);
        var target = new WindForceMath.WindForce(1.0F, 0.0F, 1.0F);

        for (int tick = 0; tick < 15; tick++) {
            filter.update(target, true);
        }

        assertEquals(0.7941F, filter.snapshot(WindForceMath.WindForce.NONE, 1.5F)
                .smoothed().intensity(), 0.001F);
    }

    @Test
    void clampsModerateResponseToPointOne() {
        FastGustFilter filter = new FastGustFilter();
        filter.initialize(new WindForceMath.WindForce(1.0F, 0.0F, 1.0F));

        FastGustFilter.Snapshot snapshot = filter.snapshot(WindForceMath.WindForce.NONE, 1.5F);

        assertEquals(FastGustFilter.MAX_ADJUSTMENT, snapshot.adjustment().intensity(), EPSILON);
        assertEquals(FastGustFilter.MAX_ADJUSTMENT, snapshot.nearby().intensity(), EPSILON);
    }

    @Test
    void weakeningWindReducesCommittedForce() {
        FastGustFilter filter = new FastGustFilter();
        var committed = new WindForceMath.WindForce(1.0F, 0.0F, 0.20F);
        filter.initialize(committed);

        filter.update(WindForceMath.WindForce.NONE, true);
        FastGustFilter.Snapshot snapshot = filter.snapshot(committed, 1.5F);

        assertTrue(snapshot.adjustment().x() < 0.0F);
        assertEquals(0.19F, snapshot.nearby().intensity(), EPSILON);
    }

    @Test
    void componentSmoothingHandlesDirectionWraparound() {
        FastGustFilter filter = new FastGustFilter();
        var beforeWrap = WindForceMath.fromAtmosphere(1.0F, 359.0F, 1.0D, 2.0D);
        var afterWrap = WindForceMath.fromAtmosphere(1.0F, 1.0F, 1.0D, 2.0D);
        filter.initialize(beforeWrap);

        filter.update(afterWrap, true);
        WindForceMath.WindForce smoothed = filter.snapshot(beforeWrap, 2.0F).smoothed();

        assertTrue(smoothed.z() > 0.99F);
        assertTrue(Math.abs(smoothed.x()) < 0.02F);
    }

    @Test
    void invalidSamplesHoldTheLastSmoothedValue() {
        FastGustFilter filter = new FastGustFilter();
        var initial = new WindForceMath.WindForce(0.0F, 1.0F, 0.4F);
        filter.initialize(initial);

        filter.update(new WindForceMath.WindForce(1.0F, 0.0F, 1.0F), false);

        assertEquals(initial, filter.snapshot(initial, 1.5F).smoothed());
    }

    @Test
    void capsFinalNearbyWindAndResetsToInactive() {
        FastGustFilter filter = new FastGustFilter();
        filter.initialize(new WindForceMath.WindForce(1.0F, 0.0F, 2.0F));
        var committed = new WindForceMath.WindForce(1.0F, 0.0F, 0.95F);

        assertEquals(1.0F, filter.snapshot(committed, 1.0F).nearby().intensity(), EPSILON);

        filter.reset();
        FastGustFilter.Snapshot reset = filter.snapshot(WindForceMath.WindForce.NONE, 1.0F);
        assertFalse(reset.smoothed().isPresent());
        assertFalse(reset.nearby().isPresent());
    }
}
