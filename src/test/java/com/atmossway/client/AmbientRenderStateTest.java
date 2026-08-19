package com.atmossway.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmbientRenderStateTest {
    @AfterEach
    void resetState() {
        AmbientRenderState.reset();
    }

    @Test
    void unselectedSectionsUseTheStaticPublishedWind() {
        var wind = new WindForceMath.WindForce(1.0F, 0.0F, 0.4F);
        AmbientRenderState.publishWind(wind);

        var snapshot = AmbientRenderState.captureForSection(10L);

        assertEquals(wind, snapshot.wind());
        assertFalse(snapshot.animated());
    }

    @Test
    void selectedSectionCapturesItsOwnPose() {
        long section = 234L;
        AmbientRenderState.publishWind(new WindForceMath.WindForce(0.0F, 1.0F, 0.5F));
        AmbientRenderState.targetAnimation(section, 77L);

        var snapshot = AmbientRenderState.captureForSection(section);

        assertTrue(snapshot.animated());
        assertEquals(77L, snapshot.animationPoseTick());
    }

    @Test
    void threadLocalBuildContextHoldsItsCapturedSnapshot() {
        long section = 123L;
        var firstWind = new WindForceMath.WindForce(1.0F, 0.0F, 0.25F);
        AmbientRenderState.publishWind(firstWind);
        AmbientRenderState.targetAnimation(section, 40L);
        AmbientRenderState.beginPackedSectionBuild(section);

        AmbientRenderState.publishWind(new WindForceMath.WindForce(0.0F, 1.0F, 0.9F));
        AmbientRenderState.targetAnimation(section, 42L);
        var captured = AmbientRenderState.forPackedSection(section);
        AmbientRenderState.endSectionBuild();

        assertEquals(firstWind, captured.wind());
        assertEquals(40L, captured.animationPoseTick());
    }
}
