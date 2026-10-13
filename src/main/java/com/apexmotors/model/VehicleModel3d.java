package com.apexmotors.model;

public record VehicleModel3d(
    String path,
    String displayName,
    String attribution,
    String license,
    String sourceUrl,
    double initialYawDegrees
) {
}
