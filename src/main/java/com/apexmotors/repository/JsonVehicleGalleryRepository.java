package com.apexmotors.repository;

import com.apexmotors.model.GalleryImage;
import com.apexmotors.model.VehicleGallery;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** Loads the editorial manifest once; no directory scanning or random ordering per request. */
@Repository
public class JsonVehicleGalleryRepository implements VehicleGalleryRepository {
    private final VehicleGallery gallery;

    public JsonVehicleGalleryRepository(ObjectMapper mapper) throws IOException {
        try (var input = new ClassPathResource("galleries/ferrari-sf90-stradale.json").getInputStream()) {
            gallery = mapper.readValue(input, VehicleGallery.class);
        }
        validate(gallery);
    }

    private static void validate(VehicleGallery gallery) {
        if (!"ferrari-sf90-stradale".equals(gallery.vehicleSlug()) || gallery.images().isEmpty()) {
            throw new IllegalStateException("Invalid Ferrari gallery manifest");
        }
        Set<String> ids = new HashSet<>();
        Set<String> originals = new HashSet<>();
        Set<Integer> orders = new HashSet<>();
        for (GalleryImage image : gallery.images()) {
            if (image.id() == null || !image.id().matches("[a-z0-9-]+") || !ids.add(image.id())
                    || !originals.add(image.originalFilename()) || !orders.add(image.displayOrder())
                    || image.displayOrder() < 1 || image.width() < 1 || image.height() < 1
                    || !Set.of("exterior", "details", "interior").contains(image.category())
                    || image.title() == null || image.title().isBlank()
                    || image.description() == null || image.description().isBlank()
                    || image.classificationConfidence() < 0 || image.classificationConfidence() > 1
                    || image.originalFilename() == null
                    || !image.originalFilename().matches("[a-zA-Z0-9-]+\\.png")) {
                throw new IllegalStateException("Invalid gallery entry: " + image.id());
            }
            for (String url : new String[]{image.imageUrl(), image.getThumbnailUrl(), image.getOriginalUrl()}) {
                if (url == null || !url.startsWith("/assets/cars/ferrari-sf90-stradale/")
                        || url.contains("..") || !new ClassPathResource("static" + url).exists()) {
                    throw new IllegalStateException("Missing or unsafe gallery asset: " + url);
                }
            }
        }
    }

    @Override
    public Optional<VehicleGallery> findByVehicleSlug(String slug) {
        return gallery.vehicleSlug().equals(slug) ? Optional.of(gallery) : Optional.empty();
    }
}
