package com.apexmotors.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public final class Vehicle {

    private final Long id;
    private final String slug;
    private final String manufacturer;
    private final String model;
    private final int year;
    private final String bodyStyle;
    private final String powertrain;
    private final String transmission;
    private final BigDecimal price;
    private final int horsepower;
    private final double zeroToHundred;
    private final int topSpeed;
    private final String imagePath;
    private final String accent;
    private final boolean featured;
    private final String tagline;
    private final String description;
    private final VehicleImageSequence imageSequence;
    private final List<String> highlights;

    public Vehicle(Long id, String slug, String manufacturer, String model, int year,
                   String bodyStyle, String powertrain, String transmission, BigDecimal price,
                   int horsepower, double zeroToHundred, int topSpeed, String imagePath,
                   String accent, boolean featured, String tagline, String description,
                   VehicleImageSequence imageSequence, List<String> highlights) {
        this.id = id;
        this.slug = slug;
        this.manufacturer = manufacturer;
        this.model = model;
        this.year = year;
        this.bodyStyle = bodyStyle;
        this.powertrain = powertrain;
        this.transmission = transmission;
        this.price = price;
        this.horsepower = horsepower;
        this.zeroToHundred = zeroToHundred;
        this.topSpeed = topSpeed;
        this.imagePath = imagePath;
        this.accent = accent;
        this.featured = featured;
        this.tagline = tagline;
        this.description = description;
        this.imageSequence = Objects.requireNonNull(imageSequence, "imageSequence must not be null");
        this.highlights = List.copyOf(highlights);
    }

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public String getManufacturer() { return manufacturer; }
    public String getModel() { return model; }
    public int getYear() { return year; }
    public String getBodyStyle() { return bodyStyle; }
    public String getPowertrain() { return powertrain; }
    public String getTransmission() { return transmission; }
    public BigDecimal getPrice() { return price; }
    public int getHorsepower() { return horsepower; }
    public double getZeroToHundred() { return zeroToHundred; }
    public int getTopSpeed() { return topSpeed; }
    public String getImagePath() { return imagePath; }
    public String getAccent() { return accent; }
    public boolean isFeatured() { return featured; }
    public String getTagline() { return tagline; }
    public String getDescription() { return description; }
    public VehicleImageSequence getImageSequence() { return imageSequence; }
    public boolean isViewerEnabled() { return imageSequence.viewerEnabled(); }
    public List<String> getHighlights() { return highlights; }
    public String getDisplayName() { return manufacturer + " " + model; }
}
