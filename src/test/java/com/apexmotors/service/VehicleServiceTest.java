package com.apexmotors.service;

import com.apexmotors.dto.VehicleQuery;
import com.apexmotors.dto.VehicleQuery.SortOption;
import com.apexmotors.model.Vehicle;
import com.apexmotors.repository.InMemoryVehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VehicleServiceTest {

    private VehicleService vehicleService;

    @BeforeEach
    void setUp() {
        vehicleService = new VehicleService(new InMemoryVehicleRepository());
    }

    @Test
    void filtersByManufacturerAndSearchesByName() {
        List<Vehicle> ferraris = vehicleService.search(new VehicleQuery("sf90", "Ferrari", SortOption.FEATURED));

        assertThat(ferraris).singleElement()
            .extracting(Vehicle::getSlug)
            .isEqualTo("ferrari-sf90-stradale");
    }

    @Test
    void sortsByHorsepowerDescending() {
        List<Vehicle> vehicles = vehicleService.search(new VehicleQuery("", "", SortOption.HORSEPOWER_DESC));

        assertThat(vehicles).isSortedAccordingTo(
            (left, right) -> Integer.compare(right.getHorsepower(), left.getHorsepower()));
        assertThat(vehicles.getFirst().getModel()).isEqualTo("Nevera");
    }

    @Test
    void throwsForUnknownSlug() {
        assertThatThrownBy(() -> vehicleService.getBySlug("not-a-car"))
            .isInstanceOf(VehicleNotFoundException.class);
    }

    @Test
    void exposesOptional3dModelMetadataWithoutChangingOtherVehicles() {
        Vehicle nevera = vehicleService.getBySlug("rimac-nevera");
        Vehicle ferrari = vehicleService.getBySlug("ferrari-sf90-stradale");

        assertThat(nevera.hasModel3d()).isTrue();
        assertThat(nevera.getModel3d().path()).endsWith("CarConcept.glb");
        assertThat(ferrari.hasModel3d()).isFalse();
    }
}
