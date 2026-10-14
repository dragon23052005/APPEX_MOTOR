package com.apexmotors.service;

import com.apexmotors.model.VehicleGallery;
import com.apexmotors.repository.VehicleGalleryRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class VehicleGalleryService {
    private final VehicleGalleryRepository repository;

    public VehicleGalleryService(VehicleGalleryRepository repository) {
        this.repository = repository;
    }

    public Optional<VehicleGallery> findForVehicle(String slug) {
        return repository.findByVehicleSlug(slug);
    }
}
