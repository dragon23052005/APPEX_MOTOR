package com.apexmotors.dto;

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
    }
}
