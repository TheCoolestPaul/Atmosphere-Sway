package com.atmossway.client;

import com.github.razorplay01.sway.client.SwayData;

/**
 * A render-only, already-interpolated SWAY snapshot.
 */
final class AtmosSwayData extends SwayData {
    AtmosSwayData(float nx, float nz, float intensity) {
        super(nx, nz, intensity);
    }

    @Override
    public SwayData getInterpolated(float smoothness) {
        return this;
    }
}
