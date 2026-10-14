package com.apexmotors.model;

/** Editorial metadata for one supplied photograph, not a physical rotation frame. */
public record GalleryImage(
        String id, String imageUrl,
        String originalFilename, int displayOrder, String category, String cameraAngle,
        String title, String description, String transitionStyle, String verificationStatus,
        double classificationConfidence, String similarityNote, String duplicateNote,
        String reviewNote, int width, int height) {

    public String getThumbnailUrl() {
        return imageUrl.replace("/gallery/", "/gallery/thumbnails/");
    }

    public String getOriginalUrl() {
        return "/assets/cars/ferrari-sf90-stradale/ferrari_sf90_20_views/" + originalFilename;
    }

    public String getCategoryLabel() {
        return switch (category) {
            case "exterior" -> "Exterior";
            case "details" -> "Details";
            case "interior" -> "Interior";
            default -> throw new IllegalStateException("Unknown gallery category: " + category);
        };
    }
}
