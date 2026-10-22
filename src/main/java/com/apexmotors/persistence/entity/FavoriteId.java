package com.apexmotors.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class FavoriteId implements Serializable {
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    protected FavoriteId() { }

    public FavoriteId(Long userId, Long vehicleId) {
        this.userId = Objects.requireNonNull(userId);
        this.vehicleId = Objects.requireNonNull(vehicleId);
    }

    public Long getUserId() { return userId; }
    public Long getVehicleId() { return vehicleId; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FavoriteId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(vehicleId, that.vehicleId);
    }

    @Override
    public int hashCode() { return Objects.hash(userId, vehicleId); }
}
