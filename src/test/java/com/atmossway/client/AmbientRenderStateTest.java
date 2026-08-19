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
        var gustWind = new WindForceMath.WindForce(1.0F, 0.0F, 0.7F);
        AmbientRenderState.publishWind(new WindForceMath.WindForce(0.0F, 1.0F, 0.5F));
        AmbientRenderState.targetAnimation(section, gustWind, 77L);

        var snapshot = AmbientRenderState.captureForSection(section);

        assertTrue(snapshot.animated());
        assertEquals(gustWind, snapshot.wind());
        assertEquals(77L, snapshot.animationPoseTick());
    }

    @Test
    void propagatedGustUsesAStaticSectionSpecificWind() {
        long section = 91L;
        var gustWind = new WindForceMath.WindForce(-1.0F, 0.0F, 0.3F);
        AmbientRenderState.publishWind(new WindForceMath.WindForce(0.0F, 1.0F, 0.2F));
        AmbientRenderState.targetGust(section, gustWind);

        var snapshot = AmbientRenderState.captureForSection(section);

        assertEquals(gustWind, snapshot.wind());
        assertFalse(snapshot.animated());
    }

    @Test
    void threadLocalBuildContextHoldsItsCapturedSnapshot() {
        long section = 123L;
        var firstWind = new WindForceMath.WindForce(1.0F, 0.0F, 0.25F);
        AmbientRenderState.publishWind(firstWind);
        AmbientRenderState.targetAnimation(section, firstWind, 40L);
        AmbientRenderState.beginPackedSectionBuild(section);

        var secondWind = new WindForceMath.WindForce(0.0F, 1.0F, 0.9F);
        AmbientRenderState.publishWind(secondWind);
        AmbientRenderState.targetAnimation(section, secondWind, 42L);
        var captured = AmbientRenderState.forPackedSection(section);
        AmbientRenderState.endSectionBuild();

        assertEquals(firstWind, captured.wind());
        assertEquals(40L, captured.animationPoseTick());
    }

    @Test
    void clearingASectionTargetRestoresThePublishedWind() {
        long section = 55L;
        var base = new WindForceMath.WindForce(0.0F, 1.0F, 0.2F);
        AmbientRenderState.publishWind(base);
        AmbientRenderState.targetGust(section, new WindForceMath.WindForce(1.0F, 0.0F, 0.4F));

        AmbientRenderState.clearSectionTarget(section);

        assertEquals(base, AmbientRenderState.captureForSection(section).wind());
    }
}
