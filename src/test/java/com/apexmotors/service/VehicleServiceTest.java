package com.apexmotors.service;

import com.apexmotors.dto.VehicleQuery;
import com.apexmotors.dto.VehicleQuery.SortOption;
import com.apexmotors.model.Vehicle;
import com.apexmotors.model.VehicleImageSequence;
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
    void keepsThe360ViewerDisabledUntilRealSequencesExist() {
        Vehicle nevera = vehicleService.getBySlug("rimac-nevera");
        Vehicle ferrari = vehicleService.getBySlug("ferrari-sf90-stradale");

        assertThat(nevera.isViewerEnabled()).isFalse();
        assertThat(nevera.getImageSequence().posterImage()).isEqualTo(nevera.getImagePath());
        assertThat(ferrari.isViewerEnabled()).isFalse();
    }

    @Test
    void validatesImageSequenceMetadataBeforeEnablingViewer() {
        assertThatThrownBy(() -> new VehicleImageSequence(true, 1,
            "/assets/cars/demo/360/frame-{frame}.webp", "/images/black-hypercar.png"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least two frames");

        assertThatThrownBy(() -> new VehicleImageSequence(true, 36,
            "/assets/cars/demo/360/image.webp", "/images/black-hypercar.png"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("{frame}");

        VehicleImageSequence sequence = new VehicleImageSequence(true, 36,
            "/assets/cars/demo/360/frame-{frame}.webp", "/assets/cars/demo/hero.webp");

        assertThat(sequence.viewerEnabled()).isTrue();
        assertThat(sequence.frameCount()).isEqualTo(36);
    }
}
