package com.apexmotors.repository;

import com.apexmotors.model.Vehicle;
import com.apexmotors.model.VehicleImageSequence;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public class InMemoryVehicleRepository implements VehicleRepository {

    private final List<Vehicle> vehicles = List.of(
        vehicle(1L, "ferrari-sf90-stradale", "Ferrari", "SF90 Stradale", 2025, "Coupe",
            "4.0L twin-turbo V8 hybrid", "8-speed dual-clutch", "625000", 986, 2.5, 340,
            hero("ferrari-sf90-stradale"), "#e10600", true, "Electrified intensity, without compromise.",
            "A landmark plug-in hybrid supercar balancing extreme performance with remarkable composure.",
            "Three electric motors", "Carbon-fibre driver zone", "Adaptive performance control"),
        vehicle(2L, "lamborghini-revuelto", "Lamborghini", "Revuelto", 2025, "Coupe",
            "6.5L V12 hybrid", "8-speed dual-clutch", "608000", 1001, 2.5, 350,
            hero("lamborghini-revuelto"), "#ff3b18", true, "A new era of naturally aspirated drama.",
            "V12 theatre meets a sophisticated hybrid system in a shape engineered for maximum presence.",
            "Carbon monofuselage", "V12 hybrid architecture", "Active aerodynamics"),
        vehicle(3L, "mclaren-750s", "McLaren", "750S", 2025, "Coupe",
            "4.0L twin-turbo V8", "7-speed seamless-shift", "324000", 740, 2.8, 332,
            hero("mclaren-750s"), "#b9bec5", true, "Lightness, distilled.",
            "Focused engineering and low mass create a deeply connected, road-ready supercar experience.",
            "Carbon-fibre monocoque", "Proactive chassis control", "Active rear wing"),
        vehicle(4L, "porsche-911-gt3-rs", "Porsche", "911 GT3 RS", 2025, "Coupe",
            "4.0L naturally aspirated flat-six", "7-speed PDK", "285000", 518, 3.2, 296,
            hero("porsche-911-gt3-rs"), "#d3d7da", false, "Born on the circuit. Calibrated for the road.",
            "A precision instrument shaped by downforce, response, and the unmistakable character of a flat-six.",
            "DRS rear wing", "Motorsport suspension", "Naturally aspirated response"),
        vehicle(5L, "aston-martin-valhalla", "Aston Martin", "Valhalla", 2026, "Coupe",
            "4.0L twin-turbo V8 hybrid", "8-speed dual-clutch", "850000", 1064, 2.5, 350,
            hero("aston-martin-valhalla"), "#6a7472", false, "British elegance at hypercar velocity.",
            "A mid-engine hybrid flagship combining sculptural design, active aero, and devastating pace.",
            "Carbon-fibre tub", "Torque-vectoring e-axle", "Active aero surfaces"),
        vehicle(6L, "rimac-nevera", "Rimac", "Nevera", 2025, "Coupe",
            "Quad-motor electric", "Single-speed per motor", "2400000", 1914, 1.9, 412,
            hero("rimac-nevera"), "#6f7780", true, "The electric horizon, accelerated.",
            "Four independent motors deliver extraordinary control and a new definition of instant performance.",
            "120 kWh battery", "All-wheel torque vectoring", "Carbon monocoque"),
        vehicle(7L, "koenigsegg-jesko-attack", "Koenigsegg", "Jesko Attack", 2025, "Coupe",
            "5.0L twin-turbo V8", "9-speed Light Speed", "3000000", 1578, 2.5, 480,
            hero("koenigsegg-jesko-attack"), "#aeb4ba", false, "Engineered beyond the edge.",
            "An uncompromising high-downforce machine built around airflow, low mass, and immense power.",
            "Light Speed transmission", "Triplex suspension", "Active rear aerodynamics"),
        vehicle(8L, "mercedes-amg-one", "Mercedes-AMG", "ONE", 2025, "Coupe",
            "1.6L V6 E-Turbo hybrid", "7-speed automated manual", "2720000", 1049, 2.9, 352,
            hero("mercedes-amg-one"), "#9ca3aa", false, "Formula technology. Road registered.",
            "A road-going expression of Formula-derived hybrid engineering with extraordinary technical depth.",
            "F1-derived power unit", "Active aero programme", "Carbon monocoque")
    );

    private static String hero(String slug) {
        return "/assets/cars/" + slug + "/hero.png";
    }

    private Vehicle vehicle(Long id, String slug, String manufacturer, String model, int year,
                            String bodyStyle, String powertrain, String transmission, String price,
                            int horsepower, double zeroToHundred, int topSpeed, String imagePath,
                            String accent, boolean featured, String tagline, String description,
                            String... highlights) {
        return new Vehicle(id, slug, manufacturer, model, year, bodyStyle, powertrain, transmission,
            new BigDecimal(price), horsepower, zeroToHundred, topSpeed, imagePath, accent, featured,
            tagline, description, VehicleImageSequence.unavailable(imagePath), List.of(highlights));
    }

    @Override
    public List<Vehicle> findAll() {
        return vehicles;
    }

    @Override
    public Optional<Vehicle> findBySlug(String slug) {
        return vehicles.stream().filter(vehicle -> vehicle.getSlug().equalsIgnoreCase(slug)).findFirst();
    }

    @Override
    public List<Vehicle> findFeatured() {
        return vehicles.stream().filter(Vehicle::isFeatured).toList();
    }
}
