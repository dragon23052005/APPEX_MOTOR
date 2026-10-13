package com.apexmotors.model;

import java.util.Objects;

/**
 * Optional metadata for a real, multi-angle vehicle image sequence.
 *
 * <p>The disabled factory is deliberately used until a complete sequence exists. This prevents
 * the UI from presenting a single image as a working 360-degree experience.</p>
 */
public record VehicleImageSequence(
    boolean viewerEnabled,
    int frameCount,
    String framePathPattern,
    String posterImage
) {

    public VehicleImageSequence {
        posterImage = requireText(posterImage, "posterImage");

        if (viewerEnabled) {
            if (frameCount < 2) {
                throw new IllegalArgumentException("An enabled image sequence requires at least two frames");
            }
            framePathPattern = requireText(framePathPattern, "framePathPattern");
            if (!framePathPattern.contains("{frame}")) {
                throw new IllegalArgumentException("framePathPattern must contain the {frame} token");
            }
        } else {
            frameCount = 0;
            framePathPattern = "";
        }
    }

    public static VehicleImageSequence unavailable(String posterImage) {
        return new VehicleImageSequence(false, 0, "", posterImage);
    }

    private static String requireText(String value, String fieldName) {
        if (Objects.requireNonNull(value, fieldName + " must not be null").isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
