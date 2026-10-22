package com.apexmotors.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "favorites")
public class FavoriteEntity {
    @EmbeddedId
    private FavoriteId id;

    @JsonIgnore
    @NotNull
    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @JsonIgnore
    @NotNull
    @MapsId("vehicleId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private VehicleEntity vehicle;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected FavoriteEntity() { }

    public FavoriteEntity(UserEntity user, VehicleEntity vehicle) {
        this.id = new FavoriteId(user.getId(), vehicle.getId());
        this.user = user;
        this.vehicle = vehicle;
    }

    @PrePersist
    protected void recordCreation() { createdAt = LocalDateTime.now(ZoneOffset.UTC); }

    public FavoriteId getId() { return id; }
    public UserEntity getUser() { return user; }
    public VehicleEntity getVehicle() { return vehicle; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
