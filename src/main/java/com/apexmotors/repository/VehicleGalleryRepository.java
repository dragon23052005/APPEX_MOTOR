package com.apexmotors.repository;

import com.apexmotors.model.VehicleGallery;
import java.util.Optional;

public interface VehicleGalleryRepository {
    Optional<VehicleGallery> findByVehicleSlug(String slug);
}
