package com.apexmotors.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "vehicles")
public class VehicleEntity extends AuditedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private BrandEntity brand;

    @NotBlank @Size(max = 160)
    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @NotBlank @Size(max = 180)
    @Column(name = "slug", nullable = false, length = 180, unique = true)
    @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*")
    private String slug;

    @Size(max = 4000)
    @Column(name = "description", length = 4000)
    private String description;

    @Min(1886) @Max(9999)
    @Column(name = "model_year")
    private Integer modelYear;

    @DecimalMin("0.00") @Digits(integer = 13, fraction = 2)
    @Column(name = "price", precision = 15, scale = 2)
    private BigDecimal price;

    @Pattern(regexp = "[A-Z]{3}")
    @Column(name = "currency", length = 3)
    private String currency;

    @PositiveOrZero
    @Column(name = "horsepower")
    private Integer horsepower;

    @PositiveOrZero
    @Column(name = "top_speed_kmh")
    private Integer topSpeedKmh;

    @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 3, fraction = 2)
    @Column(name = "acceleration_0_100", precision = 5, scale = 2)
    private BigDecimal acceleration0100;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "availability_status", nullable = false, length = 24)
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.UNSPECIFIED;

    public Long getId() { return id; }
    public BrandEntity getBrand() { return brand; }
    public void setBrand(BrandEntity brand) { this.brand = brand; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getModelYear() { return modelYear; }
    public void setModelYear(Integer modelYear) { this.modelYear = modelYear; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Integer getHorsepower() { return horsepower; }
    public void setHorsepower(Integer horsepower) { this.horsepower = horsepower; }
    public Integer getTopSpeedKmh() { return topSpeedKmh; }
    public void setTopSpeedKmh(Integer topSpeedKmh) { this.topSpeedKmh = topSpeedKmh; }
    public BigDecimal getAcceleration0100() { return acceleration0100; }
    public void setAcceleration0100(BigDecimal acceleration0100) { this.acceleration0100 = acceleration0100; }
    public AvailabilityStatus getAvailabilityStatus() { return availabilityStatus; }
    public void setAvailabilityStatus(AvailabilityStatus availabilityStatus) { this.availabilityStatus = availabilityStatus; }

    @JsonIgnore
    @AssertTrue(message = "price and currency must be supplied together")
    public boolean isPriceAndCurrencyConsistent() {
        return (price == null) == (currency == null);
    }
}
