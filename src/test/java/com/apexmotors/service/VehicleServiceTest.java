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
    void sortsByPriceInBothDirections() {
        List<Vehicle> ascending = vehicleService.search(new VehicleQuery("", "", SortOption.PRICE_ASC));
        List<Vehicle> descending = vehicleService.search(new VehicleQuery("", "", SortOption.PRICE_DESC));

        assertThat(ascending).isSortedAccordingTo(
            (left, right) -> left.getPrice().compareTo(right.getPrice()));
        assertThat(ascending.getFirst().getSlug()).isEqualTo("porsche-911-gt3-rs");

        assertThat(descending).isSortedAccordingTo(
            (left, right) -> right.getPrice().compareTo(left.getPrice()));
        assertThat(descending.getFirst().getSlug()).isEqualTo("koenigsegg-jesko-attack");
    }

    @Test
    void searchesNamesAndPowertrainsCaseInsensitively() {
        assertThat(vehicleService.search(new VehicleQuery("NEVERA", "", SortOption.FEATURED)))
            .singleElement()
            .extracting(Vehicle::getSlug)
            .isEqualTo("rimac-nevera");

        assertThat(vehicleService.search(new VehicleQuery("QUAD-MOTOR", "RIMAC", SortOption.FEATURED)))
            .singleElement()
            .extracting(Vehicle::getSlug)
            .isEqualTo("rimac-nevera");
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
    void givesEveryVehicleItsOwnSlugSpecificStudioImage() {
        List<Vehicle> vehicles = vehicleService.search(new VehicleQuery("", "", SortOption.FEATURED));

        assertThat(vehicles)
            .extracting(Vehicle::getImagePath)
            .doesNotHaveDuplicates();

        assertThat(vehicles)
            .allSatisfy(vehicle -> assertThat(vehicle.getImagePath())
                .isEqualTo("/assets/cars/" + vehicle.getSlug() + "/hero.webp"));
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
