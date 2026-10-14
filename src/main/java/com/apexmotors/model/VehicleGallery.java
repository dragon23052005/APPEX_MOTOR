package com.apexmotors.model;

import java.util.Comparator;
import java.util.List;

public record VehicleGallery(String vehicleSlug, List<GalleryImage> images) {
    public VehicleGallery {
        images = images.stream().sorted(Comparator.comparingInt(GalleryImage::displayOrder)).toList();
    }

    public GalleryImage getFirstImage() {
        return images.getFirst();
    }
}
