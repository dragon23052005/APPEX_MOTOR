package com.apexmotors;

import com.apexmotors.model.GalleryImage;
import com.apexmotors.model.VehicleGallery;
import com.apexmotors.repository.VehicleGalleryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class VehicleGalleryTest {
    @Autowired VehicleGalleryRepository repository;
    @Autowired MockMvc mvc;

    private VehicleGallery gallery() {
        return repository.findByVehicleSlug("ferrari-sf90-stradale").orElseThrow();
    }

    @Test
    void manifestCoversEverySuppliedImageExactlyOnce() throws Exception {
        var resources = new PathMatchingResourcePatternResolver().getResources(
                "classpath*:static/assets/cars/ferrari-sf90-stradale/ferrari_sf90_20_views/*.png");
        var actual = Arrays.stream(resources).map(resource -> resource.getFilename()).collect(Collectors.toSet());
        assertThat(actual).isNotEmpty();
        assertThat(gallery().images()).hasSize(actual.size());
        assertThat(gallery().images()).extracting(GalleryImage::originalFilename)
                .containsExactlyInAnyOrderElementsOf(actual).doesNotHaveDuplicates();
        assertThat(gallery().images()).extracting(GalleryImage::id).doesNotHaveDuplicates();
        assertThat(gallery().images()).extracting(GalleryImage::displayOrder).isSorted().doesNotHaveDuplicates();
    }

    @Test
    void eachEntryHasReviewedEditorialMetadata() {
        for (var image : gallery().images()) {
            assertThat(image.category()).isIn("exterior", "details", "interior");
            assertThat(image.title()).isNotBlank();
            assertThat(image.description()).isNotBlank();
            assertThat(image.cameraAngle()).isNotBlank();
            assertThat(image.verificationStatus()).startsWith("visually-reviewed");
            assertThat(image.classificationConfidence()).isBetween(0.0, 1.0);
            assertThat(image.similarityNote()).isNotBlank();
            assertThat(image.duplicateNote()).isNotBlank();
            assertThat(image.reviewNote()).isNotBlank();
            assertThat(image.transitionStyle()).isIn("walkaround", "focus", "fade", "reveal");
            assertThat(image.width()).isPositive();
            assertThat(image.height()).isPositive();
        }
    }

    @Test
    void narrativeIsExteriorThenDetailsThenInteriorThenFinale() {
        var stages = new ArrayList<String>();
        for (var image : gallery().images()) {
            if (stages.isEmpty() || !stages.getLast().equals(image.category())) stages.add(image.category());
        }
        assertThat(stages).containsExactly("exterior", "details", "interior", "exterior");
        assertThat(gallery().getFirstImage().cameraAngle()).startsWith("Front three-quarter");
        assertThat(gallery().images().getLast().transitionStyle()).isEqualTo("reveal");
    }

    @Test
    void manifestOrderIsDeterministicAndImmutable() {
        var reversed = new ArrayList<>(gallery().images());
        Collections.reverse(reversed);
        var sorted = new VehicleGallery(gallery().vehicleSlug(), reversed);
        assertThat(sorted.images()).isEqualTo(gallery().images());
        assertThatThrownBy(() -> sorted.images().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void allOriginalsDeliveryImagesAndThumbnailsAreServed() throws Exception {
        Set<String> unique = new HashSet<>();
        for (var image : gallery().images()) {
            for (var url : new String[]{image.imageUrl(), image.getThumbnailUrl(), image.getOriginalUrl()}) {
                assertThat(unique.add(url)).isTrue();
                mvc.perform(get(url)).andExpect(status().isOk())
                        .andExpect(content().contentType(url.endsWith(".png") ? "image/png" : "image/webp"))
                        .andExpect(header().string("Cache-Control", containsString("max-age=2592000")));
            }
        }
    }

    @Test
    void ferrariRendersManifestAndAccessibleProgressiveEnhancement() throws Exception {
        var html = mvc.perform(get("/cars/ferrari-sf90-stradale")).andExpect(status().isOk())
                .andExpect(model().attributeExists("gallery"))
                .andExpect(content().string(containsString("vehicle-gallery.js")))
                .andExpect(content().string(containsString("vehicle-gallery.css")))
                .andExpect(content().string(containsString("data-gallery-dialog")))
                .andExpect(content().string(containsString("role=\"progressbar\"")))
                .andExpect(content().string(containsString("not a 3D model")))
                .andExpect(content().string(containsString("<noscript>")))
                .andExpect(content().string(not(containsString("data-sequence-toolbar"))))
                .andReturn().getResponse().getContentAsString();
        int previous = -1;
        for (var image : gallery().images()) {
            int position = html.indexOf("data-id=\"" + image.id() + "\"");
            assertThat(position).isGreaterThan(previous);
            previous = position;
        }
    }

    @Test
    void galleryCopyAndAccessibilityLabelsAreEnglish() throws Exception {
        var html = mvc.perform(get("/cars/ferrari-sf90-stradale")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Every angle.")))
                .andExpect(content().string(containsString("Explore 24 perspectives")))
                .andExpect(content().string(containsString("aria-label=\"Open fullscreen\"")))
                .andExpect(content().string(containsString("Preparing image")))
                .andExpect(content().string(containsString("Unable to load this image.")))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).doesNotContain("lang=\"vi\"")
                .doesNotContainPattern("[\\p{L}&&[^\\p{ASCII}]]");
        for (var image : gallery().images()) {
            assertThat(image.getCategoryLabel()).isIn("Exterior", "Details", "Interior");
            for (String copy : new String[]{image.title(), image.description(), image.similarityNote(),
                    image.duplicateNote(), image.reviewNote()}) {
                assertThat(copy).isNotBlank().doesNotContainPattern("[\\p{L}&&[^\\p{ASCII}]]");
            }
        }
        mvc.perform(get("/js/vehicle-gallery.js"))
                .andExpect(content().string(containsString("Arrow keys or swipe to browse")))
                .andExpect(content().string(containsString("Image ${index + 1} of ${list.length}")));
    }

    @Test
    void otherVehiclesKeepExistingStaticGalleryAndNoFerrariScripts() throws Exception {
        for (String slug : new String[]{"rimac-nevera", "lamborghini-revuelto", "mclaren-750s", "porsche-911-gt3-rs",
                "aston-martin-valhalla", "koenigsegg-jesko-attack", "mercedes-amg-one"}) {
            assertThat(repository.findByVehicleSlug(slug)).isEmpty();
            mvc.perform(get("/cars/" + slug)).andExpect(status().isOk())
                    .andExpect(model().attributeDoesNotExist("gallery"))
                    .andExpect(content().string(containsString("detail-gallery__frame")))
                    .andExpect(content().string(not(containsString("vehicle-gallery.js"))))
                    .andExpect(content().string(not(containsString("vehicle-gallery.css"))));
        }
        assertThat(repository.findByVehicleSlug("../../unsafe")).isEmpty();
    }

    @Test
    void galleryAssetsAndExistingPagesReturnSuccessfully() throws Exception {
        for (String url : new String[]{"/", "/collection?q=sf90&manufacturer=Ferrari&sort=price-asc", "/about", "/contact",
                "/css/vehicle-gallery.css", "/js/vehicle-gallery.js"}) {
            mvc.perform(get(url)).andExpect(status().isOk());
        }
    }
}
