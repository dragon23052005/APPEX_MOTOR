package com.apexmotors.service;

import com.apexmotors.dto.VehicleQuery;
import com.apexmotors.model.Vehicle;
import com.apexmotors.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public List<Vehicle> getFeaturedVehicles() {
        return vehicleRepository.findFeatured();
    }

    public List<String> getManufacturers() {
        return vehicleRepository.findAll().stream()
            .map(Vehicle::getManufacturer)
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    public Vehicle getBySlug(String slug) {
        return vehicleRepository.findBySlug(slug)
            .orElseThrow(() -> new VehicleNotFoundException(slug));
    }

    public List<Vehicle> search(VehicleQuery query) {
        String term = query.search().toLowerCase(Locale.ROOT);
        String manufacturer = query.manufacturer();

        return vehicleRepository.findAll().stream()
            .filter(vehicle -> term.isBlank()
                || vehicle.getDisplayName().toLowerCase(Locale.ROOT).contains(term)
                || vehicle.getPowertrain().toLowerCase(Locale.ROOT).contains(term))
            .filter(vehicle -> manufacturer.isBlank()
                || vehicle.getManufacturer().equalsIgnoreCase(manufacturer))
            .sorted(comparatorFor(query.sort()))
            .toList();
    }

    private Comparator<Vehicle> comparatorFor(VehicleQuery.SortOption option) {
        return switch (option) {
            case PRICE_ASC -> Comparator.comparing(Vehicle::getPrice);
            case PRICE_DESC -> Comparator.comparing(Vehicle::getPrice).reversed();
            case HORSEPOWER_ASC -> Comparator.comparingInt(Vehicle::getHorsepower);
            case HORSEPOWER_DESC -> Comparator.comparingInt(Vehicle::getHorsepower).reversed();
            case FEATURED -> Comparator.comparing(Vehicle::isFeatured).reversed()
                .thenComparing(Vehicle::getManufacturer);
        };
    }
}
