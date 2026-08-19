package com.atmossway.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrecipitationWindControllerTest {
    private static final float EPSILON = 0.0001F;

    @AfterEach
    void resetController() {
        PrecipitationWindController.reset();
    }

    @Test
    void activatesOnFirstValidSampleAndResetsCleanly() {
        PrecipitationWindController.reset();
        assertFalse(PrecipitationWindController.tickPose().active());

        PrecipitationWindController.update(9.0F, 90.0F, true);
        var active = PrecipitationWindController.tickPose();
        assertTrue(active.active());
        assertEquals(9.0F, active.speedMps(), EPSILON);
        assertEquals(90.0F, (float) Math.toDegrees(active.headingRadians()), EPSILON);

        PrecipitationWindController.disable();
        assertFalse(PrecipitationWindController.tickPose().active());
    }

    @Test
    void transientInvalidSamplesHoldTheLastValidPose() {
        PrecipitationWindController.update(5.0F, 180.0F, true);
        var valid = PrecipitationWindController.tickPose();

        PrecipitationWindController.update(Float.NaN, 0.0F, false);
        var held = PrecipitationWindController.tickPose();

        assertTrue(held.active());
        assertEquals(valid.speedMps(), held.speedMps(), EPSILON);
        assertEquals(valid.headingRadians(), held.headingRadians(), EPSILON);
        assertEquals(valid.tiltRadians(), held.tiltRadians(), EPSILON);
    }

    @Test
    void framePoseInterpolatesBetweenSmoothedTickVectors() {
        PrecipitationWindController.update(10.0F, 0.0F, true);
        var previous = PrecipitationWindController.tickPose();
        PrecipitationWindController.update(10.0F, 90.0F, true);
        var current = PrecipitationWindController.tickPose();

        var frameStart = PrecipitationWindController.framePose(0.0F);
        var frameEnd = PrecipitationWindController.framePose(1.0F);

        assertEquals(previous.headingRadians(), frameStart.headingRadians(), EPSILON);
        assertEquals(current.headingRadians(), frameEnd.headingRadians(), EPSILON);
        assertEquals(current.tiltRadians(), frameEnd.tiltRadians(), EPSILON);
    }

    @Test
    void nativeFrameUsesTheInterpolatedPoseAndResetsWhenDisabled() {
        PrecipitationWindController.update(6.0F, 0.0F, true);

        PrecipitationWindController.beginNativeFrame(1.0F);
        var nativePose = PrecipitationWindController.nativeFramePose();

        assertTrue(nativePose.active());
        assertEquals(1.0F, nativePose.magnitude(), EPSILON);
        assertEquals(-1.0F, nativePose.topOffsetZ(), EPSILON);

        PrecipitationWindController.disable();
        assertFalse(PrecipitationWindController.nativeFramePose().active());
    }
}
