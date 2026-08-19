package com.atmossway.client;

import com.github.razorplay01.sway.client.SwayData;

/**
 * A render-only, already-interpolated SWAY snapshot.
 */
final class AtmosSwayData extends SwayData {
    private final long packedRenderPosition;

    AtmosSwayData(float nx, float nz, float intensity, long packedRenderPosition) {
        super(nx, nz, intensity);
        this.packedRenderPosition = packedRenderPosition;
    }

    long packedRenderPosition() {
        return packedRenderPosition;
    }

    @Override
    public SwayData getInterpolated(float smoothness) {
        return this;
    }
}
