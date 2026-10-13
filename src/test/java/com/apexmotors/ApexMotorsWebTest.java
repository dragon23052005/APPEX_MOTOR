package com.apexmotors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class ApexMotorsWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void homeAndCollectionRender() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(view().name("index"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Beyond")));

        mockMvc.perform(get("/collection").param("manufacturer", "Rimac").param("sort", "PRICE_DESC"))
            .andExpect(status().isOk())
            .andExpect(view().name("collection"))
            .andExpect(model().attributeExists("vehicles"));
    }

    @Test
    void vehicleDetailsAndCustom404Render() throws Exception {
        mockMvc.perform(get("/cars/rimac-nevera"))
            .andExpect(status().isOk())
            .andExpect(view().name("car-details"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Nevera")));

        mockMvc.perform(get("/does-not-exist"))
            .andExpect(status().isNotFound())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("This route ends here")));
    }

    @Test
    void contactValidationRejectsInvalidDataAndAcceptsValidSubmission() throws Exception {
        mockMvc.perform(post("/contact")
                .param("name", "")
                .param("email", "not-an-email")
                .param("interest", "")
                .param("message", "short"))
            .andExpect(status().isOk())
            .andExpect(view().name("contact"))
            .andExpect(model().attributeHasFieldErrors("contactForm", "name", "email", "interest", "message", "privacyAccepted"));

        mockMvc.perform(post("/contact")
                .param("name", "Alex Morgan")
                .param("email", "alex@example.com")
                .param("phone", "+84 900 000 000")
                .param("interest", "Private viewing")
                .param("message", "I would like to arrange a private showroom consultation.")
                .param("privacyAccepted", "true"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/contact"));
    }
}
