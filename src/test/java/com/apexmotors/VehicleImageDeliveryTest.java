package com.apexmotors;

import com.apexmotors.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.DigestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VehicleImageDeliveryTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private VehicleRepository vehicles;

    @Test
    void collectionVersionsBothImageCandidatesAndServesTheExactSourceBytes() throws Exception {
        String html = mockMvc.perform(get("/collection"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        for (var vehicle : vehicles.findAll()) {
            String full = versionedPath(vehicle.getImagePath());
            String small = versionedPath(vehicle.getImagePath().replace("hero.webp", "hero-960.webp"));
            assertThat(html).contains("src=\"" + full + "\"",
                "srcset=\"" + small + " 960w, " + full + " 1672w\"");

            for (String filename : new String[]{"hero.webp", "hero-960.webp"}) {
                String path = "/assets/cars/" + vehicle.getSlug() + "/" + filename;
                byte[] original = new ClassPathResource("static" + path).getContentAsByteArray();
                assertThat(new String(original, 0, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("RIFF");
                assertThat(new String(original, 8, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("WEBP");
                mockMvc.perform(get(versionedPath(path)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("image/webp"))
                    .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age=2592000")))
                    .andExpect(content().bytes(original));
            }
        }
    }

    @Test
    void everyDetailPosterAndFeaturedCardUsesVersionedUrls() throws Exception {
        String home = mockMvc.perform(get("/"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for (var vehicle : vehicles.findAll()) {
            String expected = versionedPath(vehicle.getImagePath());
            String detail = mockMvc.perform(get("/cars/" + vehicle.getSlug()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(detail).contains("data-poster-image=\"" + expected + "\"", "src=\"" + expected + "\"");
            if (vehicle.isFeatured()) {
                assertThat(home).contains("src=\"" + expected + "\"");
            }
        }
    }

    @Test
    void imageUrlsRetainTheApplicationContextPath() throws Exception {
        String html = mockMvc.perform(get("/apex/collection").contextPath("/apex"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for (var vehicle : vehicles.findAll()) {
            String full = "/apex" + versionedPath(vehicle.getImagePath());
            String small = "/apex" + versionedPath(vehicle.getImagePath().replace("hero.webp", "hero-960.webp"));
            assertThat(html).contains("src=\"" + full + "\"",
                "srcset=\"" + small + " 960w, " + full + " 1672w\"");
        }
    }

    private String versionedPath(String path) throws Exception {
        byte[] bytes = new ClassPathResource("static" + path).getContentAsByteArray();
        return path.replace(".webp", "-" + DigestUtils.md5DigestAsHex(bytes) + ".webp");
    }
}
