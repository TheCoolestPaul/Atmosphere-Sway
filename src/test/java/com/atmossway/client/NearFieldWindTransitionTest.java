package com.atmossway.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NearFieldWindTransitionTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void preservesThePreviousVectorOnTheCommitTick() {
        NearFieldWindTransition transition = new NearFieldWindTransition();
        var before = WindForceMath.fromComponents(0.8F, 0.2F);
        var after = WindForceMath.fromComponents(0.2F, -0.3F);

        transition.begin(100L, before, after);
        var displayed = transition.apply(100L, after, 2.0F);

        assertForceComponents(before, displayed);
        assertEquals(0.0F, transition.progress(), EPSILON);
        assertEquals(20, transition.remainingTicks());
    }

    @Test
    void usesSmoothstepAndReachesTheTargetAfterTwentyTicks() {
        NearFieldWindTransition transition = new NearFieldWindTransition();
        var before = WindForceMath.fromComponents(0.8F, 0.0F);
        var after = WindForceMath.fromComponents(0.2F, 0.0F);
        transition.begin(40L, before, after);

        var quarter = transition.apply(45L, after, 2.0F);
        assertEquals(0.70625F, quarter.forceX(), EPSILON);
        assertEquals(0.25F, transition.progress(), EPSILON);

        var halfway = transition.apply(50L, after, 2.0F);
        assertEquals(0.5F, halfway.forceX(), EPSILON);

        var complete = transition.apply(60L, after, 2.0F);
        assertForceComponents(after, complete);
        assertFalse(transition.active());
        assertEquals(0, transition.remainingTicks());
    }

    @Test
    void interpolatesDirectionReversalsThroughCancellation() {
        NearFieldWindTransition transition = new NearFieldWindTransition();
        var before = WindForceMath.fromComponents(0.5F, 0.0F);
        var after = WindForceMath.fromComponents(-0.5F, 0.0F);
        transition.begin(0L, before, after);

        assertFalse(transition.apply(10L, after, 2.0F).isPresent());
        assertForceComponents(after, transition.apply(20L, after, 2.0F));
    }

    @Test
    void retainsNewGustChangesWhileTheCommitCorrectionDecays() {
        NearFieldWindTransition transition = new NearFieldWindTransition();
        var before = WindForceMath.fromComponents(0.8F, 0.0F);
        var after = WindForceMath.fromComponents(0.2F, 0.0F);
        transition.begin(0L, before, after);

        var unchangedTarget = transition.apply(5L, after, 2.0F);
        var strongerGust = transition.apply(5L, WindForceMath.fromComponents(0.3F, 0.0F), 2.0F);

        assertEquals(0.1F, strongerGust.forceX() - unchangedTarget.forceX(), EPSILON);
    }

    @Test
    void capsTheDisplayedVectorAndHandlesCalm() {
        NearFieldWindTransition transition = new NearFieldWindTransition();
        var before = WindForceMath.fromComponents(2.0F, 0.0F);
        var after = WindForceMath.fromComponents(1.5F, 0.0F);
        transition.begin(0L, before, after);

        assertEquals(1.8F, transition.apply(0L, after, 1.8F).intensity(), EPSILON);

        transition.begin(30L, after, WindForceMath.WindForce.NONE);
        assertForceComponents(after, transition.apply(30L, WindForceMath.WindForce.NONE, 2.0F));
        assertFalse(transition.apply(50L, WindForceMath.WindForce.NONE, 2.0F).isPresent());
    }

    @Test
    void timeRollbackCancelsTheTransitionSafely() {
        NearFieldWindTransition transition = new NearFieldWindTransition();
        var before = WindForceMath.fromComponents(0.8F, 0.0F);
        var after = WindForceMath.fromComponents(0.2F, 0.0F);
        transition.begin(100L, before, after);

        assertForceComponents(after, transition.apply(99L, after, 2.0F));
        assertFalse(transition.active());
    }

    private static void assertForceComponents(WindForceMath.WindForce expected,
                                              WindForceMath.WindForce actual) {
        assertEquals(expected.forceX(), actual.forceX(), EPSILON);
        assertEquals(expected.forceZ(), actual.forceZ(), EPSILON);
    }
}
