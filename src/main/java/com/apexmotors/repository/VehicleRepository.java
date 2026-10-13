package com.apexmotors.repository;

import com.apexmotors.model.Vehicle;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository {

    List<Vehicle> findAll();

    Optional<Vehicle> findBySlug(String slug);

    List<Vehicle> findFeatured();
}
