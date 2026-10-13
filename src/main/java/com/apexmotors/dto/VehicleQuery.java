package com.apexmotors.dto;

import java.util.Locale;

public record VehicleQuery(String search, String manufacturer, SortOption sort) {

    public VehicleQuery {
        search = search == null ? "" : search.trim();
        manufacturer = manufacturer == null ? "" : manufacturer.trim();
        sort = sort == null ? SortOption.FEATURED : sort;
    }

    public enum SortOption {
        FEATURED("Featured first"),
        PRICE_ASC("Price: low to high"),
        PRICE_DESC("Price: high to low"),
        HORSEPOWER_DESC("Horsepower: high to low"),
        HORSEPOWER_ASC("Horsepower: low to high");

        private final String label;

        SortOption(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static SortOption fromRequestValue(String value) {
            if (value == null || value.isBlank()) {
                return FEATURED;
            }

            String normalized = value.trim()
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);

            try {
                return valueOf(normalized);
            } catch (IllegalArgumentException ignored) {
                return FEATURED;
            }
        }
    }
}
